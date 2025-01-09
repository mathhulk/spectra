package com.mathhulk.spectra;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Spectra extends JavaPlugin {
  private ScriptManager scriptManager;

  @Override
  public void onEnable() {
    PluginCommand command = getCommand("scripts");

    if (command == null) {
      getLogger().severe("Failed to get scripts command");

      return;
    }

    ScriptsCommand scriptsCommand = new ScriptsCommand(this);
    command.setExecutor(scriptsCommand);
    command.setTabCompleter(scriptsCommand);

    scriptManager = new ScriptManager(this);
    scriptManager.enable(true);
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