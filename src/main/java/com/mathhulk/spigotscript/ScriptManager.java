package com.mathhulk.spigotscript;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class ScriptManager {
    private final JavaPlugin plugin;

    private final Map<String, Script> scripts = new HashMap<>();

    public ScriptManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        File scriptsFolder = new File(plugin.getDataFolder(), "scripts");

        // Load all .js files in the scripts folder
        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".js"));
        if (files == null) {
            plugin.getLogger().warning("No scripts found in folder: " + scriptsFolder.getAbsolutePath());
            return;
        }

        for (File file : files) {
            String name = file.getName();

            try {
                plugin.getLogger().info("Loading script: " + name);

                String source = Files.readString(file.toPath());

                Script script = new Script(name, source, plugin);
                script.execute();

                plugin.getLogger().info("Successfully executed script: " + name);

                this.scripts.put(name, script);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to read script: " + name);

                e.printStackTrace();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to execute script: " + name);

                e.printStackTrace();
            }
        }

        // Watch the scripts folder for changes in a separate thread
        new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path folderPath = scriptsFolder.toPath();

                // Register the folder for create, modify, and delete events
                folderPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                while (true) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path filePath = folderPath.resolve((Path) event.context());
                        File file = filePath.toFile();

                        try {

                            // Add a new script
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                String name = file.getName();

                                if (!name.endsWith(".js")) {
                                    continue;
                                }

                                String source = Files.readString(file.toPath());

                                Script script = new Script(name, source, plugin);
                                script.execute();

                                this.scripts.put(name, script);

                                plugin.getLogger().info("Script added: " + name);

                                // Modify an existing script
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                String name = file.getName();

                                if (!name.endsWith(".js")) {
                                    continue;
                                }

                                // Close the current script
                                Script script = this.scripts.get(file.getName());

                                if (script != null) {
                                    script.close();
                                }

                                String source = Files.readString(file.toPath());

                                script = new Script(name, source, plugin);
                                script.execute();

                                this.scripts.put(name, script);

                                plugin.getLogger().info("Script modified: " + name);

                                // Remove a script
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                String name = file.getName();

                                if (!name.endsWith(".js")) {
                                    continue;
                                }

                                Script script = this.scripts.get(name);

                                if (script != null) {
                                    script.close();
                                }

                                this.scripts.remove(name);

                                plugin.getLogger().info("Script deleted: " + name);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Failed to handle event: " + kind.name());

                            e.printStackTrace();
                        }
                    }

                    key.reset(); // Reset the key to continue listening
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void close() {
        for (String name : this.scripts.keySet()) {
            this.scripts.get(name).close();
        }
    }
}
