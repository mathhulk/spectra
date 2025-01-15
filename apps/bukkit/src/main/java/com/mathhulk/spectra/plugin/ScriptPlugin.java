package com.mathhulk.spectra.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLogger;

import com.mathhulk.spectra.Script;
import com.mathhulk.spectra.Spectra;

public class ScriptPlugin implements Plugin {
  private Script script;

  private final ScriptPluginLoader loader;
  private final PluginLogger logger;
  private final Server server;

  private boolean naggable = true;

  public ScriptPlugin(ScriptPluginLoader loader, Server server) {
    this.loader = loader;
    this.server = server;

    logger = new PluginLogger(this);
  }

  @Override
  public ScriptPluginLoader getPluginLoader() {
    return loader;
  }

  @Override
  public void onEnable() {
    script.enable();
  }

  @Override
  public void onDisable() {
    script.disable();
  }

  public void setScript(Script script) {
    this.script = script;
  }

  @Override
  public String getName() {
    return script.getName();
  }

  @Override
  public void onLoad() {
    // TODO: Implement
    script.enable();
  }

  @Override
  public boolean isEnabled() {
    return script.isEnabled();
  }

  @Override
  public void reloadConfig() {
    // TODO: Implement
  }

  @Override
  public PluginLogger getLogger() {
    return logger;
  }

  @Override
  public Server getServer() {
    return server;
  }

  @Override
  public InputStream getResource(String filename) {
    // TODO: Implement
    return Spectra.instance.getResource(filename);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
    return null;
  }

  @Override
  public void saveDefaultConfig() {
    // TODO: Implement
  }

  @Override
  public FileConfiguration getConfig() {
    // TODO: Implement
    return Spectra.instance.getConfig();
  }

  @Override
  public PluginDescriptionFile getDescription() {
    return new PluginDescriptionFile("Test", "1.0.0", "com.mathhulk.spectra.SpectraPlugin");
  }

  @Override
  public void saveConfig() {
    // TODO: Implement
  }

  @Override
  public BiomeProvider getDefaultBiomeProvider(String worldName, String id) {
    return Spectra.instance.getDefaultBiomeProvider(worldName, id);
  }

  @Override
  public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
    return Spectra.instance.getDefaultWorldGenerator(worldName, id);
  }

  @Override
  public File getDataFolder() {
    // TODO: Implement
    return Spectra.instance.getDataFolder();
  }

  @Override
  public void saveResource(String resourcePath, boolean replace) {
    // TODO: Implement
  }

  @Override
  public void setNaggable(boolean naggable) {
    this.naggable = naggable;
  }

  @Override
  public boolean isNaggable() {
    return naggable;
  }
}
