package com.mathhulk.spectra.api.clients;

import com.mathhulk.spectra.api.Registration;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** The Spectra handshake: which players have a compatible client, and what it can do. */
public interface Clients {
  /** Current knowledge; {@link ClientStatus#PENDING} until the handshake resolves or times out. */
  ClientInfo info(UUID player);

  /** Completes (on the main thread) once the player's status is known; immediately if it already is. */
  CompletableFuture<ClientInfo> resolve(UUID player);

  /** Fires when a joining player's status becomes known. */
  Registration onResolved(Consumer<ClientInfo> listener);
}
