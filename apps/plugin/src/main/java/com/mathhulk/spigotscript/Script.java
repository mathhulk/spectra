package com.mathhulk.spigotscript;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.File;

public class Script {
    private Boolean enabled = false;

    private Context context;
    private EventListenerManager eventListenerManager;
    private CommandManager commandManager;
    private TaskManager taskManager;

    private final SpigotScript plugin;
    private final File file;

    private final String name;

    private final String language = "js";

    public Script(File file, SpigotScript plugin) {
        this.file = file;
        this.plugin = plugin;

        name = file.getName();
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public SpigotScript getPlugin() {
        return plugin;
    }

    public Boolean enable() {
        if (enabled) return false;

        eventListenerManager = new EventListenerManager(this);
        commandManager = new CommandManager(this);
        taskManager = new TaskManager(this);

        try {
            context = Context.newBuilder(language).allowAllAccess(true).build();

            // TODO: Determine what to expose
            context.getBindings(language).putMember("Server", plugin.getServer());
            context.getBindings(language).putMember("Logger", plugin.getLogger());

            context.getBindings(language).putMember("addCommand", (CommandManager.AddCommandFunction) commandManager::addCommand);
            context.getBindings(language).putMember("removeCommand", (CommandManager.RemoveCommandFunction) commandManager::removeCommand);

            context.getBindings(language).putMember("addEventListener", (EventListenerManager.AddEventListenerFunction) eventListenerManager::addEventListener);
            context.getBindings(language).putMember("removeEventListener", (EventListenerManager.RemoveEventListenerFunction) eventListenerManager::removeEventListener);

            context.getBindings(language).putMember("setInterval", (TaskManager.SetIntervalFunction) taskManager::setInterval);
            context.getBindings(language).putMember("clearInterval", (TaskManager.ClearIntervalFunction) taskManager::clearInterval);

            context.getBindings(language).putMember("setTimeout", (TaskManager.SetTimeoutFunction) taskManager::setTimeout);
            context.getBindings(language).putMember("clearTimeout", (TaskManager.ClearTimeoutFunction) taskManager::clearTimeout);

            context.eval(Source.newBuilder(language, file).build());

            enabled = true;

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable script: " + file.getName());

            e.printStackTrace();

            return false;
        }
    }

    public Boolean disable() {
        if (!enabled) return false;

        taskManager.removeTasks();
        eventListenerManager.removeEventListeners();
        commandManager.removeCommands();
        context.close();

        enabled = false;

        return true;
    }
}
