package com.mathhulk.spectra.core.platform;

/** Handle for a scheduled task. Cancelling twice is harmless. */
@FunctionalInterface
public interface Cancellable extends AutoCloseable {
  void cancel();

  @Override
  default void close() {
    cancel();
  }
}
