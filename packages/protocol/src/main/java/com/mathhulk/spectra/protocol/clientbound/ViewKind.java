package com.mathhulk.spectra.protocol.clientbound;

/** What kind of UI a view is. Ordinals are part of the wire format: only append. */
public enum ViewKind {
  /** Full-screen, focused UI. */
  SCREEN,
  /** HUD overlay without input focus. */
  LAYER
}
