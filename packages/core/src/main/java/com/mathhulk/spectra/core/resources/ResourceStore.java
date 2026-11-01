package com.mathhulk.spectra.core.resources;

import com.mathhulk.spectra.protocol.ResourceHash;
import com.mathhulk.spectra.protocol.ResourcePath;
import com.mathhulk.spectra.protocol.clientbound.ResourceManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Scans the resource directory, plus any directories plugins registered under a namespace, into a
 * {@link ResourceSnapshot}. Files are only re-read and re-hashed when their
 * size or modification time changed. Not thread-safe: call {@link #scan()} from one thread at a time.
 */
public final class ResourceStore {
  /** Guard against accidentally syncing huge directories to every player. */
  public static final long MAX_TOTAL_BYTES = 256L * 1024 * 1024;

  private record Cached(long size, long modified, ResourceHash hash, byte[] content) {
  }

  private final Path root;
  private final Map<String, Path> namespaces = new java.util.concurrent.ConcurrentHashMap<>();
  private final Logger logger;
  private final Map<String, Cached> cache = new HashMap<>();
  private ResourceSnapshot current = ResourceSnapshot.EMPTY;

  public ResourceStore(Path root, Logger logger) {
    this.root = root.toAbsolutePath().normalize();
    this.logger = logger;
  }

  public Path root() {
    return root;
  }

  /** Adds {@code directory}'s files under {@code namespace/}. Namespaces shadow same-named root folders. */
  public void addNamespace(String namespace, Path directory) {
    if (!namespace.matches("[a-z0-9_.\\-]{1,64}")) {
      throw new IllegalArgumentException("Namespaces must match [a-z0-9_.-]{1,64}: " + namespace);
    }
    if (namespaces.putIfAbsent(namespace, directory.toAbsolutePath().normalize()) != null) {
      throw new IllegalStateException("Resource namespace already registered: " + namespace);
    }
  }

  public void removeNamespace(String namespace, Path directory) {
    namespaces.remove(namespace, directory.toAbsolutePath().normalize());
  }

  public Map<String, Path> namespaces() {
    return Map.copyOf(namespaces);
  }

  public ResourceSnapshot current() {
    return current;
  }

  private void collect(Path directory, String prefix, Map<String, Path> files, java.util.Set<String> shadowed)
      throws IOException {
    List<Path> found;
    try (Stream<Path> stream = Files.walk(directory)) {
      found = stream.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList();
    }
    for (Path file : found) {
      String relative = directory.relativize(file.toAbsolutePath().normalize()).toString()
          .replace(file.getFileSystem().getSeparator(), "/");
      String path = prefix + relative;
      if (!ResourcePath.isValid(path)) {
        logger.warning("Skipping resource with an unsupported path: " + path);
        continue;
      }
      int slash = relative.indexOf('/');
      if (slash > 0 && shadowed.contains(relative.substring(0, slash))) {
        continue; // A plugin namespace owns this folder name.
      }
      files.put(path, file);
    }
  }

  private void unusedWarning(Map<String, Path> namespaces) {
    namespaces.forEach((name, dir) -> {
      if (!Files.isDirectory(dir)) {
        logger.warning("Resource namespace " + name + " points to a missing directory: " + dir);
      }
    });
  }

  /** Rescans the directory. Returns the same snapshot instance when nothing changed. */
  public ResourceSnapshot scan() throws IOException {
    Files.createDirectories(root);
    Map<String, Path> namespaces = Map.copyOf(this.namespaces);
    // Resource path -> file, sorted for stable manifests.
    java.util.TreeMap<String, Path> files = new java.util.TreeMap<>();
    collect(root, "", files, namespaces.keySet());
    for (Map.Entry<String, Path> namespace : namespaces.entrySet()) {
      if (Files.isDirectory(namespace.getValue())) {
        collect(namespace.getValue(), namespace.getKey() + "/", files, java.util.Set.of());
      }
    }

    Map<String, Cached> next = new HashMap<>();
    List<ResourceManifest.Entry> entries = new ArrayList<>();
    Map<ResourceHash, byte[]> content = new HashMap<>();
    long total = 0;

    for (Map.Entry<String, Path> resource : files.entrySet()) {
      String path = resource.getKey();
      Path file = resource.getValue();
      BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
      long modified = attributes.lastModifiedTime().toMillis();
      Cached cached = cache.get(path);
      if (cached == null || cached.size() != attributes.size() || cached.modified() != modified) {
        byte[] bytes = Files.readAllBytes(file);
        cached = new Cached(bytes.length, modified, ResourceHash.compute(bytes), bytes);
      }
      total += cached.size();
      if (total > MAX_TOTAL_BYTES) {
        throw new IOException("Resources exceed " + MAX_TOTAL_BYTES + " bytes; refusing to sync " + root);
      }
      next.put(path, cached);
      entries.add(new ResourceManifest.Entry(path, cached.hash(), cached.size()));
      content.put(cached.hash(), cached.content());
    }

    cache.clear();
    cache.putAll(next);
    unusedWarning(namespaces);

    if (Objects.equals(entries, current.entries())) {
      return current;
    }
    current = new ResourceSnapshot(current.generation() + 1, entries, content);
    return current;
  }
}
