package com.mathhulk.spectra.core.scripts;

/** One loaded script in some language. Every method is called on the main thread. */
public interface ScriptInstance extends AutoCloseable {
  /** Evaluates the script (top-level code). Throwing leaves the script unloaded. */
  void load() throws Exception;

  /** Calls the script's enable hook, if any. */
  void enable() throws Exception;

  /** Calls the script's disable hook, if any. Never throws past the manager; errors are logged. */
  void disable() throws Exception;

  /**
   * A value the script exports (for example a world generator factory), as the language's own handle, or null.
   * Adapters must call into it through the script's {@link ScriptScope#invoke} lock.
   */
  default Object export(String name) {
    return null;
  }

  /** Releases the language runtime (contexts, VMs). The script's scope is disposed separately. */
  @Override
  void close();
}
