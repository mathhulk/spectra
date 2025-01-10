package com.mathhulk.spectra;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class SpectraBungeeCordPlugin extends Plugin {
  private ScriptManager scriptManager;

  @Override
  public void onEnable() {
    ProxyServer.getInstance().getPluginManager().registerCommand(this, new ScriptsCommand(this));

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