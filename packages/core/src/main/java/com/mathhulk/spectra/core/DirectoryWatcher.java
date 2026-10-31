package com.mathhulk.spectra.core;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Recursively watches a directory and reports batches of changed paths after a quiet period, so one editor save
 * (or one esbuild rebuild touching many files) produces one callback. The callback runs on the watcher thread;
 * callers must marshal to the main thread themselves.
 */
public final class DirectoryWatcher implements AutoCloseable {
  private final Path root;
  private final long debounceMillis;
  private final Consumer<Set<Path>> onChange;
  private final Logger logger;
  private final WatchService service;
  private final Map<WatchKey, Path> keys = new HashMap<>();
  private final Thread thread;
  private volatile boolean running = true;

  public DirectoryWatcher(Path root, long debounceMillis, Consumer<Set<Path>> onChange, Logger logger)
      throws IOException {
    this.root = root.toAbsolutePath().normalize();
    this.debounceMillis = debounceMillis;
    this.onChange = onChange;
    this.logger = logger;
    this.service = FileSystems.getDefault().newWatchService();
    registerTree(this.root);
    this.thread = Thread.ofPlatform().daemon().name("spectra-watch-" + this.root.getFileName()).unstarted(this::run);
    this.thread.start();
  }

  private void registerTree(Path directory) throws IOException {
    try (Stream<Path> paths = Files.walk(directory)) {
      for (Path path : (Iterable<Path>) paths.filter(Files::isDirectory)::iterator) {
        keys.put(path.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE), path);
      }
    }
  }

  private void run() {
    Set<Path> pending = new HashSet<>();
    try {
      while (running) {
        WatchKey key = pending.isEmpty()
            ? service.take()
            : service.poll(debounceMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (key == null) {
          Set<Path> batch = Set.copyOf(pending);
          pending.clear();
          try {
            onChange.accept(batch);
          } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "File change handler failed", e);
          }
          continue;
        }
        Path directory = keys.get(key);
        for (WatchEvent<?> event : key.pollEvents()) {
          if (event.kind() == StandardWatchEventKinds.OVERFLOW || directory == null) {
            pending.add(root);
            continue;
          }
          Path changed = directory.resolve((Path) event.context());
          pending.add(changed);
          if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
            try {
              registerTree(changed);
            } catch (IOException e) {
              logger.log(Level.WARNING, "Could not watch " + changed, e);
            }
          }
        }
        if (!key.reset()) {
          keys.remove(key);
        }
      }
    } catch (InterruptedException | ClosedWatchServiceException e) {
      // Closing.
    }
  }

  @Override
  public void close() {
    running = false;
    try {
      service.close();
    } catch (IOException e) {
      logger.log(Level.FINE, "Error closing watch service", e);
    }
    thread.interrupt();
  }
}
