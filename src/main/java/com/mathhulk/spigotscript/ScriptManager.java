package com.mathhulk.spigotscript;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScriptManager {
    private final JavaPlugin plugin;

    private final Map<String, Script> scripts = new HashMap<>();

    private Boolean watching = false;
    private Thread thread;

    public ScriptManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Script addScript(String name, Boolean enable) {
        Script existingScript = scripts.get(name);
        if (existingScript != null) return null;

        File file = new File(plugin.getDataFolder(), "scripts/" + name + ".js");

        if (!file.exists()) {
            plugin.getLogger().info("Script does not exist: " + name);

            return null;
        }

        try {
            String source = Files.readString(file.toPath());

            Script script = new Script(name, source, plugin);
            if (enable) script.enable();

            scripts.put(name, script);

            plugin.getLogger().info("Added script: " + name);

            return script;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add script: " + name);

            e.printStackTrace();

            return null;
        }
    }

    public Boolean isWatching() {
        return watching;
    }

    public void enableScript(String name) {
        Script script = scripts.get(name);
        if (script == null) return;

        script.enable();
    }

    public void disableScript(String name) {
        Script script = scripts.get(name);
        if (script == null) return;

        script.disable();
    }

    public void removeScript(String name) {
        Script script = scripts.get(name);
        if (script == null) return;

        script.disable();
        scripts.remove(name);
    }

    public Script getScript(String name) {
        return scripts.get(name);
    }

    public ArrayList<Script> getScripts() {
        return new ArrayList<>(scripts.values());
    }

    public void enable(Boolean watch) {
        File scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        File[] files = scriptsFolder.listFiles((_, name) -> name.endsWith(".js"));

        if (files != null) {
            for (File file : files) {
                String name = file.getName().substring(0, file.getName().length() - 3);

                addScript(name, true);
            }
        }

        if (!watch) return;

        startWatching();
    }

    public void stopWatching() {
        if (!watching) return;

        watching = false;

        thread.interrupt();

        plugin.getLogger().info("Stopped watching for scripts");
    }

    public void startWatching() {
        thread = new Thread(() -> {
            try {
                watching = true;

                File scriptsFolder = new File(plugin.getDataFolder(), "scripts");
                Path folderPath = scriptsFolder.toPath();

                WatchService watchService = FileSystems.getDefault().newWatchService();
                folderPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                plugin.getLogger().info("Started watching for scripts");

                while (watching) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path filePath = folderPath.resolve((Path) event.context());
                        File file = filePath.toFile();

                        String fileName = file.getName();

                        if (!fileName.endsWith(".js")) {
                            continue;
                        }

                        String name = fileName.substring(0, fileName.length() - 3);

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            addScript(name, true);
                        }

                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            removeScript(name);
                            addScript(name, true);
                        }

                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            removeScript(name);
                        }
                    }

                    key.reset();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Stopped watching for scripts");

                watching = false;
            }
        });

        thread.start();
    }

    public void disable() {
        if (watching) stopWatching();

        scripts.keySet().forEach((name) -> {
            scripts.get(name).disable();
            scripts.remove(name);
        });
    }
}
