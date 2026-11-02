package com.mathhulk.spectra.api;

/** Undoes a registration (listener, route, resource namespace). Calling it more than once is harmless. */
@FunctionalInterface
public interface Registration extends AutoCloseable {
  void unregister();

  /** Alias of {@link #unregister()}, so script handles read naturally ({@code listener.cancel()}). */
  default void cancel() {
    unregister();
  }

  @Override
  default void close() {
    unregister();
  }
}
