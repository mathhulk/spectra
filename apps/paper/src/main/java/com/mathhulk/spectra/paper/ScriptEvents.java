package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * {@code addEventListener(EventClass, handler, priority?, ignoreCancelled?)} for scripts. Handlers run on the
 * thread that fired the event (the main thread, or the event's own thread for asynchronous events such as chat),
 * always under the script's lock.
 */
final class ScriptEvents {
  @FunctionalInterface
  public interface AddEventListener {
    Cancellable apply(Class<?> eventClass, Consumer<Object> handler, Object... options);
  }

  @FunctionalInterface
  public interface RemoveEventListener {
    void apply(Cancellable listener);
  }

  private ScriptEvents() {
  }

  static AddEventListener add(Plugin plugin, ScriptScope scope) {
    return (eventClass, handler, options) -> {
      if (!Event.class.isAssignableFrom(eventClass)) {
        throw new IllegalArgumentException(eventClass.getName() + " is not a Bukkit event");
      }
      if (options.length > 2) {
        throw new IllegalArgumentException("addEventListener takes at most 4 arguments");
      }
      EventPriority priority = options.length > 0 && options[0] != null ? priority(options[0]) : EventPriority.NORMAL;
      boolean ignoreCancelled = options.length > 1 && Boolean.TRUE.equals(options[1]);

      @SuppressWarnings("unchecked")
      Class<? extends Event> type = (Class<? extends Event>) eventClass;
      String what = "event " + type.getSimpleName();
      Listener listener = new Listener() {
      };
      Bukkit.getPluginManager().registerEvent(type, listener, priority, (ignored, event) -> {
        if (type.isInstance(event)) {
          scope.invoke(what, () -> handler.accept(event));
        }
      }, plugin, ignoreCancelled);
      return scope.track(() -> HandlerList.unregisterAll(listener));
    };
  }

  static RemoveEventListener remove() {
    return listener -> {
      if (listener != null) {
        listener.cancel();
      }
    };
  }

  private static EventPriority priority(Object value) {
    if (value instanceof EventPriority priority) {
      return priority;
    }
    return EventPriority.valueOf(String.valueOf(value).toUpperCase(Locale.ROOT));
  }
}
