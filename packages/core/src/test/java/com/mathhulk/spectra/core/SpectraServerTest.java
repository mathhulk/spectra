package com.mathhulk.spectra.core;

import com.mathhulk.spectra.api.clients.ClientInfo;
import com.mathhulk.spectra.api.clients.ClientStatus;
import com.mathhulk.spectra.api.http.HttpResponse;
import com.mathhulk.spectra.api.http.WebSocket;
import com.mathhulk.spectra.api.views.Views;
import com.mathhulk.spectra.api.views.ViewSpec;
import com.mathhulk.spectra.protocol.Header;
import com.mathhulk.spectra.core.platform.MainThread;
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
import java.nio.charset.StandardCharsets;
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
    SpectraConfig strict = new SpectraConfig(true, OptionalInt.empty(), "need mod", 20, false, 4096, List.of());
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

    SpectraServer server = start(new SpectraConfig(false, OptionalInt.empty(), "", 100, false, 64 * 1024, List.of()));
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

    var views = server.views();
    assertEquals(Views.OpenResult.UNSUPPORTED,
        views.open(player, "hud", ViewSpec.layer("shop.html", com.mathhulk.spectra.api.views.Layout.FULLSCREEN)));
    assertEquals(Views.OpenResult.QUEUED, views.open(player, "shop", ViewSpec.screen("shop.html")));
    assertTrue(platform.drain(player, OpenView.class).isEmpty());

    server.handleFrame(player, Packets.encode(new ResourcesSynced(generation)));
    assertEquals("shop", platform.drain(player, OpenView.class).getFirst().viewId());
    assertEquals(Views.OpenResult.SENT, views.open(player, "shop", ViewSpec.screen("shop.html")));

    List<Views.Message> posts = new ArrayList<>();
    views.onMessage(posts::add);
    server.handleFrame(player, Packets.encode(new ViewPost("not-opened", "{}")));
    server.handleFrame(player, Packets.encode(new ViewPost("shop", "{\"buy\":1}")));
    assertEquals(1, posts.size());
    assertEquals("{\"buy\":1}", posts.getFirst().json());

    server.handleFrame(player, Packets.encode(new ViewEvent("shop", ViewEvent.Type.CLOSED, "escape")));
    assertTrue(views.open(player).isEmpty());
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

  /** Joins with every capability and finishes resource sync, then opens a screen so the view may talk to us. */
  private SpectraServer readyWithView(String viewId) throws Exception {
    Files.createDirectories(data.resolve("resources"));
    Files.writeString(data.resolve("resources/app.html"), "app");
    SpectraServer server = start(SpectraConfig.DEFAULTS);
    server.join(player);
    hello(server, Protocol.VERSION, Set.of(Capability.RESOURCES, Capability.SCREENS, Capability.MESSAGES,
        Capability.HTTP, Capability.SOCKETS));
    long generation = platform.drain(player, ResourceManifest.class).getFirst().generation();
    server.handleFrame(player, Packets.encode(new ResourcesSynced(generation)));
    server.views().open(player, viewId, ViewSpec.screen("app.html"));
    return server;
  }

  @Test
  void httpRequestsRouteByLongestPrefixAndReassembleBodies() throws Exception {
    SpectraServer server = readyWithView("app");
    server.http().route("/", request -> null); // Declines: falls through.
    server.http().route("/shop/", request -> java.util.concurrent.CompletableFuture.completedFuture(
        HttpResponse.json(200, "{\"got\":\"" + request.text() + "\",\"path\":\"" + request.path() + "\"}")));

    server.handleFrame(player, Packets.encode(new com.mathhulk.spectra.protocol.serverbound.HttpRequest(1, "app",
        "POST", "/api/shop/buy?item=sword", List.of(new Header("Content-Type", "text/plain")),
        "ab".getBytes(StandardCharsets.UTF_8), false)));
    server.handleFrame(player, Packets.encode(new HttpRequestBody(1, "cd".getBytes(StandardCharsets.UTF_8), true)));

    var response = platform.drain(player, com.mathhulk.spectra.protocol.clientbound.HttpResponse.class).getFirst();
    assertEquals(200, response.status());
    assertTrue(response.last());
    assertEquals("{\"got\":\"abcd\",\"path\":\"/api/shop/buy?item=sword\"}",
        new String(response.body(), StandardCharsets.UTF_8));

    // Unrouted paths, paths outside /api, and views the server never opened get 404.
    for (String[] request : new String[][] {{"app", "/api/nothing"}, {"app", "/index.html"}, {"other", "/api/shop/x"}}) {
      server.handleFrame(player, Packets.encode(new com.mathhulk.spectra.protocol.serverbound.HttpRequest(9,
          request[0], "GET", request[1], List.of(), new byte[0], true)));
      assertEquals(404, platform.drain(player, com.mathhulk.spectra.protocol.clientbound.HttpResponse.class)
          .getFirst().status());
    }
  }

  @Test
  void largeResponsesAreChunked() throws Exception {
    SpectraServer server = readyWithView("app");
    byte[] big = new byte[600 * 1024];
    server.http().route("/big", request -> java.util.concurrent.CompletableFuture.completedFuture(
        HttpResponse.of(200, "application/octet-stream", big)));
    server.handleFrame(player, Packets.encode(new com.mathhulk.spectra.protocol.serverbound.HttpRequest(2, "app",
        "GET", "/api/big", List.of(), new byte[0], true)));
    var head = platform.drain(player, com.mathhulk.spectra.protocol.clientbound.HttpResponse.class).getFirst();
    var rest = platform.drain(player, HttpResponseBody.class);
    assertFalse(head.last());
    assertTrue(rest.getLast().last());
    assertEquals(big.length, head.body().length + rest.stream().mapToInt(b -> b.data().length).sum());
  }

  @Test
  void slowHandlersTimeOut() throws Exception {
    SpectraServer server = readyWithView("app");
    server.http().route("/slow", request -> new java.util.concurrent.CompletableFuture<>());
    server.handleFrame(player, Packets.encode(new com.mathhulk.spectra.protocol.serverbound.HttpRequest(3, "app",
        "GET", "/api/slow", List.of(), new byte[0], true)));
    platform.ticks((int) com.mathhulk.spectra.core.http.HttpService.TIMEOUT_TICKS);
    assertEquals(504, platform.drain(player, com.mathhulk.spectra.protocol.clientbound.HttpResponse.class)
        .getFirst().status());
  }

  @Test
  void webSocketsOpenEchoAndClose() throws Exception {
    SpectraServer server = readyWithView("app");
    List<String> closes = new ArrayList<>();
    server.sockets().route("/chat", socket -> {
      socket.onMessage(message -> socket.send("echo:" + message.text()));
      socket.onClose(close -> closes.add(close.code() + " " + close.reason()));
      return true;
    });

    server.handleFrame(player, Packets.encode(new SocketOpen(7, "app", "/api/chat", List.of())));
    assertEquals(7, platform.drain(player, SocketOpened.class).getFirst().socketId());

    server.handleFrame(player, Packets.encode(new SocketSend(7, false, "he".getBytes(StandardCharsets.UTF_8), false)));
    server.handleFrame(player, Packets.encode(new SocketSend(7, false, "llo".getBytes(StandardCharsets.UTF_8), true)));
    SocketMessage echo = platform.drain(player, SocketMessage.class).getFirst();
    assertEquals("echo:hello", new String(echo.data(), StandardCharsets.UTF_8));
    assertTrue(echo.fin());

    server.handleFrame(player, Packets.encode(new SocketClose(7, 1000, "bye")));
    assertEquals(List.of("1000 bye"), closes);

    server.handleFrame(player, Packets.encode(new SocketOpen(8, "app", "/api/unknown", List.of())));
    assertEquals(4404, platform.drain(player, SocketClosed.class).getFirst().code());
  }

  @Test
  void networkPolicyIsSentToClientsThatSupportIt() throws Exception {
    SpectraConfig config = new SpectraConfig(false, OptionalInt.empty(), "", 100, false, 4096,
        List.of("https://api.example.com"));
    SpectraServer server = start(config);
    server.join(player);
    hello(server, Protocol.VERSION, Set.of(Capability.NETWORK));
    assertEquals(List.of("https://api.example.com"),
        platform.drain(player, NetworkPolicy.class).getFirst().allowedOrigins());
  }

  @Test
  void pluginResourceNamespacesAreSynced() throws Exception {
    Path pluginUi = data.resolve("plugin-ui");
    Files.createDirectories(pluginUi.resolve("shop"));
    Files.writeString(pluginUi.resolve("shop/index.html"), "plugin shop");
    SpectraServer server = start(SpectraConfig.DEFAULTS);
    server.resources().register("myplugin", pluginUi);
    server.join(player);
    hello(server, Protocol.VERSION, ALL);
    List<String> paths = platform.drain(player, ResourceManifest.class).getLast().entries().stream()
        .map(ResourceManifest.Entry::path).toList();
    assertTrue(paths.contains("myplugin/shop/index.html"), paths.toString());
  }

  @Test
  void packetsBeforeTheHandshakeAreIgnored() throws Exception {
    SpectraServer server = start(SpectraConfig.DEFAULTS);
    server.join(player);
    server.handleFrame(player, Packets.encode(new SocketOpen(1, "app", "/api/x", List.of())));
    assertTrue(platform.drain(player, SocketClosed.class).isEmpty());
  }
}
