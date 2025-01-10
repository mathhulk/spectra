package com.mathhulk.spectra;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.scheduler.ScheduledTask;

public class TaskManager {
  private final HashMap<Integer, ScheduledTask> tasks = new HashMap<>();

  private final Script script;

  TaskManager(Script script) {
    this.script = script;
  }

  public Script getScript() {
    return script;
  }

  @FunctionalInterface
  public interface SetIntervalFunction {
    Integer apply(Runnable task, long delay);
  }

  @FunctionalInterface
  public interface ClearIntervalFunction {
    void apply(Integer id);
  }

  @FunctionalInterface
  public interface SetTimeoutFunction {
    Integer apply(Runnable task, long delay);
  }

  @FunctionalInterface
  public interface ClearTimeoutFunction {
    void apply(Integer id);
  }

  public Integer setInterval(Runnable task, long delay) {
    ScheduledTask scheduledTask = script.getPlugin().getProxy().getScheduler().schedule(script.getPlugin(), task, delay,
        delay, TimeUnit.MILLISECONDS);

    Integer id = scheduledTask.getId();
    tasks.put(id, scheduledTask);

    return id;
  }

  public void clearInterval(Integer id) {
    ScheduledTask task = tasks.get(id);

    if (task == null) {
      return;
    }

    script.getPlugin().getProxy().getScheduler().cancel(task);
  }

  public Integer setTimeout(Runnable task, long delay) {
    ScheduledTask scheduledTask = script.getPlugin().getProxy().getScheduler().schedule(script.getPlugin(), task, delay,
        TimeUnit.MILLISECONDS);

    Integer id = scheduledTask.getId();
    tasks.put(id, scheduledTask);

    return id;
  }

  public void clearTimeout(Integer id) {
    clearInterval(id);
  }

  public void removeTasks() {
    for (ScheduledTask task : tasks.values()) {
      script.getPlugin().getProxy().getScheduler().cancel(task);
    }
  }
}
