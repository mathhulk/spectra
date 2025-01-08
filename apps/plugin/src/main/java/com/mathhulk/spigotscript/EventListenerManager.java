package com.mathhulk.spigotscript;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;

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

    public Listener addEventListener(Class<? extends Event> eventClass, Value callback) {
        JavaPlugin plugin = script.getPlugin();

        try {
            // Create an EventExecutor
            EventExecutor executor = (_, event) -> {
                if (!eventClass.isInstance(event)) {
                    return;
                }

                callback.execute(event);
            };

            // TODO: What side effects does this listener have?
            Listener eventListener = new Listener() {};

            // Register the executor
            plugin.getServer().getPluginManager().registerEvent(
                eventClass,
                eventListener,
                EventPriority.NORMAL,
                executor,
                plugin
            );

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