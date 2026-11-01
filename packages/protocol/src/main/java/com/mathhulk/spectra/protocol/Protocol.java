package com.mathhulk.spectra.protocol;

/** Protocol-wide constants. See PROTOCOL.md. */
public final class Protocol {
  /** The plugin-message channel used in both directions. */
  public static final String CHANNEL = "spectra:main";

  /** Current protocol version. Bump on any incompatible change. */
  public static final int VERSION = 2;

  /** Oldest client protocol version this build can still talk to. */
  public static final int MIN_SUPPORTED_VERSION = 2;

  private Protocol() {
  }

  public static boolean isSupported(int version) {
    return version >= MIN_SUPPORTED_VERSION && version <= VERSION;
  }
}
