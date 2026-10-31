package com.mathhulk.spectra.runtime.js;

import com.mathhulk.spectra.core.platform.MainThread;
import com.mathhulk.spectra.core.scripts.ScriptGlobals;
import com.mathhulk.spectra.core.scripts.ScriptInstance;
import com.mathhulk.spectra.core.scripts.ScriptLanguage;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.graalvm.polyglot.Engine;

import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JavaScript (and bundled TypeScript) via GraalJS. One {@link Engine} is shared by every script so parsed code is
 * cached across scripts and hot reloads; each script gets its own context.
 *
 * <p>GraalJS optimizes code only when the JVM provides the Graal compiler (GraalVM, or a JVMCI-enabled JDK).
 * Otherwise it runs in interpreter mode: fully functional, but slower. {@link #compilerAvailable()} reports which.
 */
public final class JsLanguage implements ScriptLanguage {
  private final Engine engine;
  private final MainThread mainThread;
  private final Logger logger;
  private final ClassLoader hostClassLoader;

  public JsLanguage(MainThread mainThread, Logger logger, ClassLoader hostClassLoader) {
    this.mainThread = mainThread;
    this.logger = logger;
    this.hostClassLoader = hostClassLoader;
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
