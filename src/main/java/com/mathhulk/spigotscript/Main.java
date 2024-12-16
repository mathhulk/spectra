package com.mathhulk.spigotscript;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        scriptManager = new ScriptManager(this);
        scriptManager.initialize();
    }

    @Override
    public void onDisable() {
        scriptManager.close();
    }
}