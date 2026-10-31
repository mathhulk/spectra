package com.mathhulk.spectra.core;

import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.platform.MainThread;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.Packets;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/** Deterministic platform: the "main thread" is whoever calls {@link #tick()}. */
final class FakePlatform implements Platform {
  private record Task(Runnable runnable, long[] due, long period, boolean[] cancelled) {
  }

  final Path dataDirectory;
  final Map<UUID, List<ClientboundPacket>> sent = new HashMap<>();
  final Set<UUID> kicked = new HashSet<>();
  private final List<Task> tasks = new ArrayList<>();
  private long now;

  FakePlatform(Path dataDirectory) {
    this.dataDirectory = dataDirectory;
  }

  private final MainThread mainThread = new MainThread() {
    @Override
    public boolean isMainThread() {
      return true;
    }

    @Override
    public void execute(Runnable task) {
      task.run();
    }

    @Override
    public Cancellable runLater(Runnable task, long delayTicks) {
      return schedule(task, delayTicks, 0);
    }

    @Override
    public Cancellable runRepeating(Runnable task, long delayTicks, long periodTicks) {
      return schedule(task, delayTicks, periodTicks);
    }
  };

  private Cancellable schedule(Runnable runnable, long delay, long period) {
    Task task = new Task(runnable, new long[] {now + Math.max(1, delay)}, period, new boolean[1]);
    tasks.add(task);
    return () -> task.cancelled()[0] = true;
  }

  void tick() {
    now++;
    for (Task task : List.copyOf(tasks)) {
      if (task.cancelled()[0] || task.due()[0] > now) {
        continue;
      }
      task.runnable().run();
      if (task.period() > 0) {
        task.due()[0] = now + task.period();
      } else {
        task.cancelled()[0] = true;
      }
    }
    tasks.removeIf(task -> task.cancelled()[0]);
  }

  void ticks(int count) {
    for (int i = 0; i < count; i++) {
      tick();
    }
  }

  @SuppressWarnings("unchecked")
  <T extends ClientboundPacket> List<T> drain(UUID player, Class<T> type) {
    List<T> matches = new ArrayList<>();
    Iterator<ClientboundPacket> iterator = sent.getOrDefault(player, new ArrayList<>()).iterator();
    while (iterator.hasNext()) {
      ClientboundPacket packet = iterator.next();
      if (type.isInstance(packet)) {
        matches.add((T) packet);
        iterator.remove();
      }
    }
    return matches;
  }

  @Override
  public String name() {
    return "fake";
  }

  @Override
  public Logger logger() {
    return Logger.getLogger("spectra-test");
  }

  @Override
  public Path dataDirectory() {
    return dataDirectory;
  }

  @Override
  public MainThread mainThread() {
    return mainThread;
  }

  @Override
  public Executor asyncExecutor() {
    return Runnable::run;
  }

  @Override
  public void sendFrame(UUID player, byte[] frame) {
    // Round-trip through the codec so tests exercise exactly what goes on the wire.
    sent.computeIfAbsent(player, id -> new ArrayList<>()).add(Packets.decodeClientbound(frame));
  }

  @Override
  public void kick(UUID player, String reason) {
    kicked.add(player);
  }

  @Override
  public boolean isOnline(UUID player) {
    return !kicked.contains(player);
  }
}
