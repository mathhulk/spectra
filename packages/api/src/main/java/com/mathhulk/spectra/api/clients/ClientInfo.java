package com.mathhulk.spectra.api.clients;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Immutable snapshot of a player's client support. */
public record ClientInfo(UUID player, ClientStatus status, int protocolVersion, Optional<String> modVersion,
    Set<String> capabilities) {
  public ClientInfo {
    capabilities = Set.copyOf(capabilities);
  }

  public static ClientInfo unsupported(UUID player) {
    return new ClientInfo(player, ClientStatus.UNSUPPORTED, 0, Optional.empty(), Set.of());
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
