package com.mathhulk.spectra.runtime.js;

import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.platform.MainThread;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Web-compatible timers: delays are milliseconds (rounded to the nearest 50 ms tick, minimum one tick), extra
 * arguments are passed to the callback, and every timer is released when the script unloads.
 */
final class Timers {
  private final MainThread mainThread;
  private final ScriptScope scope;
  private final Map<Integer, Cancellable> timers = new HashMap<>();
  private int nextId = 1;

  Timers(MainThread mainThread, ScriptScope scope) {
    this.mainThread = mainThread;
    this.scope = scope;
  }

  ProxyExecutable setTimeout() {
    return args -> schedule(args, false);
  }

  ProxyExecutable setInterval() {
    return args -> schedule(args, true);
  }

  ProxyExecutable clear() {
    return args -> {
      if (args.length > 0 && args[0].fitsInInt()) {
        Cancellable timer = timers.remove(args[0].asInt());
        if (timer != null) {
          timer.cancel();
        }
      }
      return null;
    };
  }

  private int schedule(Value[] args, boolean repeat) {
    if (args.length == 0 || !args[0].canExecute()) {
      throw new IllegalArgumentException((repeat ? "setInterval" : "setTimeout") + " expects a function");
    }
    Value callback = args[0];
    double millis = args.length > 1 && args[1].isNumber() ? args[1].asDouble() : 0;
    Object[] extra = args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new Object[0];
    long ticks = MainThread.toTicks(millis, 1);
    int id = nextId++;

    Runnable run = () -> scope.invoke(repeat ? "setInterval" : "setTimeout", () -> {
      if (!repeat) {
        timers.remove(id);
      }
      callback.executeVoid(extra);
    });
    Cancellable task = repeat ? mainThread.runRepeating(run, ticks, ticks) : mainThread.runLater(run, ticks);
    timers.put(id, scope.track(task));
    return id;
  }
}
