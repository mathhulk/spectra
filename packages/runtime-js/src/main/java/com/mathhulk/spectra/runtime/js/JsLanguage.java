package com.mathhulk.spectra.runtime.js;

import com.mathhulk.spectra.core.platform.MainThread;
import com.mathhulk.spectra.core.scripts.ScriptGlobals;
import com.mathhulk.spectra.core.scripts.ScriptInstance;
import com.mathhulk.spectra.core.scripts.ScriptLanguage;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import com.mathhulk.spectra.api.Spectra;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * JavaScript (and bundled TypeScript) via GraalJS. One {@link Engine} is shared by every script so parsed code is
 * cached across scripts and hot reloads; each script gets its own context.
 *
 * <p>Every context starts with Spectra's web-platform layer ({@code fetch}, {@code URL}, {@code TextEncoder},
 * {@code EventTarget}, {@code WebSocket}, {@code crypto}, {@code process}, ...) and resolves Node builtins
 * ({@code node:path}, {@code fs}, {@code events}, ...) to Spectra's shims, so code written for Node, Bun, or Deno
 * runs with few or no changes.
 *
 * <p>GraalJS optimizes code only when the JVM provides the Graal compiler (GraalVM, or a JVMCI-enabled JDK).
 * Otherwise it runs in interpreter mode: fully functional, but slower. {@link #compilerAvailable()} reports which.
 */
public final class JsLanguage implements ScriptLanguage {
  private final Engine engine;
  private final MainThread mainThread;
  private final Logger logger;
  private final ClassLoader hostClassLoader;
  private final Path workingDirectory;
  private final Spectra spectra;
  private final Function<UUID, Object> players;
  private final Source prelude;
  private final Map<String, Path> builtins;

  /** Builtin module name -> shim resource. */
  private static final Map<String, String> SHIMS = Map.ofEntries(
      Map.entry("path", "path.mjs"), Map.entry("path/posix", "path.mjs"), Map.entry("events", "events.mjs"),
      Map.entry("util", "util.mjs"), Map.entry("os", "os.mjs"), Map.entry("buffer", "buffer.mjs"),
      Map.entry("fs", "fs.mjs"), Map.entry("fs/promises", "fs-promises.mjs"), Map.entry("crypto", "crypto.mjs"),
      Map.entry("timers", "timers.mjs"), Map.entry("timers/promises", "timers-promises.mjs"),
      Map.entry("process", "process.mjs"), Map.entry("url", "url.mjs"), Map.entry("assert", "assert.mjs"),
      Map.entry("assert/strict", "assert.mjs"));

  /** A runtime without view requests (used by tests and platforms without the Spectra API). */
  public JsLanguage(MainThread mainThread, Logger logger, ClassLoader hostClassLoader) {
    this(mainThread, logger, hostClassLoader, Path.of("").toAbsolutePath(), null, uuid -> null);
  }

  /**
   * @param workingDirectory {@code process.cwd()} and the base for relative {@code fs} paths
   * @param spectra          backs the {@code fetch}/{@code websocket} events for view requests; may be null
   * @param players          maps a player id to the platform's player object for those events
   */
  public JsLanguage(MainThread mainThread, Logger logger, ClassLoader hostClassLoader, Path workingDirectory,
      Spectra spectra, Function<UUID, Object> players) {
    this.mainThread = mainThread;
    this.logger = logger;
    this.hostClassLoader = hostClassLoader;
    this.workingDirectory = workingDirectory;
    this.spectra = spectra;
    this.players = players;
    this.prelude = Source.newBuilder("js", resource("prelude.js"), "spectra:prelude.js").cached(true).buildLiteral();
    this.builtins = extractShims();
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(JsLanguage.class.getClassLoader());
    try {
      // We report the execution mode ourselves (see compilerAvailable), so silence Truffle's generic warning.
      this.engine = Engine.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build();
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }

  /** True when GraalJS can JIT-compile scripts; false in interpreter-only mode. */
  public boolean compilerAvailable() {
    return !"Interpreted".equalsIgnoreCase(engine.getImplementationName());
  }

  public String engineDescription() {
    return "GraalJS " + engine.getVersion() + " (" + engine.getImplementationName() + ")";
  }

  @Override
  public String name() {
    return "javascript";
  }

  @Override
  public Set<String> extensions() {
    return Set.of("js", "mjs");
  }

  @Override
  public ScriptInstance create(String name, Path file, ScriptScope scope, ScriptGlobals globals) {
    return new JsScript(this, name, file, scope, globals);
  }

  private static String resource(String name) {
    try (InputStream in = JsLanguage.class.getResourceAsStream("/spectra/js/" + name)) {
      if (in == null) {
        throw new IllegalStateException("Missing runtime resource " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Writes the shim modules to a private temporary directory so the module loader can read them as files. */
  private static Map<String, Path> extractShims() {
    try {
      Path directory = Files.createTempDirectory("spectra-node-");
      directory.toFile().deleteOnExit();
      Map<String, Path> files = new HashMap<>();
      Map<String, Path> builtins = new HashMap<>();
      for (Map.Entry<String, String> shim : SHIMS.entrySet()) {
        Path file = files.get(shim.getValue());
        if (file == null) {
          file = directory.resolve(shim.getValue());
          Files.writeString(file, resource("node/" + shim.getValue()), StandardCharsets.UTF_8);
          file.toFile().deleteOnExit();
          files.put(shim.getValue(), file);
        }
        builtins.put(shim.getKey(), file);
      }
      return Map.copyOf(builtins);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not prepare Node builtin modules", e);
    }
  }

  Source prelude() {
    return prelude;
  }

  Map<String, Path> builtins() {
    return builtins;
  }

  JsNatives natives(com.mathhulk.spectra.core.scripts.ScriptScope scope) {
    return new JsNatives(mainThread, scope, workingDirectory, spectra, players);
  }

  Path workingDirectory() {
    return workingDirectory;
  }

  Engine engine() {
    return engine;
  }

  MainThread mainThread() {
    return mainThread;
  }

  Logger logger() {
    return logger;
  }

  ClassLoader hostClassLoader() {
    return hostClassLoader;
  }

  @Override
  public void close() {
    engine.close();
  }
}
