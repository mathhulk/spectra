package com.mathhulk.spectra.core.resources;

import com.mathhulk.spectra.protocol.ResourceHash;
import com.mathhulk.spectra.protocol.clientbound.ResourceManifest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** An immutable view of every resource at one {@code generation}, with content held in memory. */
public record ResourceSnapshot(long generation, List<ResourceManifest.Entry> entries,
    Map<ResourceHash, byte[]> content) {
  public static final ResourceSnapshot EMPTY = new ResourceSnapshot(0, List.of(), Map.of());

  public ResourceSnapshot {
    entries = List.copyOf(entries);
    content = Map.copyOf(content);
  }

  public Optional<byte[]> content(ResourceHash hash) {
    return Optional.ofNullable(content.get(hash));
  }

  public long totalBytes() {
    return content.values().stream().mapToLong(bytes -> bytes.length).sum();
  }
}
