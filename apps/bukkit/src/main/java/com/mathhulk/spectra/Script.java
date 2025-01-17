package com.mathhulk.spectra;

import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.File;

public class Script {
  private Boolean enabled = false;
  private Boolean loaded = false;

  private Context context;
  private Value exports;

  private EventListenerManager eventListenerManager;
  private CommandManager commandManager;
  private TaskManager taskManager;

  private final Plugin plugin;
  private final File file;

  private final String name;

  private final String language = "js";
  private final String mimeType = "application/javascript+module";

  public Script(File file, Plugin plugin) {
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

  public Plugin getPlugin() {
    return plugin;
  }

  public Boolean isEnabled() {
    return enabled;
  }

  public Boolean isLoaded() {
    return loaded;
  }

  public Boolean load() {
    if (loaded)
      return false;

    eventListenerManager = new EventListenerManager(this);
    commandManager = new CommandManager(this);
    taskManager = new TaskManager(this);

    // TODO: Security
    context = Context.newBuilder(language)
        .allowAllAccess(true)
        .option("js.esm-eval-returns-exports", "true")
        .option("engine.WarnInterpreterOnly", "false")
        .build();

    context.getBindings(language).putMember("addCommand",
        (CommandManager.AddCommandFunction) commandManager::addCommand);
    context.getBindings(language).putMember("removeCommand",
        (CommandManager.RemoveCommandFunction) commandManager::removeCommand);

    context.getBindings(language).putMember("addEventListener",
        (EventListenerManager.AddEventListenerFunction) eventListenerManager::addEventListener);
    context.getBindings(language).putMember("removeEventListener",
        (EventListenerManager.RemoveEventListenerFunction) eventListenerManager::removeEventListener);

    context.getBindings(language).putMember("setInterval",
        (TaskManager.SetIntervalFunction) taskManager::setInterval);
    context.getBindings(language).putMember("clearInterval",
        (TaskManager.ClearIntervalFunction) taskManager::clearInterval);

    context.getBindings(language).putMember("setTimeout", (TaskManager.SetTimeoutFunction) taskManager::setTimeout);
    context.getBindings(language).putMember("clearTimeout",
        (TaskManager.ClearTimeoutFunction) taskManager::clearTimeout);

    try {
      exports = context.eval(Source.newBuilder(language, file).mimeType(mimeType).build());

      loaded = true;

      return true;
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to enable script: " + file.getName());

      e.printStackTrace();

      return false;
    }
  }

  public Boolean enable() {
    // Load the script
    load();

    if (!loaded)
      return false;

    // Execute the onEnable event
    Value enableEvent = exports.getMember("onEnable");

    if (enableEvent != null)
      enableEvent.executeVoid();

    enabled = true;

    return true;
  }

  public Boolean disable() {
    if (!loaded)
      return false;

    // Execute the onDisable event
    Value disableEvent = exports.getMember("onDisable");

    if (disableEvent != null)
      disableEvent.executeVoid();

    // Remove event listeners, commands, and tasks
    taskManager.removeTasks();
    eventListenerManager.removeEventListeners();
    commandManager.removeCommands();

    // Close the context
    context.close();

    enabled = false;
    loaded = false;

    return true;
  }
}
