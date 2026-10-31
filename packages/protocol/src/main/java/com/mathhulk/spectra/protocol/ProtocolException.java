package com.mathhulk.spectra.protocol;

/** Thrown when a frame is malformed, too large, or otherwise violates the protocol. */
public final class ProtocolException extends RuntimeException {
  public ProtocolException(String message) {
    super(message);
  }
}
