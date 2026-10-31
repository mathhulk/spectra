package com.mathhulk.spectra.protocol;

/** Well-known capability strings. Unknown capabilities must be ignored, not rejected. */
public final class Capability {
  /** Content-addressed resource sync. */
  public static final String RESOURCES = "resources";
  /** Full-screen views. */
  public static final String SCREENS = "ui.screens";
  /** HUD layer views. */
  public static final String LAYERS = "ui.layers";
  /** JSON messages between views and the server. */
  public static final String MESSAGES = "ui.messages";

  private Capability() {
  }
}
