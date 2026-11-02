package com.mathhulk.spectra.core.scripts;

import java.nio.file.Path;
import java.util.Set;

/** A scripting language runtime (GraalJS today; Luau later). */
public interface ScriptLanguage extends AutoCloseable {
  String name();

  /** File extensions this language loads, without the dot. */
  Set<String> extensions();

  /** Creates (but does not load) a script. {@code globals} supplies the host API for this script's scope. */
  ScriptInstance create(String name, Path file, ScriptScope scope, ScriptGlobals globals);

  @Override
  void close();
}
