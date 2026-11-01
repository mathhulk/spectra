package com.mathhulk.spectra.api.resources;

import com.mathhulk.spectra.api.Registration;

import java.nio.file.Path;

/**
 * UI resources synced to clients. Spectra's own directory is the root; plugins add theirs under a namespace, so
 * {@code register("myplugin", dir)} makes {@code dir/shop/index.html} available as {@code myplugin/shop/index.html}.
 * Changes are picked up automatically when watching is enabled, or with {@link #rescan()}.
 */
public interface Resources {
  Registration register(String namespace, Path directory);

  /** Rescans every resource directory and pushes changes to connected clients. */
  void rescan();
}
