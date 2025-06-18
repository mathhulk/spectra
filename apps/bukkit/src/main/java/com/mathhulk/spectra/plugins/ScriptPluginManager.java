package com.mathhulk.spectra.plugins;

import java.io.File;
import java.util.HashSet;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.mathhulk.spectra.Spectra;

public class ScriptPluginManager {
  private ScriptPluginLoader pluginLoader;
  private HashSet<ScriptPlugin> plugins = new HashSet<>();

  private final Spectra plugin;

  public ScriptPluginManager(Spectra plugin) {
    this.plugin = plugin;
  }

  public Spectra getPlugin() {
    return plugin;
  }

  public ScriptPluginLoader getPluginLoader() {
    return pluginLoader;
  }

  public HashSet<ScriptPlugin> getPlugins() {
    return plugins;
  }

  public void load() {
    // Register a custom plugin loader for Spectra plugins
    PluginManager pluginManager = plugin.getServer().getPluginManager();
    pluginManager.registerInterface(ScriptPluginLoader.class);

    plugin.getLogger().info("Loading Spectra plugins...");

    File[] files = plugin.getDataFolder().getParentFile().listFiles();

    if (files == null)
      return;

    for (File file : files) {
      if (!ScriptPluginLoader.isSpectraPluginFile(file.getName()))
        continue;

      try {
        // Attempt to load Spectra plugins
        Plugin loadedPlugin = pluginManager.loadPlugin(file);

        if (loadedPlugin == null) {
          plugin.getLogger().severe("Failed to load Spectra plugin: " + file.getName());

          continue;
        }

        plugins.add((ScriptPlugin) loadedPlugin);

        plugin.getLogger().info("Loaded Spectra plugin: " + loadedPlugin.getName());

        // Store the ScriptPluginLoader instance
        if (pluginLoader != null) {
          continue;
        }

        pluginLoader = (ScriptPluginLoader) loadedPlugin.getPluginLoader();
      } catch (Exception e) {
        plugin.getLogger().severe("Failed to load Spectra plugin: " + file.getName());

        e.printStackTrace();
      }
    }
  }
}
