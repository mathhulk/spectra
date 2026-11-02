package com.mathhulk.spectra.runtime.js;

import com.mathhulk.spectra.core.scripts.ScriptGlobals;
import com.mathhulk.spectra.core.scripts.ScriptInstance;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;

/** One JavaScript ES module with its own context. All entry into the context goes through the scope's lock. */
final class JsScript implements ScriptInstance {
  private static final String ID = "js";

  private final JsLanguage language;
  private final String name;
  private final Path file;
  private final ScriptScope scope;
  private final ScriptGlobals globals;
  private Context context;
  private Value exports;

  JsScript(JsLanguage language, String name, Path file, ScriptScope scope, ScriptGlobals globals) {
    this.language = language;
    this.name = name;
    this.file = file;
    this.scope = scope;
    this.globals = globals;
  }

  @Override
  public void load() throws IOException {
    Source source = Source.newBuilder(ID, file.toFile()).mimeType("application/javascript+module").build();
    scope.locked(() -> {
      // Scripts are trusted operator code: full host access (the escape hatch to the platform's Java API), but
      // file access is read-only (for module imports) and scripts cannot spawn threads or load native code.
      context = Context.newBuilder(ID)
          .engine(language.engine())
          .allowHostAccess(HostAccess.ALL)
          .allowHostClassLookup(className -> true)
          .hostClassLoader(language.hostClassLoader())
          .allowPolyglotAccess(PolyglotAccess.NONE)
          .allowCreateThread(false)
          .allowNativeAccess(false)
          .allowIO(IOAccess.newBuilder().fileSystem(new ModuleFileSystem(language.builtins())).build())
          .allowExperimentalOptions(true)
          .option("js.esm-eval-returns-exports", "true")
          .option("js.ecmascript-version", "latest")
          .currentWorkingDirectory(file.toAbsolutePath().getParent())
          .out(new LogOutputStream(language.logger(), Level.INFO, name))
          .err(new LogOutputStream(language.logger(), Level.WARNING, name))
          .build();

      Value bindings = context.getBindings(ID);
      Timers timers = new Timers(language.mainThread(), scope);
      bindings.putMember("setTimeout", timers.setTimeout());
      bindings.putMember("setInterval", timers.setInterval());
      bindings.putMember("clearTimeout", timers.clear());
      bindings.putMember("clearInterval", timers.clear());
      for (Map.Entry<String, Object> global : globals.create(scope).entrySet()) {
        bindings.putMember(global.getKey(), global.getValue());
      }
      bindings.putMember("__spectraNative", language.natives(scope));
      context.eval(language.prelude());

      exports = context.eval(source);
      return null;
    });
  }

  private void callExport(String hook) {
    scope.locked(() -> {
      Value function = exports == null ? null : exports.getMember(hook);
      if (function != null && function.canExecute()) {
        function.executeVoid();
      }
      return null;
    });
  }

  @Override
  public void enable() {
    callExport("onEnable");
  }

  @Override
  public void disable() {
    callExport("onDisable");
  }

  @Override
  public Object export(String member) {
    Value value = exports == null ? null : exports.getMember(member);
    return value == null || value.isNull() ? null : value;
  }

  @Override
  public void close() {
    scope.locked(() -> {
      if (context != null) {
        context.close();
        context = null;
        exports = null;
      }
      return null;
    });
  }
}
