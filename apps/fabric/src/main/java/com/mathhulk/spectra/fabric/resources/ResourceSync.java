package com.mathhulk.spectra.fabric.resources;

import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ResourceHash;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.clientbound.ResourceChunk;
import com.mathhulk.spectra.protocol.clientbound.ResourceManifest;
import com.mathhulk.spectra.protocol.serverbound.ResourceRequest;
import com.mathhulk.spectra.protocol.serverbound.ResourcesSynced;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Client side of resource sync for one server connection. Packet handling runs on the client thread; verification
 * and disk writes run on {@code io}, with results posted back through {@code clientThread}.
 *
 * <p>{@link #resolve(String)} is read from browser threads, so the published path map is an immutable snapshot.
 */
public final class ResourceSync {
  /** Refuse manifests whose missing content would exceed this much memory while downloading. */
  private static final long MAX_PENDING_BYTES = 256L * 1024 * 1024;

  private static final class Download {
    final long size;
    byte[][] chunks;
    int received;

    Download(long size) {
      this.size = size;
    }
  }

  private final ObjectStore store;
  private final Executor io;
  private final Executor clientThread;
  private final Consumer<ServerboundPacket> send;
  private final Logger logger;

  private volatile Map<String, ResourceHash> published = Map.of();

  private long pendingGeneration = -1;
  private final Map<Integer, List<ResourceManifest.Entry>> parts = new HashMap<>();
  private int expectedParts;
  private Map<String, ResourceHash> pendingPaths;
  private final Map<ResourceHash, Download> downloads = new HashMap<>();
  private int writesInFlight;

  public ResourceSync(ObjectStore store, Executor io, Executor clientThread, Consumer<ServerboundPacket> send,
      Logger logger) {
    this.store = store;
    this.io = io;
    this.clientThread = clientThread;
    this.send = send;
    this.logger = logger;
  }

  /** The object file for a resource path of the current generation, if any. Safe from any thread. */
  public Optional<Path> resolve(String path) {
    ResourceHash hash = published.get(path);
    return hash == null ? Optional.empty() : Optional.of(store.path(hash));
  }

  public void handleManifest(ResourceManifest manifest) {
    if (manifest.generation() != pendingGeneration) {
      // A new generation supersedes anything in flight.
      pendingGeneration = manifest.generation();
      parts.clear();
      downloads.clear();
      pendingPaths = null;
      expectedParts = manifest.parts();
    }
    if (manifest.parts() != expectedParts || parts.containsKey(manifest.part())) {
      logger.warn("Ignoring inconsistent Spectra manifest part {}/{}", manifest.part(), manifest.parts());
      return;
    }
    parts.put(manifest.part(), manifest.entries());
    if (parts.size() == expectedParts) {
      startSync();
    }
  }

  private void startSync() {
    Map<String, ResourceHash> paths = new HashMap<>();
    Map<ResourceHash, Long> missing = new HashMap<>();
    long pendingBytes = 0;
    for (List<ResourceManifest.Entry> entries : parts.values()) {
      for (ResourceManifest.Entry entry : entries) {
        paths.put(entry.path(), entry.hash());
        if (!missing.containsKey(entry.hash()) && !store.contains(entry.hash())) {
          missing.put(entry.hash(), entry.size());
          pendingBytes += entry.size();
        }
      }
    }
    parts.clear();
    if (pendingBytes > MAX_PENDING_BYTES) {
      logger.error("Spectra server resources need {} bytes, over the {} byte limit; not syncing", pendingBytes,
          MAX_PENDING_BYTES);
      return;
    }
    pendingPaths = paths;
    missing.forEach((hash, size) -> downloads.put(hash, new Download(size)));

    List<ResourceHash> request = new ArrayList<>(missing.keySet());
    for (int from = 0; from < request.size(); from += Limits.MAX_REQUEST_HASHES) {
      List<ResourceHash> batch = request.subList(from, Math.min(request.size(), from + Limits.MAX_REQUEST_HASHES));
      send.accept(new ResourceRequest(pendingGeneration, List.copyOf(batch)));
    }
    maybeFinish();
  }

  public void handleChunk(ResourceChunk chunk) {
    Download download = downloads.get(chunk.hash());
    if (download == null) {
      return; // Unrequested or from an older generation.
    }
    if (download.chunks == null) {
      download.chunks = new byte[chunk.count()][];
    }
    if (chunk.count() != download.chunks.length || download.chunks[chunk.index()] != null) {
      logger.warn("Inconsistent chunk {}/{} for {}", chunk.index(), chunk.count(), chunk.hash());
      return;
    }
    download.chunks[chunk.index()] = chunk.data();
    download.received++;
    if (download.received < download.chunks.length) {
      return;
    }

    downloads.remove(chunk.hash());
    ByteArrayOutputStream content = new ByteArrayOutputStream((int) Math.min(download.size, Integer.MAX_VALUE));
    for (byte[] part : download.chunks) {
      content.writeBytes(part);
    }
    byte[] bytes = content.toByteArray();
    if (bytes.length != download.size) {
      logger.warn("Resource {} has {} bytes, manifest said {}", chunk.hash(), bytes.length, download.size);
      return;
    }
    long generation = pendingGeneration;
    writesInFlight++;
    io.execute(() -> {
      IOException failure = null;
      try {
        store.put(chunk.hash(), bytes);
      } catch (IOException e) {
        failure = e;
      }
      IOException error = failure;
      clientThread.execute(() -> {
        writesInFlight--;
        if (error != null) {
          logger.error("Could not store Spectra resource {}", chunk.hash(), error);
          return;
        }
        if (generation == pendingGeneration) {
          maybeFinish();
        }
      });
    });
  }

  private void maybeFinish() {
    if (pendingPaths == null || !downloads.isEmpty() || writesInFlight > 0) {
      return;
    }
    Set<ResourceHash> unavailable = new LinkedHashSet<>();
    pendingPaths.values().forEach(hash -> {
      if (!store.contains(hash)) {
        unavailable.add(hash);
      }
    });
    if (!unavailable.isEmpty()) {
      logger.error("Spectra resource sync incomplete; {} objects failed", unavailable.size());
      return;
    }
    published = Map.copyOf(pendingPaths);
    pendingPaths = null;
    send.accept(new ResourcesSynced(pendingGeneration));
  }
}
