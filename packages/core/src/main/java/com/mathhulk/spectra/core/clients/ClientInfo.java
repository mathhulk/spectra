package com.mathhulk.spectra.core.clients;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable snapshot of a player's client support. This is what scripts see when they ask "does this player
 * support custom UIs?".
 */
public record ClientInfo(UUID player, ClientStatus status, int protocolVersion, Optional<String> modVersion,
    Set<String> capabilities) {
  public ClientInfo {
    capabilities = Set.copyOf(capabilities);
  }

  static ClientInfo pending(UUID player) {
    return new ClientInfo(player, ClientStatus.PENDING, 0, Optional.empty(), Set.of());
  }

  public boolean isResolved() {
    return status != ClientStatus.PENDING;
  }

  public boolean isSupported() {
    return status == ClientStatus.SUPPORTED;
  }

  /** True when the client completed the handshake and advertised {@code capability}. */
  public boolean has(String capability) {
    return isSupported() && capabilities.contains(capability);
  }
}
