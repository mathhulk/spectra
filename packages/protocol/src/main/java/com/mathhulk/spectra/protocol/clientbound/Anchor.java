package com.mathhulk.spectra.protocol.clientbound;

/** Where a view's layout offsets are measured from. Ordinals are part of the wire format: only append. */
public enum Anchor {
  TOP_LEFT, TOP, TOP_RIGHT,
  LEFT, CENTER, RIGHT,
  BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
}
