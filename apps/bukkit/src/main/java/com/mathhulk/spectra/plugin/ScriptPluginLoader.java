package com.mathhulk.spectra.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;

import com.mathhulk.spectra.Script;
import com.mathhulk.spectra.Spectra;

public class ScriptPluginLoader implements PluginLoader {
  private static final Pattern[] pluginFileFilters = new Pattern[] {
      Pattern.compile(".*\\.js$"),
      Pattern.compile(".*\\.mjs$")
  };

  private final ArrayList<ScriptPlugin> plugins = new ArrayList<>();

  private final Server server;

  public ScriptPluginLoader(Server server) {
    this.server = server;
  }

  @Override
  public Plugin loadPlugin(File file) {
    ScriptPlugin plugin = new ScriptPlugin(this, server);
    Script script = new Script(file, plugin);

    plugin.setScript(script);
    plugins.add(plugin);

    return plugin;
  }

  @Override
  public PluginDescriptionFile getPluginDescription(File file) {
    return new PluginDescriptionFile("Test", "1.0.0", "com.mathhulk.spectra.ScriptPlugin");
  }

  public static boolean isSpectraPluginFile(String fileName) {
    for (Pattern pattern : pluginFileFilters) {
      if (pattern.matcher(fileName).find())
        return true;
    }

    return false;
  }

  @Override
  public Pattern[] getPluginFileFilters() {
    return pluginFileFilters;
  }

  public ArrayList<ScriptPlugin> getPlugins() {
    return plugins;
  }

  @Override
  public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener,
      Plugin plugin) {
    return Spectra.instance.getPluginLoader().createRegisteredListeners(listener, plugin);
  }

  @Override
  public void enablePlugin(Plugin plugin) {
    plugin.onEnable();
  }

  @Override
  public void disablePlugin(Plugin plugin) {
    plugin.onDisable();
  }
}
