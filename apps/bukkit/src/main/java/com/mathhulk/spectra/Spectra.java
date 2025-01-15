package com.mathhulk.spectra;

import java.io.File;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.mathhulk.spectra.plugin.ScriptPlugin;
import com.mathhulk.spectra.plugin.ScriptPluginLoader;

public class Spectra extends JavaPlugin {
  private ScriptManager scriptManager;
  private ScriptPluginLoader pluginLoader;

  public static Spectra instance;

  @Override
  public void onLoad() {
    // Register ScriptPluginLoader
    instance = this;

    PluginManager pluginManager = getServer().getPluginManager();
    pluginManager.registerInterface(ScriptPluginLoader.class);

    // Load Spectra plugins
    getLogger().info("Loading Spectra plugins...");

    File[] plugins = getDataFolder().getParentFile().listFiles();

    if (plugins == null)
      return;

    for (File file : plugins) {
      if (!ScriptPluginLoader.isSpectraPluginFile(file.getName()))
        continue;

      try {
        Plugin loadedPlugin = pluginManager.loadPlugin(file);

        if (loadedPlugin == null) {
          getLogger().severe("Failed to load " + file.getName());

          continue;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Store the ScriptPluginLoader instance
    for (Plugin plugin : pluginManager.getPlugins()) {
      getLogger().info("Checking " + plugin.getName());

      if (!(plugin instanceof ScriptPlugin))
        continue;

      pluginLoader = (ScriptPluginLoader) plugin.getPluginLoader();

      break;
    }

    // TODO: Add a flag to disable the script manager
    scriptManager = new ScriptManager(this);

    // TODO: Add a flag to disable watching by default
    scriptManager.load(true);
  }

  @Override
  public void onEnable() {
    // Register the command
    PluginCommand command = getCommand("scripts");

    if (command == null) {
      getLogger().severe("Failed to get spectra command");

      return;
    }

    ScriptsCommand scriptsCommand = new ScriptsCommand(this);
    command.setExecutor(scriptsCommand);
    command.setTabCompleter(scriptsCommand);

    // Log the number of Spectra plugins loaded
    int totalPlugins = pluginLoader == null ? 0 : pluginLoader.getPlugins().size();

    getLogger().info(totalPlugins == 0 ? "No Spectra plugins loaded"
        : totalPlugins == 1 ? "Loaded 1 Spectra plugin" : "Loaded " + totalPlugins + " Spectra plugins");

    // Enable scripts
    if (scriptManager == null)
      return;

    scriptManager.enable();
  }

  @Override
  public void onDisable() {
    if (scriptManager == null)
      return;

    scriptManager.disable();
  }

  public ScriptManager getScriptManager() {
    return scriptManager;
  }
}