package com.mathhulk.spectra.core;

import com.mathhulk.spectra.api.Registration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A small listener list. One failing listener never prevents the others from running. */
public final class Listeners<T> {
  private final List<Consumer<? super T>> listeners = new CopyOnWriteArrayList<>();
  private final Logger logger;
  private final String name;

  public Listeners(Logger logger, String name) {
    this.logger = logger;
    this.name = name;
  }

  public Registration add(Consumer<? super T> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  public void fire(T value) {
    for (Consumer<? super T> listener : listeners) {
      try {
        listener.accept(value);
      } catch (RuntimeException e) {
        logger.log(Level.SEVERE, "A " + name + " listener failed", e);
      }
    }
  }
}
