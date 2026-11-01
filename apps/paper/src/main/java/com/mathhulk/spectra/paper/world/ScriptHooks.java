package com.mathhulk.spectra.paper.world;

import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.graalvm.polyglot.Value;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calls an optional member of a script object under the script's lock. A missing member, a script that has been
 * unloaded, or an error all yield {@code null}, so callers fall back to Paper's default behavior. Errors are logged
 * (once per member) instead of silently swallowed.
 */
final class ScriptHooks {
  /**
   * The server thread and the task queues it drains while blocked on a chunk load: the loading world's chunk
   * executor (by world id), or the server's queue when the world is unknown.
   */
  record ServerThread(Thread thread,
      java.util.function.Supplier<java.util.Collection<java.util.concurrent.Executor>> worlds,
      java.util.concurrent.Executor fallback) {
    /**
     * Runs {@code task} once on the server thread through whichever queue it drains first: each loaded world's
     * chunk executor (drained while it waits for chunks of that world) and the server's own queue.
     */
    <T> T runOnServerThread(java.util.function.Supplier<T> task) {
      java.util.concurrent.CompletableFuture<T> result = new java.util.concurrent.CompletableFuture<>();
      java.util.concurrent.atomic.AtomicBoolean claimed = new java.util.concurrent.atomic.AtomicBoolean();
      Runnable once = () -> {
        if (claimed.compareAndSet(false, true)) {
          try {
            result.complete(task.get());
          } catch (Throwable error) {
            result.completeExceptionally(error);
          }
        }
      };
      for (java.util.concurrent.Executor executor : worlds.get()) {
        executor.execute(once);
      }
      fallback.execute(once);
      return result.join();
    }
  }

  private static volatile ServerThread server;

  private final ScriptScope scope;
  private final Value target;
  private final Logger logger;
  private final java.util.Set<String> reported = java.util.concurrent.ConcurrentHashMap.newKeySet();

  ScriptHooks(ScriptScope scope, Value target, Logger logger) {
    this.scope = scope;
    this.target = target;
    this.logger = logger;
  }

  static void serverThread(ServerThread thread) {
    server = thread;
  }

  /** Records {@code thread} as the server thread when it differs from what we assumed (cheap; call freely). */
  static void observeServerThread(Thread thread) {
    ServerThread current = server;
    if (current != null && current.thread() != thread) {
      server = new ServerThread(thread, current.worlds(), current.fallback());
    }
  }

  /**
   * Runs {@code call} under the script's lock without deadlocking: if the server thread holds the lock (for
   * example a script reading a block, which synchronously generates a chunk with a generator from the same
   * script), the call is handed to the server thread, which runs queued tasks while it waits for the chunk.
   */
  private <T> T locked(java.util.function.Supplier<T> call) {
    ServerThread main = server;
    if (main == null || Thread.currentThread() == main.thread()) {
      return scope.locked(call);
    }
    try {
      while (true) {
        // The server thread is inside this script and waiting for our chunk: run the call on it instead.
        if (scope.isHeldBy(server.thread())) {
          return server.runOnServerThread(() -> scope.locked(call));
        }
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        if (scope.tryLocked(() -> {
          result.set(call.get());
          return Boolean.TRUE;
        }, 2).isPresent()) {
          return result.get();
        }
        if (scope.isDisposed()) {
          return null;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (java.util.concurrent.CompletionException e) {
      throw e.getCause() instanceof RuntimeException runtime ? runtime : e;
    }
  }

  /** True when the value is a host (Java) object, checked under the lock. */
  boolean isHost(Value value) {
    return Boolean.TRUE.equals(locked(value::isHostObject));
  }

  ScriptScope scope() {
    return scope;
  }

  boolean has(String member) {
    try {
      return Boolean.TRUE.equals(locked(() -> target.hasMember(member) && !target.getMember(member).isNull()));
    } catch (RuntimeException e) {
      return false;
    }
  }

  /** Calls {@code member(args)} if it is a function, or returns its value if it is not. */
  Value call(String member, Object... args) {
    if (scope.isDisposed()) {
      warn(member, null);
      return null;
    }
    try {
      return locked(() -> {
        if (!target.hasMember(member)) {
          return null;
        }
        Value value = target.getMember(member);
        if (value == null || value.isNull()) {
          return null;
        }
        return value.canExecute() ? target.invokeMember(member, args) : value;
      });
    } catch (RuntimeException e) {
      warn(member, e);
      return null;
    }
  }

  /** Calls the target itself when it is a function, otherwise its {@code member}. */
  Value callSelfOr(String member, Object... args) {
    boolean function = Boolean.TRUE.equals(locked(target::canExecute));
    if (!function) {
      return call(member, args);
    }
    try {
      return locked(() -> target.execute(args));
    } catch (RuntimeException e) {
      warn(member, e);
      return null;
    }
  }

  /** Runs {@code work} under the script's lock, deadlock-safe; used for reading arrays returned by scripts. */
  <T> T read(java.util.function.Supplier<T> work) {
    return locked(work);
  }

  /** Converts a script value; every Value operation happens under the (deadlock-safe) lock. */
  <T> T as(Value value, Class<T> type) {
    if (value == null) {
      return null;
    }
    try {
      return locked(() -> value.isNull() ? null : value.isHostObject() && type.isInstance(value.asHostObject())
          ? type.cast(value.asHostObject()) : value.as(type));
    } catch (RuntimeException e) {
      warn("conversion to " + type.getSimpleName(), e);
      return null;
    }
  }

  private void warn(String member, Exception error) {
    if (!reported.add(member)) {
      return;
    }
    if (error == null) {
      logger.warning("World hook '" + member + "' belongs to an unloaded script (" + scope.scriptName()
          + "); restart the server to use the reloaded version");
    } else {
      logger.log(Level.SEVERE, "World hook '" + member + "' in " + scope.scriptName() + " failed: "
          + error.getMessage(), error);
    }
  }
}
