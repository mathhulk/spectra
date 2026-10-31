package com.mathhulk.spectra.core.resources;

import com.mathhulk.spectra.core.Listeners;
import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.ResourceHash;
import com.mathhulk.spectra.protocol.clientbound.ResourceChunk;
import com.mathhulk.spectra.protocol.clientbound.ResourceManifest;
import com.mathhulk.spectra.protocol.serverbound.ResourceRequest;
import com.mathhulk.spectra.protocol.serverbound.ResourcesSynced;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sends manifests and resource content to supported players. Main thread only: uploads are paced by
 * {@link #tick()} so no single tick sends more than the per-player byte budget, and no file IO happens here.
 */
public final class ResourceService {
  /** Chunk size used for uploads; well under the clientbound frame limit. */
  public static final int CHUNK_SIZE = 128 * 1024;
  /** Leaves room for the manifest packet header inside the clientbound frame. */
  private static final int MANIFEST_PART_BUDGET = Limits.MAX_CLIENTBOUND_FRAME - 4096;

  private static final class Upload {
    final ResourceHash hash;
    final byte[] content;
    final int count;
    int next;

    Upload(ResourceHash hash, byte[] content) {
      this.hash = hash;
      this.content = content;
      this.count = Math.max(1, (content.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
    }
  }

  private static final class PlayerState {
    long syncedGeneration = -1;
    final Deque<Upload> queue = new ArrayDeque<>();
    final Set<ResourceHash> requested = new HashSet<>();
  }

  /** A player finished syncing a generation. */
  public record Synced(UUID player, long generation) {
  }

  private final Platform platform;
  private final int bytesPerTick;
  private final Map<UUID, PlayerState> players = new HashMap<>();
  private final Listeners<Synced> synced;
  private ResourceSnapshot snapshot = ResourceSnapshot.EMPTY;

  public ResourceService(Platform platform, int bytesPerTick) {
    this.platform = platform;
    this.bytesPerTick = bytesPerTick;
    this.synced = new Listeners<>(platform.logger(), "resources synced");
  }

  public Cancellable onSynced(Consumer<? super Synced> listener) {
    return synced.add(listener);
  }

  public ResourceSnapshot snapshot() {
    return snapshot;
  }

  /** Publishes a new snapshot and pushes its manifest to every tracked player. */
  public void publish(ResourceSnapshot next) {
    if (next == snapshot) {
      return;
    }
    snapshot = next;
    for (Map.Entry<UUID, PlayerState> entry : players.entrySet()) {
      entry.getValue().queue.clear();
      entry.getValue().requested.clear();
      sendManifest(entry.getKey());
    }
  }

  /** Starts syncing a player whose client supports resources. */
  public void track(UUID player) {
    players.put(player, new PlayerState());
    sendManifest(player);
  }

  public void untrack(UUID player) {
    players.remove(player);
  }

  public boolean isSynced(UUID player) {
    PlayerState state = players.get(player);
    return state != null && state.syncedGeneration == snapshot.generation();
  }

  private void sendManifest(UUID player) {
    List<List<ResourceManifest.Entry>> parts = new ArrayList<>();
    List<ResourceManifest.Entry> part = new ArrayList<>();
    int partSize = 0;
    for (ResourceManifest.Entry entry : snapshot.entries()) {
      int size = entry.encodedSizeBound();
      if (!part.isEmpty() && (partSize + size > MANIFEST_PART_BUDGET || part.size() == Limits.MAX_MANIFEST_ENTRIES)) {
        parts.add(part);
        part = new ArrayList<>();
        partSize = 0;
      }
      part.add(entry);
      partSize += size;
    }
    parts.add(part);
    for (int i = 0; i < parts.size(); i++) {
      platform.sendFrame(player,
          Packets.encode(new ResourceManifest(snapshot.generation(), i, parts.size(), parts.get(i))));
    }
  }

  public void handleRequest(UUID player, ResourceRequest request) {
    PlayerState state = players.get(player);
    if (state == null || request.generation() != snapshot.generation()) {
      return; // Stale request: the client already has (or will get) a newer manifest.
    }
    for (ResourceHash hash : request.hashes()) {
      if (!state.requested.add(hash)) {
        continue; // Each hash is uploaded at most once per generation, however often it is requested.
      }
      snapshot.content(hash).ifPresent(content -> state.queue.add(new Upload(hash, content)));
    }
  }

  public void handleSynced(UUID player, ResourcesSynced packet) {
    PlayerState state = players.get(player);
    if (state == null || packet.generation() != snapshot.generation()) {
      return;
    }
    state.syncedGeneration = packet.generation();
    synced.fire(new Synced(player, packet.generation()));
  }

  /** Sends up to the per-player budget of queued chunks. Call once per tick. */
  public void tick() {
    for (Map.Entry<UUID, PlayerState> entry : players.entrySet()) {
      Deque<Upload> queue = entry.getValue().queue;
      int budget = bytesPerTick;
      while (budget > 0 && !queue.isEmpty()) {
        Upload upload = queue.peek();
        int from = upload.next * CHUNK_SIZE;
        int to = Math.min(upload.content.length, from + CHUNK_SIZE);
        byte[] data = java.util.Arrays.copyOfRange(upload.content, from, to);
        platform.sendFrame(entry.getKey(),
            Packets.encode(new ResourceChunk(upload.hash, upload.next, upload.count, data)));
        budget -= Math.max(1, data.length);
        if (++upload.next == upload.count) {
          queue.poll();
        }
      }
    }
  }
}
