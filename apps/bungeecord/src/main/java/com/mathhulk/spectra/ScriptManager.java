package com.mathhulk.spectra;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.md_5.bungee.api.ProxyServer;

public class ScriptManager {
  private final Spectra plugin;

  private final Map<String, Script> scripts = new HashMap<>();

  private static final List<String> ALLOWED_EXTENSIONS = List.of("js", "mjs");

  private static final File SCRIPTS_DIRECTORY = new File(ProxyServer.getInstance().getPluginsFolder().getParentFile(),
      "scripts");

  private Boolean enabled = false;
  private Boolean loaded = false;

  private WatchService watchService;
  private volatile Boolean watching = false;
  private Thread thread;

  public ScriptManager(Spectra plugin) {
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
      script.load();

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

  public Boolean load(Boolean watch) {
    if (loaded)
      return false;

    File[] files = SCRIPTS_DIRECTORY.listFiles((_, name) -> {
      String extension = name.substring(name.lastIndexOf(".") + 1);
      return ALLOWED_EXTENSIONS.contains(extension);
    });

    if (files != null) {
      for (File file : files) {
        addScript(file.getName(), false);
      }
    }

    if (watch)
      startWatching();

    loaded = true;

    return true;
  }

  public void stopWatching() {
    if (!watching)
      return;

    watching = false;

    // try {
    // watchService.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // }

    thread.interrupt();

    plugin.getLogger().info("Stopped watching for scripts");
  }

  public void startWatching() {
    if (watching)
      return;

    thread = new Thread(() -> {
      try {
        Path folderPath = SCRIPTS_DIRECTORY.toPath();

        watchService = FileSystems.getDefault().newWatchService();

        folderPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);

        plugin.getLogger().info("Started watching for scripts");

        watching = true;

        while (watching) {
          WatchKey key;

          try {
            key = watchService.take();
          } catch (InterruptedException e) {
            if (!watching)
              break;

            continue;
          }

          //
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
        e.printStackTrace();
      } finally {
        try {
          if (watchService != null) {
            watchService.close();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        watching = false;

        plugin.getLogger().info("Stopped watching for scripts");
      }
    });

    thread.start();
  }

  public Boolean enable() {
    if (enabled)
      return false;

    scripts.keySet().forEach((name) -> {
      scripts.get(name).enable();
    });

    enabled = true;

    return true;
  }

  public Boolean disable() {
    if (!enabled)
      return false;

    if (watching)
      stopWatching();

    scripts.keySet().forEach((name) -> {
      scripts.get(name).disable();
    });

    enabled = false;

    return true;
  }
}
