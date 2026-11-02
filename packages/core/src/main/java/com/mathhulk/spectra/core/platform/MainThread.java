package com.mathhulk.spectra.core.platform;

/**
 * The thread that owns game state. All script code and all core state changes run here. Durations are in ticks
 * (1 tick = 50 ms).
 */
public interface MainThread {
  long MILLIS_PER_TICK = 50;

  boolean isMainThread();

  /** Runs {@code task} on the main thread: immediately if already on it, otherwise on the next tick. */
  void execute(Runnable task);

  Cancellable runLater(Runnable task, long delayTicks);

  Cancellable runRepeating(Runnable task, long delayTicks, long periodTicks);

  /** Converts milliseconds to ticks, rounding to nearest and never returning less than {@code min}. */
  static long toTicks(double millis, long min) {
    if (Double.isNaN(millis) || millis <= 0) {
      return min;
    }
    return Math.max(min, Math.round(millis / MILLIS_PER_TICK));
  }
}
