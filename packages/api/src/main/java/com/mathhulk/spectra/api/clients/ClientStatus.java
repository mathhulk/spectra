package com.mathhulk.spectra.api.clients;

/** What is known about a player's client. */
public enum ClientStatus {
  /** Joined recently; still waiting for the handshake. */
  PENDING,
  /** Completed the handshake with a supported protocol version. */
  SUPPORTED,
  /** No handshake before the timeout: a vanilla or unrelated client. */
  UNSUPPORTED,
  /** Has the mod, but an incompatible protocol version. */
  INCOMPATIBLE
}
