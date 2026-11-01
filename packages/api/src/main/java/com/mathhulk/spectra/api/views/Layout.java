package com.mathhulk.spectra.api.views;

/** Position and size in GUI-scaled pixels. Width or height 0 fills that axis. */
public record Layout(Anchor anchor, int x, int y, int width, int height) {
  public static final Layout FULLSCREEN = new Layout(Anchor.TOP_LEFT, 0, 0, 0, 0);

  public Layout {
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Negative view size");
    }
  }
}
