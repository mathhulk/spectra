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
 * Scans a resource directory into a {@link ResourceSnapshot}. Files are only re-read and re-hashed when their
 * size or modification time changed. Not thread-safe: call {@link #scan()} from one thread at a time.
 */
public final class ResourceStore {
  /** Guard against accidentally syncing huge directories to every player. */
  public static final long MAX_TOTAL_BYTES = 256L * 1024 * 1024;

  private record Cached(long size, long modified, ResourceHash hash, byte[] content) {
  }

  private final Path root;
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

  public ResourceSnapshot current() {
    return current;
  }

  /** Rescans the directory. Returns the same snapshot instance when nothing changed. */
  public ResourceSnapshot scan() throws IOException {
    Files.createDirectories(root);
    List<Path> files;
    try (Stream<Path> stream = Files.walk(root)) {
      files = stream.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList();
    }

    Map<String, Cached> next = new HashMap<>();
    List<ResourceManifest.Entry> entries = new ArrayList<>();
    Map<ResourceHash, byte[]> content = new HashMap<>();
    long total = 0;

    for (Path file : files) {
      String path = root.relativize(file.toAbsolutePath().normalize()).toString().replace(file.getFileSystem()
          .getSeparator(), "/");
      if (!ResourcePath.isValid(path)) {
        logger.warning("Skipping resource with an unsupported path: " + path);
        continue;
      }
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

    if (Objects.equals(entries, current.entries())) {
      return current;
    }
    current = new ResourceSnapshot(current.generation() + 1, entries, content);
    return current;
  }
}
