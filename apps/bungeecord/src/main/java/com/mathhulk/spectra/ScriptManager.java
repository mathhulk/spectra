package com.mathhulk.spectra;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.md_5.bungee.api.ProxyServer;

public class ScriptManager {
  private final SpectraBungeeCordPlugin plugin;

  private final Map<String, Script> scripts = new HashMap<>();

  private static final List<String> ALLOWED_EXTENSIONS = List.of("js", "mjs");

  private static final String SCRIPTS_DIRECTORY = new File(ProxyServer.getInstance().getPluginsFolder().getParentFile(),
      "scripts").getAbsolutePath();

  private Boolean enabled = false;
  private Boolean watching = false;
  private Thread thread;

  public ScriptManager(SpectraBungeeCordPlugin plugin) {
    this.plugin = plugin;
  }

  public Script addScript(String fileName, Boolean enable) {
    Script existingScript = scripts.get(fileName);
    if (existingScript != null)
      return null;

    File file = new File(SCRIPTS_DIRECTORY, fileName);

    if (!file.exists()) {
      plugin.getLogger().info("Script does not exist: " + file);

      return null;
    }

    String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      plugin.getLogger().info("Invalid script extension: " + fileName);

      return null;
    }

    try {
      Script script = new Script(file, plugin);
      if (enable)
        script.enable();

      scripts.put(fileName, script);

      plugin.getLogger().info("Added script: " + fileName);

      return script;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to add script: " + fileName);

      e.printStackTrace();

      return null;
    }
  }

  public Boolean isWatching() {
    return watching;
  }

  public Boolean isEnabled() {
    return enabled;
  }

  public Boolean enableScript(String name) {
    Script script = scripts.get(name);
    if (script == null)
      return false;

    return script.enable();
  }

  public Boolean disableScript(String name) {
    Script script = scripts.get(name);
    if (script == null)
      return false;

    return script.disable();
  }

  public Boolean removeScript(String name) {
    Script script = scripts.get(name);
    if (script == null)
      return false;

    script.disable();
    scripts.remove(name);

    return true;
  }

  public Script getScript(String name) {
    return scripts.get(name);
  }

  public ArrayList<Script> getScripts() {
    return new ArrayList<>(scripts.values());
  }

  public Boolean enable(Boolean watch) {
    if (enabled)
      return false;

    File scriptsFolder = new File(SCRIPTS_DIRECTORY);

    File[] files = scriptsFolder.listFiles((_, name) -> {
      String extension = name.substring(name.lastIndexOf(".") + 1);
      return ALLOWED_EXTENSIONS.contains(extension);
    });

    if (files != null) {
      for (File file : files) {
        addScript(file.getName(), true);
      }
    }

    if (!watch)
      return true;

    startWatching();

    return true;
  }

  public void stopWatching() {
    if (!watching)
      return;

    watching = false;

    thread.interrupt();

    plugin.getLogger().info("Stopped watching for scripts");
  }

  public void startWatching() {
    if (watching)
      return;

    thread = new Thread(() -> {
      try {
        watching = true;

        File scriptsFolder = new File(SCRIPTS_DIRECTORY);
        Path folderPath = scriptsFolder.toPath();

        WatchService watchService = FileSystems.getDefault().newWatchService();
        folderPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);

        plugin.getLogger().info("Started watching for scripts");

        while (watching) {
          WatchKey key = watchService.take();

          for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            Path filePath = folderPath.resolve((Path) event.context());
            File file = filePath.toFile();

            String name = file.getName();
            String extension = name.substring(name.lastIndexOf(".") + 1);

            if (!ALLOWED_EXTENSIONS.contains(extension)) {
              continue;
            }

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

        e.printStackTrace();

        watching = false;
      }
    });

    thread.start();
  }

  public Boolean disable() {
    if (!enabled)
      return false;

    if (watching)
      stopWatching();

    scripts.keySet().forEach((name) -> {
      scripts.get(name).disable();
      scripts.remove(name);
    });

    enabled = false;

    return true;
  }
}
