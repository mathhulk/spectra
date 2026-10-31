package com.mathhulk.spectra.core;

import com.mathhulk.spectra.core.clients.ClientInfo;
import com.mathhulk.spectra.core.clients.ClientStatus;
import com.mathhulk.spectra.core.platform.MainThread;
import com.mathhulk.spectra.core.views.ViewService;
import com.mathhulk.spectra.core.views.ViewSpec;
import com.mathhulk.spectra.protocol.Capability;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.Protocol;
import com.mathhulk.spectra.protocol.ResourceHash;
import com.mathhulk.spectra.protocol.clientbound.*;
import com.mathhulk.spectra.protocol.serverbound.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class SpectraServerTest {
  private static final Set<String> ALL = Set.of(Capability.RESOURCES, Capability.SCREENS, Capability.LAYERS,
      Capability.MESSAGES);

  @TempDir
  Path data;

  private FakePlatform platform;
  private final UUID player = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    platform = new FakePlatform(data);
  }

  private SpectraServer start(SpectraConfig config) throws Exception {
    SpectraServer server = new SpectraServer(platform, config);
    server.start();
    return server;
  }

  private void hello(SpectraServer server, int version, Set<String> capabilities) {
    server.handleFrame(player, Packets.encode(new Hello(version, "test", capabilities)));
  }

  @Test
  void handshakeResolvesSupportedClient() throws Exception {
    SpectraServer server = start(SpectraConfig.DEFAULTS);
    server.join(player);
    CompletableFuture<ClientInfo> resolved = server.clients().resolve(player);
    assertFalse(resolved.isDone());

    hello(server, Protocol.VERSION, Set.of(Capability.SCREENS, "unknown.future.capability"));

    Welcome welcome = platform.drain(player, Welcome.class).getFirst();
    assertTrue(welcome.accepted());
    assertEquals(server.serverId(), welcome.serverId());
    ClientInfo info = resolved.getNow(null);
    assertEquals(ClientStatus.SUPPORTED, info.status());
    assertTrue(info.has(Capability.SCREENS));
    assertFalse(info.capabilities().contains("unknown.future.capability"));
  }

  @Test
  void serverIdIsStableAcrossRestarts() throws Exception {
    String first = new SpectraServer(platform, SpectraConfig.DEFAULTS).serverId();
    assertEquals(first, new SpectraServer(platform, SpectraConfig.DEFAULTS).serverId());
  }

  @Test
  void vanillaClientsAreUnsupportedButNotKickedByDefault() throws Exception {
    SpectraServer server = start(SpectraConfig.DEFAULTS);
    server.join(player);
    platform.ticks((int) SpectraConfig.DEFAULTS.handshakeTimeoutTicks());
    assertEquals(ClientStatus.UNSUPPORTED, server.clients().info(player).status());
    assertTrue(platform.kicked.isEmpty());

    // A Hello after the timeout cannot flip the status.
    hello(server, Protocol.VERSION, ALL);
    assertEquals(ClientStatus.UNSUPPORTED, server.clients().info(player).status());
  }

  @Test
  void requireClientKicksOnlyWhenOperatorOptsIn() throws Exception {
    SpectraConfig strict = new SpectraConfig(true, OptionalInt.empty(), "need mod", 20, false, 4096);
    SpectraServer server = start(strict);
    server.join(player);
    platform.ticks(20);
    assertTrue(platform.kicked.contains(player));
  }

  @Test
  void incompatibleVersionsAreRejected() throws Exception {
    SpectraServer server = start(SpectraConfig.DEFAULTS);
    server.join(player);
    hello(server, Protocol.VERSION + 1, ALL);
    assertFalse(platform.drain(player, Welcome.class).getFirst().accepted());
    assertEquals(ClientStatus.INCOMPATIBLE, server.clients().info(player).status());
  }

  @Test
  void resourcesSyncInChunksAndReassemble() throws Exception {
    byte[] big = new byte[300 * 1024];
    new java.util.Random(1).nextBytes(big);
    Files.createDirectories(data.resolve("resources/ui"));
    Files.write(data.resolve("resources/ui/big.bin"), big);
    Files.writeString(data.resolve("resources/index.html"), "<h1>hi</h1>");

    SpectraServer server = start(new SpectraConfig(false, OptionalInt.empty(), "", 100, false, 64 * 1024));
    server.join(player);
    hello(server, Protocol.VERSION, ALL);

    ResourceManifest manifest = platform.drain(player, ResourceManifest.class).getFirst();
    assertEquals(2, manifest.entries().size());
    ResourceHash bigHash = manifest.entries().stream().filter(e -> e.path().equals("ui/big.bin")).findFirst()
        .orElseThrow().hash();

    // Request twice: the second request must not cause a second upload.
    ResourceRequest request = new ResourceRequest(manifest.generation(), List.of(bigHash));
    server.handleFrame(player, Packets.encode(request));
    server.handleFrame(player, Packets.encode(request));

    List<ResourceChunk> chunks = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      platform.tick();
      List<ResourceChunk> sent = platform.drain(player, ResourceChunk.class);
      assertTrue(sent.size() <= 1, "64 KiB budget allows at most one 128 KiB chunk per tick");
      chunks.addAll(sent);
    }
    assertEquals(3, chunks.size());
    ByteArrayOutputStream assembled = new ByteArrayOutputStream();
    chunks.stream().sorted(Comparator.comparingInt(ResourceChunk::index))
        .forEach(chunk -> assembled.writeBytes(chunk.data()));
    assertEquals(bigHash, ResourceHash.compute(assembled.toByteArray()));

    // Stale generations are ignored.
    server.handleFrame(player, Packets.encode(new ResourceRequest(manifest.generation() + 5, List.of(bigHash))));
    platform.ticks(5);
    assertTrue(platform.drain(player, ResourceChunk.class).isEmpty());
  }

  @Test
  void viewsQueueUntilSyncedAndRespectCapabilities() throws Exception {
    Files.createDirectories(data.resolve("resources"));
    Files.writeString(data.resolve("resources/shop.html"), "shop");
    SpectraServer server = start(SpectraConfig.DEFAULTS);
    server.join(player);
    hello(server, Protocol.VERSION, Set.of(Capability.RESOURCES, Capability.SCREENS, Capability.MESSAGES));
    long generation = platform.drain(player, ResourceManifest.class).getFirst().generation();

    ViewService views = server.views();
    assertEquals(ViewService.OpenResult.UNSUPPORTED,
        views.open(player, "hud", ViewSpec.layer("shop.html", Layout.FULLSCREEN)));
    assertEquals(ViewService.OpenResult.QUEUED, views.open(player, "shop", ViewSpec.screen("shop.html")));
    assertTrue(platform.drain(player, OpenView.class).isEmpty());

    server.handleFrame(player, Packets.encode(new ResourcesSynced(generation)));
    assertEquals("shop", platform.drain(player, OpenView.class).getFirst().viewId());
    assertEquals(ViewService.OpenResult.SENT, views.open(player, "shop", ViewSpec.screen("shop.html")));

    List<ViewService.Post> posts = new ArrayList<>();
    views.onPost(posts::add);
    server.handleFrame(player, Packets.encode(new ViewPost("not-opened", "{}")));
    server.handleFrame(player, Packets.encode(new ViewPost("shop", "{\"buy\":1}")));
    assertEquals(1, posts.size());
    assertEquals("{\"buy\":1}", posts.getFirst().json());

    server.handleFrame(player, Packets.encode(new ViewEvent("shop", ViewEvent.Type.CLOSED, "escape")));
    assertTrue(views.openViews(player).isEmpty());
  }

  @Test
  void malformedFramesEventuallyDisconnect() throws Exception {
    SpectraServer server = start(SpectraConfig.DEFAULTS);
    server.join(player);
    for (int i = 0; i < 8; i++) {
      server.handleFrame(player, new byte[] {127});
    }
    assertTrue(platform.kicked.contains(player));
  }

  @Test
  void millisecondsConvertToRoundedTicks() {
    assertEquals(10, MainThread.toTicks(500, 1));
    assertEquals(30, MainThread.toTicks(1500, 1));
    assertEquals(1, MainThread.toTicks(10, 1));
    assertEquals(0, MainThread.toTicks(0, 0));
    assertEquals(1, MainThread.toTicks(Double.NaN, 1));
  }
}
