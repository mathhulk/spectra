package com.mathhulk.spectra.core.platform;

import com.mathhulk.spectra.api.Registration;

/** Handle for a scheduled task or registration. Cancelling twice is harmless. */
@FunctionalInterface
public interface Cancellable extends Registration {
  @Override
  void cancel();

  @Override
  default void unregister() {
    cancel();
  }
}
