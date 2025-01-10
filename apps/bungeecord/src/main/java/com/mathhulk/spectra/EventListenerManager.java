package com.mathhulk.spectra;

import org.graalvm.polyglot.Value;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

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

    ProxyServer.getInstance().getPluginManager().unregisterListener(eventListener);
  }

  public Listener addEventListener(Class<? extends Event> eventClass, Value callback) {
    Plugin plugin = script.getPlugin();

    try {
      Listener eventListener = new Listener() {
        @EventHandler
        public void onEvent(Event event) {
          if (!eventClass.isInstance(event)) {
            return;
          }

          callback.execute(event);
        }
      };

      // Register the executor
      ProxyServer.getInstance().getPluginManager().registerListener(plugin, eventListener);

      eventListeners.add(eventListener);

      plugin.getLogger().info("Added event listener: " + eventClass.getName());

      return eventListener;
    } catch (Exception e) {
      // TODO: Track the exception
      plugin.getLogger().severe("Failed to add event listener: " + eventClass.getName());

      e.printStackTrace();

      return null;
    }
  }

  @FunctionalInterface
  public interface RemoveEventListenerFunction {
    void apply(Listener listener);
  }

  @FunctionalInterface
  public interface AddEventListenerFunction {
    Listener apply(Class<? extends Event> eventClass, Value callback);
  }
}