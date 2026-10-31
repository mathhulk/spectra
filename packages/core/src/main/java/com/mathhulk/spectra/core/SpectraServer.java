package com.mathhulk.spectra.core;

import com.mathhulk.spectra.core.clients.ClientInfo;
import com.mathhulk.spectra.core.clients.ClientSessions;
import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.core.resources.ResourceService;
import com.mathhulk.spectra.core.resources.ResourceSnapshot;
import com.mathhulk.spectra.core.resources.ResourceStore;
import com.mathhulk.spectra.core.views.ViewService;
import com.mathhulk.spectra.protocol.Capability;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.ProtocolException;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.serverbound.Hello;
import com.mathhulk.spectra.protocol.serverbound.ResourceRequest;
import com.mathhulk.spectra.protocol.serverbound.ResourcesSynced;
import com.mathhulk.spectra.protocol.serverbound.ViewEvent;
import com.mathhulk.spectra.protocol.serverbound.ViewPost;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Platform-independent server side of Spectra: client handshakes, resource sync, and views. Adapters forward
 * joins, quits, and frames; everything else lives here. Main thread only, except where noted.
 */
public final class SpectraServer implements AutoCloseable {
  /** Malformed frames tolerated per player before they are disconnected. */
  private static final int MAX_PROTOCOL_ERRORS = 8;

  private final Platform platform;
  private final SpectraConfig config;
  private final String serverId;
  private final ClientSessions sessions;
  private final ResourceStore store;
  private final ResourceService resources;
  private final ViewService views;
  private final Map<UUID, Integer> protocolErrors = new HashMap<>();
  private Cancellable ticker;
  private DirectoryWatcher resourceWatcher;
  private CompletableFuture<Void> scanning = CompletableFuture.completedFuture(null);
  private boolean rescanRequested;

  public SpectraServer(Platform platform, SpectraConfig config) throws IOException {
    this.platform = platform;
    this.config = config;
    this.serverId = loadServerId(platform.dataDirectory());
    this.sessions = new ClientSessions(platform, config, serverId);
    this.store = new ResourceStore(platform.dataDirectory().resolve("resources"), platform.logger());
    this.resources = new ResourceService(platform, config.resourceBytesPerTick());
    this.views = new ViewService(platform, sessions, resources);

    sessions.onResolved(this::onClientResolved);
  }

  /** A stable random id, so clients can cache resources per server without using its address. */
  private static String loadServerId(Path dataDirectory) throws IOException {
    Path file = dataDirectory.resolve("server-id");
    if (Files.isRegularFile(file)) {
      String id = Files.readString(file, StandardCharsets.UTF_8).strip();
      if (id.matches("[a-f0-9-]{36}")) {
        return id;
      }
    }
    Files.createDirectories(dataDirectory);
    String id = UUID.randomUUID().toString();
    Files.writeString(file, id, StandardCharsets.UTF_8);
    return id;
  }

  public Platform platform() {
    return platform;
  }

  public SpectraConfig config() {
    return config;
  }

  public String serverId() {
    return serverId;
  }

  public ClientSessions clients() {
    return sessions;
  }

  public ResourceService resources() {
    return resources;
  }

  public ViewService views() {
    return views;
  }

  public Path resourceDirectory() {
    return store.root();
  }

  /** Scans resources, starts the upload ticker, and (if configured) watches resources for changes. */
  public void start() {
    ticker = platform.mainThread().runRepeating(resources::tick, 1, 1);
    rescanResources();
    if (config.watchScripts()) {
      try {
        Files.createDirectories(store.root());
        resourceWatcher = new DirectoryWatcher(store.root(), 150,
            changed -> platform.mainThread().execute(this::rescanResources), platform.logger());
      } catch (IOException e) {
        platform.logger().log(Level.WARNING, "Could not watch resources for changes", e);
      }
    }
  }

  /** Rescans resources off the main thread and publishes the result on it. Coalesces overlapping requests. */
  public void rescanResources() {
    if (!scanning.isDone()) {
      rescanRequested = true;
      return;
    }
    scanning = CompletableFuture.supplyAsync(() -> {
      try {
        return store.scan();
      } catch (IOException e) {
        throw new java.io.UncheckedIOException(e);
      }
    }, platform.asyncExecutor()).handle((snapshot, error) -> {
      platform.mainThread().execute(() -> {
        if (error != null) {
          platform.logger().log(Level.SEVERE, "Failed to scan resources", error);
        } else {
          publish(snapshot);
        }
        if (rescanRequested) {
          rescanRequested = false;
          rescanResources();
        }
      });
      return null;
    });
  }

  private void publish(ResourceSnapshot snapshot) {
    if (snapshot != resources.snapshot()) {
      platform.logger().info("Resources at generation " + snapshot.generation() + ": " + snapshot.entries().size()
          + " files, " + snapshot.totalBytes() + " bytes");
    }
    resources.publish(snapshot);
  }

  public void join(UUID player) {
    sessions.join(player);
  }

  public void quit(UUID player) {
    sessions.quit(player);
    resources.untrack(player);
    views.untrack(player);
    protocolErrors.remove(player);
  }

  private void onClientResolved(ClientInfo info) {
    if (info.has(Capability.RESOURCES)) {
      resources.track(info.player());
    }
  }

  /** Handles one frame received on the Spectra channel. */
  public void handleFrame(UUID player, byte[] frame) {
    ServerboundPacket packet;
    try {
      packet = Packets.decodeServerbound(frame);
    } catch (ProtocolException e) {
      int errors = protocolErrors.merge(player, 1, Integer::sum);
      platform.logger().warning("Malformed Spectra frame from " + player + ": " + e.getMessage());
      if (errors >= MAX_PROTOCOL_ERRORS) {
        platform.kick(player, "Spectra protocol error");
      }
      return;
    }
    switch (packet) {
      case Hello hello -> sessions.handleHello(player, hello);
      case ResourceRequest request -> resources.handleRequest(player, request);
      case ResourcesSynced synced -> resources.handleSynced(player, synced);
      case ViewPost post -> views.handlePost(player, post);
      case ViewEvent event -> views.handleEvent(player, event);
    }
  }

  @Override
  public void close() {
    if (resourceWatcher != null) {
      resourceWatcher.close();
    }
    if (ticker != null) {
      ticker.cancel();
    }
    sessions.clear();
  }
}
