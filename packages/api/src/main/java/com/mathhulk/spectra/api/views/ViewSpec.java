package com.mathhulk.spectra.api.views;

import java.util.Optional;

/**
 * What to open. {@code resource} is a path in Spectra's resource tree, e.g. {@code "myplugin/shop/index.html"}
 * for files registered under the {@code myplugin} namespace. {@code after} names a vanilla HUD element a layer
 * renders after ("chat", "hotbar", ...); empty renders on top.
 */
public record ViewSpec(ViewKind kind, String resource, Layout layout, int zIndex, boolean visible,
    Optional<String> after) {
  public ViewSpec {
    if (resource == null || resource.isEmpty() || resource.startsWith("/") || resource.contains("..")) {
      throw new IllegalArgumentException("Invalid resource path: " + resource);
    }
  }

  public static ViewSpec screen(String resource) {
    return new ViewSpec(ViewKind.SCREEN, resource, Layout.FULLSCREEN, 0, true, Optional.empty());
  }

  public static ViewSpec layer(String resource, Layout layout) {
    return new ViewSpec(ViewKind.LAYER, resource, layout, 0, true, Optional.empty());
  }

  public ViewSpec withVisible(boolean visible) {
    return new ViewSpec(kind, resource, layout, zIndex, visible, after);
  }
}
