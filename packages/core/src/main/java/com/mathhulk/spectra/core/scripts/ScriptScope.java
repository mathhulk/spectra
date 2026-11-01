package com.mathhulk.spectra.core.scripts;

import com.mathhulk.spectra.api.Registration;
import com.mathhulk.spectra.core.platform.Cancellable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns everything a script registers (listeners, commands, timers, view handlers). Disposing the scope undoes all
 * of it in reverse order, so unloading or hot-reloading a script can never leak registrations.
 *
 * <p>The scope also guards entry into the script's runtime. Language runtimes are single-threaded, so every call
 * into script code (lifecycle hooks, timers, event handlers, commands) goes through {@link #invoke}, which holds
 * the scope's lock. Most calls happen on the main thread; asynchronous platform events (such as chat) enter from
 * their own thread and simply wait for the lock.
 */
public final class ScriptScope {
  private final String scriptName;
  private final Logger logger;
  private final Deque<Registration> resources = new ArrayDeque<>();
  private final ReentrantLock lock = new ReentrantLock();
  private volatile boolean disposed;

  public ScriptScope(String scriptName, Logger logger) {
    this.scriptName = scriptName;
    this.logger = logger;
  }

  public String scriptName() {
    return scriptName;
  }

  /** Runs script code under this scope's lock. Errors are logged and swallowed; returns {@code fallback} then. */
  public <T> T invoke(String what, Supplier<T> call, T fallback) {
    if (disposed) {
      return fallback;
    }
    lock.lock();
    try {
      return disposed ? fallback : call.get();
    } catch (RuntimeException e) {
      logger.log(Level.SEVERE, "Error in script " + scriptName + " (" + what + "): " + e.getMessage(), e);
      return fallback;
    } finally {
      lock.unlock();
    }
  }

  /** Runs script code under this scope's lock, logging errors. */
  public void invoke(String what, Runnable call) {
    invoke(what, () -> {
      call.run();
      return null;
    }, null);
  }

  /** Runs code under the lock and lets exceptions propagate (used for load, where failure matters). */
  public <T> T locked(Supplier<T> call) {
    lock.lock();
    try {
      return call.get();
    } finally {
      lock.unlock();
    }
  }

  public boolean isDisposed() {
    return disposed;
  }

  /** Registers a resource to release on dispose. Returns a handle that releases it early and untracks it. */
  public Cancellable track(Registration resource) {
    if (disposed) {
      resource.unregister();
      throw new IllegalStateException("Script " + scriptName + " is unloaded");
    }
    resources.push(resource);
    return () -> {
      if (resources.remove(resource)) {
        resource.unregister();
      }
    };
  }

  public void dispose() {
    lock.lock();
    try {
      if (disposed) {
        return;
      }
      disposed = true;
    } finally {
      lock.unlock();
    }
    while (!resources.isEmpty()) {
      Registration resource = resources.pop();
      try {
        resource.unregister();
      } catch (RuntimeException e) {
        logger.log(Level.WARNING, "Failed to release a resource of script " + scriptName, e);
      }
    }
  }
}
