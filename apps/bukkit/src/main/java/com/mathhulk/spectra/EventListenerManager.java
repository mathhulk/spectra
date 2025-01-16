package com.mathhulk.spectra;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public class EventListenerManager {
  private final Script script;

  private final ArrayList<Listener> eventListeners = new ArrayList<>();

  public EventListenerManager(Script script) {
    this.script = script;
  }

  public Script getScript() {
    return script;
  }

  public void removeEventListeners() {
    for (Listener eventListener : eventListeners) {
      removeEventListener(eventListener);
    }
  }

  public void removeEventListener(Listener eventListener) {
    if (!eventListeners.contains(eventListener)) {
      return;
    }

    HandlerList.unregisterAll(eventListener);
  }

  public Listener addEventListener(Class<? extends Event> eventClass, EventListenerFunction callback,
      EventPriority... optionalParameters) {
    if (optionalParameters.length > 1) {
      throw new IllegalArgumentException("Expected at most 3 arguments but found " + (optionalParameters.length + 2));
    }

    if (optionalParameters.length == 0) {
      return registerEventListener(eventClass, callback, EventPriority.NORMAL);
    }

    return registerEventListener(eventClass, callback, optionalParameters[0]);
  }

  public Listener registerEventListener(Class<? extends Event> eventClass, EventListenerFunction callback,
      EventPriority priority) {
    Plugin plugin = script.getPlugin();

    try {
      EventExecutor executor = (_, event) -> {
        if (!eventClass.isInstance(event)) {
          return;
        }

        callback.apply(event);
      };

      Listener eventListener = new Listener() {
      };

      // Register the executor
      plugin.getServer().getPluginManager().registerEvent(
          eventClass,
          eventListener,
          priority,
          executor,
          plugin);

      eventListeners.add(eventListener);

      plugin.getLogger().info("Added event listener: " + eventClass.getName());

      return eventListener;
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to add event listener: " + eventClass.getName());

      e.printStackTrace();

      return null;
    }
  }

  @FunctionalInterface
  public interface EventListenerFunction {
    void apply(Event event);
  }

  @FunctionalInterface
  public interface RemoveEventListenerFunction {
    void apply(Listener eventListener);
  }

  @FunctionalInterface
  public interface AddEventListenerFunction {
    Listener apply(Class<? extends Event> eventClass, EventListenerFunction callback,
        EventPriority... optionalParameters);
  }
}