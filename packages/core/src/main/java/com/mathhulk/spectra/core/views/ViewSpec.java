package com.mathhulk.spectra.core.views;

import com.mathhulk.spectra.protocol.ResourcePath;
import com.mathhulk.spectra.protocol.clientbound.Layout;
import com.mathhulk.spectra.protocol.clientbound.ViewKind;

import java.util.Optional;

/** What to open: a resource rendered as a screen or a HUD layer. */
public record ViewSpec(ViewKind kind, String resource, Layout layout, int zIndex, boolean visible,
    Optional<String> after) {
  public ViewSpec {
    ResourcePath.requireValid(resource);
  }

  public static ViewSpec screen(String resource) {
    return new ViewSpec(ViewKind.SCREEN, resource, Layout.FULLSCREEN, 0, true, Optional.empty());
  }

  public static ViewSpec layer(String resource, Layout layout) {
    return new ViewSpec(ViewKind.LAYER, resource, layout, 0, true, Optional.empty());
  }
}
