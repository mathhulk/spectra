package com.mathhulk.spectra.core.scripts;

import java.util.Map;

/**
 * Supplies the host objects a script sees as globals. Adapters build these per script so every registration a
 * global performs is tracked by that script's {@link ScriptScope}.
 */
@FunctionalInterface
public interface ScriptGlobals {
  Map<String, Object> create(ScriptScope scope);
}
