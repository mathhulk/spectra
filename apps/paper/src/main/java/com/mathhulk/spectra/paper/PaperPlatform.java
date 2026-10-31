package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.platform.MainThread;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.protocol.Protocol;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/** Paper implementation of the core platform SPI. */
final class PaperPlatform implements Platform {
  private final JavaPlugin plugin;

  private final MainThread mainThread = new MainThread() {
    @Override
    public boolean isMainThread() {
      return Bukkit.isPrimaryThread();
    }

    @Override
    public void execute(Runnable task) {
      if (Bukkit.isPrimaryThread()) {
        task.run();
      } else {
        Bukkit.getScheduler().runTask(plugin, task);
      }
    }

    @Override
    public Cancellable runLater(Runnable task, long delayTicks) {
      BukkitTask scheduled = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
      return scheduled::cancel;
    }

    @Override
    public Cancellable runRepeating(Runnable task, long delayTicks, long periodTicks) {
      BukkitTask scheduled = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
      return scheduled::cancel;
    }
  };

  private final Executor async;

  PaperPlatform(JavaPlugin plugin) {
    this.plugin = plugin;
    this.async = task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
  }

  @Override
  public String name() {
    return "paper";
  }

  @Override
  public Logger logger() {
    return plugin.getLogger();
  }

  @Override
  public Path dataDirectory() {
    return plugin.getDataFolder().toPath();
  }

  @Override
  public MainThread mainThread() {
    return mainThread;
  }

  @Override
  public Executor asyncExecutor() {
    return async;
  }

  @Override
  public void sendFrame(UUID player, byte[] frame) {
    Player online = Bukkit.getPlayer(player);
    if (online != null) {
      online.sendPluginMessage(plugin, Protocol.CHANNEL, frame);
    }
  }

  @Override
  public void kick(UUID player, String reason) {
    Player online = Bukkit.getPlayer(player);
    if (online != null) {
      online.kick(Component.text(reason));
    }
  }

  @Override
  public boolean isOnline(UUID player) {
    return Bukkit.getPlayer(player) != null;
  }
}
