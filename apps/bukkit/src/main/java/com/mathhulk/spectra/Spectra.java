package com.mathhulk.spectra;

import org.bukkit.command.PluginCommand;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import com.mathhulk.spectra.plugins.ScriptPluginManager;
import com.mathhulk.spectra.scripts.ScriptManager;
import com.mathhulk.spectra.ui.VerifiedPlayerManager;

public class Spectra extends JavaPlugin {
  private ScriptManager scriptManager;
  private ScriptPluginManager scriptPluginManager;
  private VerifiedPlayerManager verifiedPlayerManager;

  public static Spectra instance;

  @Override
  public void onLoad() {
    // Register ScriptPluginLoader
    instance = this;

    verifiedPlayerManager = new VerifiedPlayerManager(this);

    // TODO: Add a flag to disable script plugins
    scriptPluginManager = new ScriptPluginManager(this);
    scriptPluginManager.load();

    // TODO: Add a flag to disable the script manager
    scriptManager = new ScriptManager(this);
    // TODO: Add a flag to disable watching by default
    scriptManager.load(true);
  }

  @Override
  public void onEnable() {
    PluginCommand command = getCommand("scripts");

    if (command == null) {
      getLogger().severe("Failed to get spectra command");

      return;
    }

    ScriptsCommand scriptsCommand = new ScriptsCommand(this);
    command.setExecutor(scriptsCommand);
    command.setTabCompleter(scriptsCommand);

    // Enable scripts
    if (scriptManager != null) {
      scriptManager.enable();
    }

    if (verifiedPlayerManager != null) {
      verifiedPlayerManager.enable();
    }
  }

  @Override
  public void onDisable() {
    // Disable scripts
    if (scriptManager != null) {
      scriptManager.disable();
    }

    if (verifiedPlayerManager != null) {
      verifiedPlayerManager.disable();
    }
  }

  public ScriptManager getScriptManager() {
    return scriptManager;
  }

  public ScriptPluginManager getScriptPluginManager() {
    return scriptPluginManager;
  }

  @Override
  public BiomeProvider getDefaultBiomeProvider(String worldName, String id) {
    BiomeProvider biomeProvider = scriptManager.getDefaultBiomeProvider(worldName,
        id);

    if (biomeProvider != null) {
      return biomeProvider;
    }

    return super.getDefaultBiomeProvider(worldName, id);
  }

  @Override
  public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
    ChunkGenerator chunkGenerator = scriptManager.getDefaultWorldGenerator(worldName, id);

    if (chunkGenerator != null) {
      return chunkGenerator;
    }

    return super.getDefaultWorldGenerator(worldName, id);
  }
}