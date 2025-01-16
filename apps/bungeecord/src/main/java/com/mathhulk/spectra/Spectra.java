package com.mathhulk.spectra;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class Spectra extends Plugin {
  private ScriptManager scriptManager;

  @Override
  public void onLoad() {
    scriptManager = new ScriptManager(this);

    // TODO: Add a flag to disable watching by default
    scriptManager.load(true);
  }

  @Override
  public void onEnable() {
    ProxyServer.getInstance().getPluginManager().registerCommand(this, new ScriptsCommand(this));

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