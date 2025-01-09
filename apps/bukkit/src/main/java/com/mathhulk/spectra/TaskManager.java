package com.mathhulk.spectra;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

public class TaskManager {
  private static final int TICKS_PER_SECOND = 20;

  private Long taskId = 0L;
  private final HashMap<Long, BukkitTask> tasks = new HashMap<>();

  private final Script script;

  TaskManager(Script script) {
    this.script = script;
  }

  public Script getScript() {
    return script;
  }

  @FunctionalInterface
  public interface SetIntervalFunction {
    long apply(Runnable runnable, long delay);
  }

  @FunctionalInterface
  public interface ClearIntervalFunction {
    void apply(long id);
  }

  @FunctionalInterface
  public interface SetTimeoutFunction {
    long apply(Runnable runnable, long delay);
  }

  @FunctionalInterface
  public interface ClearTimeoutFunction {
    void apply(long id);
  }

  public long setInterval(Runnable runnable, long delay) {
    long ticks = delay / 1000 * TICKS_PER_SECOND;

    JavaPlugin plugin = script.getPlugin();
    BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, ticks, ticks);
    tasks.put(taskId, task);

    return taskId++;
  }

  public void clearInterval(long id) {
    BukkitTask task = tasks.get(id);

    if (task == null) {
      return;
    }

    task.cancel();
  }

  public long setTimeout(Runnable runnable, long delay) {
    long ticks = delay / 1000 * TICKS_PER_SECOND;

    JavaPlugin plugin = script.getPlugin();
    BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, ticks);
    tasks.put(taskId, task);

    return taskId++;
  }

  public void clearTimeout(long id) {
    BukkitTask task = tasks.get(id);

    if (task == null) {
      return;
    }

    task.cancel();
  }

  public void removeTasks() {
    for (BukkitTask task : tasks.values()) {
      task.cancel();
    }
  }
}
