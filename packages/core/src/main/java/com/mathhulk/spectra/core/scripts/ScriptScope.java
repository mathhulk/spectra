package com.mathhulk.spectra.core.scripts;

import com.mathhulk.spectra.core.platform.Cancellable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns everything a script registers (listeners, commands, timers, view handlers). Disposing the scope undoes all
 * of it in reverse order, so unloading or hot-reloading a script can never leak registrations.
 */
public final class ScriptScope {
  private final String scriptName;
  private final Logger logger;
  private final Deque<Cancellable> resources = new ArrayDeque<>();
  private boolean disposed;

  public ScriptScope(String scriptName, Logger logger) {
    this.scriptName = scriptName;
    this.logger = logger;
  }

  public String scriptName() {
    return scriptName;
  }

  public boolean isDisposed() {
    return disposed;
  }

  /** Registers a resource to release on dispose. Returns a handle that releases it early and untracks it. */
  public Cancellable track(Cancellable resource) {
    if (disposed) {
      resource.cancel();
      throw new IllegalStateException("Script " + scriptName + " is unloaded");
    }
    resources.push(resource);
    return () -> {
      if (resources.remove(resource)) {
        resource.cancel();
      }
    };
  }

  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    while (!resources.isEmpty()) {
      Cancellable resource = resources.pop();
      try {
        resource.cancel();
      } catch (RuntimeException e) {
        logger.log(Level.WARNING, "Failed to release a resource of script " + scriptName, e);
      }
    }
  }
}
