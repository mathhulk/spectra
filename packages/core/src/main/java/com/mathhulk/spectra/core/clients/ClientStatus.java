package com.mathhulk.spectra.core.clients;

/** What we know about a player's client. */
public enum ClientStatus {
  /** Joined recently; still waiting for a Hello. */
  PENDING,
  /** Completed the handshake with a supported protocol version. */
  SUPPORTED,
  /** Sent no Hello before the timeout: a vanilla or unrelated client. */
  UNSUPPORTED,
  /** Has the mod, but an incompatible protocol version. */
  INCOMPATIBLE
}
