package com.mathhulk.spigotscript;

import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Context;

public class Script {
    private final Context context;
    private final String source;

    private final EventListenerManager eventListenerManager;
    private final CommandManager commandManager;
    private final TaskManager taskManager;

    private final String name;
    private final JavaPlugin plugin;

    public Script(String name, String source, JavaPlugin plugin) {
        this.source = source;
        this.plugin = plugin;
        this.name = name;

        // Initialize the GraalJS context
        context = Context.newBuilder("js")
                // Allow Java access
                .allowAllAccess(true)
                .build();

        eventListenerManager = new EventListenerManager(this);
        commandManager = new CommandManager(this);
        taskManager = new TaskManager(this);

        // TODO: Determine what to expose
        context.getBindings("js").putMember("Server", plugin.getServer());
        context.getBindings("js").putMember("Logger", plugin.getLogger());

        context.getBindings("js").putMember("addCommand", (CommandManager.AddCommandFunction) commandManager::addCommand);
        context.getBindings("js").putMember("removeCommand", (CommandManager.RemoveCommandFunction) commandManager::removeCommand);

        context.getBindings("js").putMember("addEventListener", (EventListenerManager.AddEventListenerFunction) eventListenerManager::addEventListener);
        context.getBindings("js").putMember("removeEventListener", (EventListenerManager.RemoveEventListenerFunction) eventListenerManager::removeEventListener);

        context.getBindings("js").putMember("setInterval", (TaskManager.SetIntervalFunction) taskManager::setInterval);
        context.getBindings("js").putMember("clearInterval", (TaskManager.ClearIntervalFunction) taskManager::clearInterval);

        context.getBindings("js").putMember("setTimeout", (TaskManager.SetTimeoutFunction) taskManager::setTimeout);
        context.getBindings("js").putMember("clearTimeout", (TaskManager.ClearTimeoutFunction) taskManager::clearTimeout);
    }

    public String getName() {
        return name;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getSource() {
        return source;
    }

    public void execute() {
        context.eval("js", this.source);
    }

    public void close() {
        taskManager.removeTasks();
        eventListenerManager.removeEventListeners();
        commandManager.removeCommands();
        context.close();
    }
}
