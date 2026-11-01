package com.mathhulk.spectra.core;

import java.util.List;
import java.util.OptionalInt;

/**
 * Operator configuration. Defaults never kick anyone: requiring the client mod is an explicit opt-in, and
 * individual scripts or plugins can enforce it on demand instead.
 *
 * @param requireClient         kick players whose client does not complete the handshake
 * @param minClientVersion      when present, also kick clients below this protocol version
 * @param kickMessage           message for kicked players
 * @param handshakeTimeoutTicks how long to wait for a client Hello before treating a player as unsupported
 * @param watchScripts          hot-reload scripts and resources when files change
 * @param resourceBytesPerTick  resource upload budget per player per tick
 * @param networkAllow          internet origins views may contact directly (sent to clients)
 */
public record SpectraConfig(boolean requireClient, OptionalInt minClientVersion, String kickMessage,
    long handshakeTimeoutTicks, boolean watchScripts, int resourceBytesPerTick, List<String> networkAllow) {
  public static final SpectraConfig DEFAULTS = new SpectraConfig(false, OptionalInt.empty(),
      "This server requires the Spectra client mod.", 100, true, 128 * 1024, List.of());

  public SpectraConfig {
    if (handshakeTimeoutTicks < 1) {
      throw new IllegalArgumentException("handshakeTimeoutTicks must be positive");
    }
    if (resourceBytesPerTick < 1024) {
      throw new IllegalArgumentException("resourceBytesPerTick must be at least 1024");
    }
    networkAllow = List.copyOf(networkAllow);
  }
}
