package com.mathhulk.spectra.core.scripts;

import com.mathhulk.spectra.core.DirectoryWatcher;
import com.mathhulk.spectra.core.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Loads every script in a directory, keeps them in name order, and hot-reloads them when files change. All state
 * changes happen on the main thread; the file watcher only schedules work there.
 */
public final class ScriptManager implements AutoCloseable {
  /** Snapshot of a loaded script, for commands and status output. */
  public record ScriptStatus(String name, String language, boolean enabled) {
  }

  private static final class Loaded {
    final String name;
    final ScriptLanguage language;
    final ScriptInstance instance;
    final ScriptScope scope;
    boolean enabled;

    Loaded(String name, ScriptLanguage language, ScriptInstance instance, ScriptScope scope) {
      this.name = name;
      this.language = language;
      this.instance = instance;
      this.scope = scope;
    }
  }

  private final Platform platform;
  private final Path directory;
  private final List<ScriptLanguage> languages;
  private final ScriptGlobals globals;
  private final Map<String, Loaded> scripts = new TreeMap<>();
  private DirectoryWatcher watcher;
  private boolean enabled;

  public ScriptManager(Platform platform, Path directory, Collection<ScriptLanguage> languages,
      ScriptGlobals globals) {
    this.platform = platform;
    this.directory = directory.toAbsolutePath().normalize();
    this.languages = List.copyOf(languages);
    this.globals = globals;
  }

  public Path directory() {
    return directory;
  }

  private void requireMainThread() {
    if (!platform.mainThread().isMainThread()) {
      throw new IllegalStateException("Scripts may only be managed on the main thread");
    }
  }

  private Optional<ScriptLanguage> languageFor(Path file) {
    String name = file.getFileName().toString();
    int dot = name.lastIndexOf('.');
    if (dot <= 0) {
      return Optional.empty();
    }
    String extension = name.substring(dot + 1);
    return languages.stream().filter(language -> language.extensions().contains(extension)).findFirst();
  }

  /** Scripts are the top-level files of the directory; subdirectories hold their own modules and assets. */
  private List<Path> scriptFiles() throws IOException {
    Files.createDirectories(directory);
    try (Stream<Path> files = Files.list(directory)) {
      return files.filter(Files::isRegularFile).filter(file -> languageFor(file).isPresent()).sorted().toList();
    }
  }

  /** Loads every script. Call once on startup; scripts are enabled by {@link #enableAll()}. */
  public void loadAll() {
    requireMainThread();
    try {
      for (Path file : scriptFiles()) {
        load(file);
      }
    } catch (IOException e) {
      platform.logger().log(Level.SEVERE, "Could not list scripts in " + directory, e);
    }
  }

  public void enableAll() {
    requireMainThread();
    enabled = true;
    List.copyOf(scripts.values()).forEach(this::enable);
  }

  public void disableAll() {
    requireMainThread();
    enabled = false;
    List<Loaded> reversed = new ArrayList<>(scripts.values());
    java.util.Collections.reverse(reversed);
    reversed.forEach(this::disable);
  }

  /** Starts hot reload. Changes are debounced so one save or one rebuild produces one reload. */
  public void watch() throws IOException {
    if (watcher != null) {
      return;
    }
    Files.createDirectories(directory);
    watcher = new DirectoryWatcher(directory, 150,
        changed -> platform.mainThread().execute(() -> handleChanges(changed)), platform.logger());
  }

  public boolean isWatching() {
    return watcher != null;
  }

  public void stopWatching() {
    if (watcher != null) {
      watcher.close();
      watcher = null;
    }
  }

  private void handleChanges(Set<Path> changed) {
    boolean nested = changed.stream().anyMatch(path -> !directory.equals(path.toAbsolutePath().getParent()));
    if (nested || changed.contains(directory)) {
      // A shared module or asset changed; any script may depend on it.
      reloadAll();
      return;
    }
    for (Path path : changed) {
      if (languageFor(path).isEmpty()) {
        continue;
      }
      String name = path.getFileName().toString();
      if (Files.isRegularFile(path)) {
        reload(name);
      } else {
        unload(name);
      }
    }
  }

  public void reloadAll() {
    requireMainThread();
    List.copyOf(scripts.keySet()).forEach(this::unload);
    loadAll();
    if (enabled) {
      List.copyOf(scripts.values()).forEach(this::enable);
    }
  }

  /** Reloads (or newly loads) one script by file name and enables it if scripts are enabled. */
  public boolean reload(String name) {
    requireMainThread();
    unload(name);
    Path file = directory.resolve(name).normalize();
    if (!file.getParent().equals(directory) || !Files.isRegularFile(file)) {
      return false;
    }
    Loaded loaded = load(file);
    if (loaded != null && enabled) {
      enable(loaded);
    }
    return loaded != null;
  }

  public boolean unload(String name) {
    requireMainThread();
    Loaded loaded = scripts.remove(name);
    if (loaded == null) {
      return false;
    }
    disable(loaded);
    loaded.scope.dispose();
    loaded.instance.close();
    platform.logger().info("Unloaded script " + name);
    return true;
  }

  private Loaded load(Path file) {
    String name = file.getFileName().toString();
    ScriptLanguage language = languageFor(file).orElseThrow();
    ScriptScope scope = new ScriptScope(name, platform.logger());
    ScriptInstance instance = null;
    try {
      instance = language.create(name, file, scope, globals);
      instance.load();
      Loaded loaded = new Loaded(name, language, instance, scope);
      scripts.put(name, loaded);
      platform.logger().info("Loaded script " + name + " (" + language.name() + ")");
      return loaded;
    } catch (Exception e) {
      platform.logger().log(Level.SEVERE, "Failed to load script " + name + ": " + describe(e), e);
      scope.dispose();
      if (instance != null) {
        instance.close();
      }
      return null;
    }
  }

  private void enable(Loaded loaded) {
    if (loaded.enabled) {
      return;
    }
    try {
      loaded.instance.enable();
      loaded.enabled = true;
    } catch (Exception e) {
      platform.logger().log(Level.SEVERE, "Failed to enable script " + loaded.name + ": " + describe(e), e);
    }
  }

  private void disable(Loaded loaded) {
    if (!loaded.enabled) {
      return;
    }
    loaded.enabled = false;
    try {
      loaded.instance.disable();
    } catch (Exception e) {
      platform.logger().log(Level.SEVERE, "Failed to disable script " + loaded.name + ": " + describe(e), e);
    }
  }

  private static String describe(Throwable error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
  }

  /** A script's export together with the scope that guards calls into it. */
  public record Export(String script, ScriptScope scope, Object value) {
  }

  /** Every loaded script's {@code name} export, in script order. Main thread only. */
  public List<Export> exports(String name) {
    List<Export> exports = new ArrayList<>();
    for (Loaded loaded : scripts.values()) {
      Object value = loaded.scope.invoke("export " + name, () -> loaded.instance.export(name), null);
      if (value != null) {
        exports.add(new Export(loaded.name, loaded.scope, value));
      }
    }
    return exports;
  }

  public List<ScriptStatus> list() {
    return scripts.values().stream()
        .map(loaded -> new ScriptStatus(loaded.name, loaded.language.name(), loaded.enabled)).toList();
  }

  @Override
  public void close() {
    stopWatching();
    disableAll();
    List.copyOf(scripts.keySet()).forEach(this::unload);
  }
}
