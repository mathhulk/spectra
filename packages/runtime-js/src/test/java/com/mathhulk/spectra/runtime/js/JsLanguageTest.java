package com.mathhulk.spectra.runtime.js;

import com.mathhulk.spectra.core.FakePlatform;
import com.mathhulk.spectra.core.scripts.ScriptManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class JsLanguageTest {
  @TempDir
  Path directory;

  private FakePlatform platform;
  private JsLanguage language;
  private ScriptManager manager;
  /** Exposed to scripts as `record(value)`. */
  private final List<Object> recorded = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() {
    platform = new FakePlatform(directory);
    language = new JsLanguage(platform.mainThread(), platform.logger(), getClass().getClassLoader());
    manager = new ScriptManager(platform, directory, List.of(language),
        scope -> Map.of("record", (Consumer<Object>) value -> recorded.add(String.valueOf(value))));
  }

  @AfterEach
  void tearDown() {
    manager.close();
    language.close();
  }

  private void script(String name, String code) throws Exception {
    Files.writeString(directory.resolve(name), code);
  }

  @Test
  void runsLifecycleHooksAndImportsModules() throws Exception {
    Files.createDirectories(directory.resolve("lib"));
    script("lib/greet.mjs", "export const greet = (name) => `hello ${name}`;");
    script("main.js", """
        import { greet } from './lib/greet.mjs';
        record(greet('top-level'));
        export const onEnable = () => record('enabled');
        export const onDisable = () => record('disabled');
        """);

    manager.loadAll();
    manager.enableAll();
    manager.disableAll();
    assertEquals(List.of("hello top-level", "enabled", "disabled"), recorded);
  }

  @Test
  void timersUseMillisecondsAndAreReleasedOnUnload() throws Exception {
    script("timers.js", """
        setTimeout((a, b) => record(`timeout ${a} ${b}`), 500, 'x', 'y');
        const id = setInterval(() => record('interval'), 100);
        setTimeout(() => clearInterval(id), 450);
        setInterval(() => record('forever'), 1000);
        """);
    manager.loadAll();

    platform.ticks(10);
    // 100 ms = 2 ticks: ticks 2, 4, 6, 8 before the clear at tick 9; the timeout fires at tick 10 (500 ms).
    assertEquals(4, recorded.stream().filter("interval"::equals).count());
    assertTrue(recorded.contains("timeout x y"));

    manager.unload("timers.js");
    recorded.clear();
    platform.ticks(100);
    assertTrue(recorded.isEmpty(), "timers must not outlive their script");
  }

  @Test
  void errorsInCallbacksAreContained() throws Exception {
    script("boom.js", """
        setTimeout(() => { throw new Error('boom'); }, 50);
        setTimeout(() => record('still running'), 100);
        """);
    manager.loadAll();
    platform.ticks(3);
    assertEquals(List.of("still running"), recorded);
  }

  @Test
  void syntaxErrorsFailTheLoadOnly() throws Exception {
    script("bad.js", "export const = ;");
    script("good.js", "record('good');");
    manager.loadAll();
    assertEquals(List.of("good"), recorded);
    assertEquals(1, manager.list().size());
  }

  @Test
  void hostInteropWorks() throws Exception {
    script("java.js", """
        const ArrayList = Java.type('java.util.ArrayList');
        const list = new ArrayList();
        list.add('from java');
        record(list.get(0));
        """);
    manager.loadAll();
    assertEquals(List.of("from java"), recorded);
  }

  @Test
  void reportsExecutionMode() {
    // On a stock JDK this is interpreter mode; on GraalVM it is compiled. Either way it must be reported.
    assertNotNull(language.engineDescription());
    System.out.println(language.engineDescription() + ", compiler=" + language.compilerAvailable());
  }
}
