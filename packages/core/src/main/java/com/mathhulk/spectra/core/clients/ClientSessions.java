package com.mathhulk.spectra.core.clients;

import com.mathhulk.spectra.core.Listeners;
import com.mathhulk.spectra.core.SpectraConfig;
import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.protocol.Capability;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.Protocol;
import com.mathhulk.spectra.protocol.clientbound.Welcome;
import com.mathhulk.spectra.protocol.serverbound.Hello;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Tracks the handshake state of every online player. Main thread only.
 *
 * <p>Nothing here kicks players unless the operator enabled {@link SpectraConfig#requireClient()}. Scripts can
 * enforce support themselves with {@link #resolve(UUID)}.
 */
public final class ClientSessions {
  /** Capabilities this server implementation supports. */
  public static final Set<String> SERVER_CAPABILITIES =
      Set.of(Capability.RESOURCES, Capability.SCREENS, Capability.LAYERS, Capability.MESSAGES);

  private static final class Session {
    ClientInfo info;
    Cancellable timeout;
    final List<CompletableFuture<ClientInfo>> waiting = new ArrayList<>();

    Session(ClientInfo info) {
      this.info = info;
    }
  }

  private final Platform platform;
  private final SpectraConfig config;
  private final String serverId;
  private final Map<UUID, Session> sessions = new HashMap<>();
  private final Listeners<ClientInfo> resolved;

  public ClientSessions(Platform platform, SpectraConfig config, String serverId) {
    this.platform = platform;
    this.config = config;
    this.serverId = serverId;
    this.resolved = new Listeners<>(platform.logger(), "client resolved");
  }

  /** Fires once per join, when a player's client status becomes known (supported or not). */
  public Cancellable onResolved(Consumer<? super ClientInfo> listener) {
    return resolved.add(listener);
  }

  public void join(UUID player) {
    quit(player);
    Session session = new Session(ClientInfo.pending(player));
    session.timeout = platform.mainThread().runLater(() -> timeOut(player, session), config.handshakeTimeoutTicks());
    sessions.put(player, session);
  }

  public void quit(UUID player) {
    Session session = sessions.remove(player);
    if (session == null) {
      return;
    }
    session.timeout.cancel();
    ClientInfo gone = new ClientInfo(player, ClientStatus.UNSUPPORTED, 0, Optional.empty(), Set.of());
    session.waiting.forEach(future -> future.complete(gone));
  }

  /** Current knowledge about a player; {@link ClientStatus#PENDING} until the handshake resolves. */
  public ClientInfo info(UUID player) {
    Session session = sessions.get(player);
    return session == null
        ? new ClientInfo(player, ClientStatus.UNSUPPORTED, 0, Optional.empty(), Set.of())
        : session.info;
  }

  /** Completes once the player's client status is known. Completes immediately if it already is. */
  public CompletableFuture<ClientInfo> resolve(UUID player) {
    Session session = sessions.get(player);
    if (session == null || session.info.isResolved()) {
      return CompletableFuture.completedFuture(info(player));
    }
    CompletableFuture<ClientInfo> future = new CompletableFuture<>();
    session.waiting.add(future);
    return future;
  }

  public void handleHello(UUID player, Hello hello) {
    Session session = sessions.get(player);
    if (session == null || session.info.isResolved()) {
      // Duplicate or late Hello: ignore rather than letting a client flip its own status.
      return;
    }
    session.timeout.cancel();

    if (!Protocol.isSupported(hello.version())) {
      String reason = "Unsupported Spectra protocol " + hello.version() + " (server supports "
          + Protocol.MIN_SUPPORTED_VERSION + "-" + Protocol.VERSION + ")";
      platform.sendFrame(player, Packets.encode(new Welcome(Protocol.VERSION, false, reason, serverId, Set.of())));
      finish(session, new ClientInfo(player, ClientStatus.INCOMPATIBLE, hello.version(),
          Optional.of(hello.modVersion()), Set.of()));
      return;
    }

    platform.sendFrame(player,
        Packets.encode(new Welcome(Protocol.VERSION, true, "", serverId, SERVER_CAPABILITIES)));
    Set<String> shared = new java.util.HashSet<>(hello.capabilities());
    shared.retainAll(SERVER_CAPABILITIES);
    finish(session, new ClientInfo(player, ClientStatus.SUPPORTED, hello.version(), Optional.of(hello.modVersion()),
        shared));
  }

  private void timeOut(UUID player, Session session) {
    if (sessions.get(player) == session && !session.info.isResolved()) {
      finish(session, new ClientInfo(player, ClientStatus.UNSUPPORTED, 0, Optional.empty(), Set.of()));
    }
  }

  private void finish(Session session, ClientInfo info) {
    session.info = info;
    List<CompletableFuture<ClientInfo>> waiting = List.copyOf(session.waiting);
    session.waiting.clear();
    waiting.forEach(future -> future.complete(info));

    if (shouldKick(info)) {
      platform.kick(info.player(), config.kickMessage());
      return;
    }
    resolved.fire(info);
  }

  private boolean shouldKick(ClientInfo info) {
    if (!config.requireClient()) {
      return false;
    }
    if (!info.isSupported()) {
      return true;
    }
    return config.minClientVersion().isPresent() && info.protocolVersion() < config.minClientVersion().getAsInt();
  }

  public void clear() {
    List.copyOf(sessions.keySet()).forEach(this::quit);
  }
}
