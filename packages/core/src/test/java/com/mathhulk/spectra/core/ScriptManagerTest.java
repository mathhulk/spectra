package com.mathhulk.spectra.core;

import com.mathhulk.spectra.core.scripts.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScriptManagerTest {
  @TempDir
  Path directory;

  private final List<String> log = new ArrayList<>();
  private ScriptManager manager;

  /** A fake language whose scripts log lifecycle calls; files containing "fail" fail to load. */
  private final class FakeLanguage implements ScriptLanguage {
    @Override
    public String name() {
      return "fake";
    }

    @Override
    public Set<String> extensions() {
      return Set.of("fake");
    }

    @Override
    public ScriptInstance create(String name, Path file, ScriptScope scope, ScriptGlobals globals) {
      globals.create(scope);
      return new ScriptInstance() {
        @Override
        public void load() throws Exception {
          if (Files.readString(file).contains("fail")) {
            throw new IllegalStateException("boom");
          }
          scope.track(() -> log.add("released " + name));
          log.add("load " + name);
        }

        @Override
        public void enable() {
          log.add("enable " + name);
        }

        @Override
        public void disable() {
          log.add("disable " + name);
        }

        @Override
        public void close() {
          log.add("close " + name);
        }
      };
    }

    @Override
    public void close() {
    }
  }

  @BeforeEach
  void setUp() {
    manager = new ScriptManager(new FakePlatform(directory), directory, List.of(new FakeLanguage()),
        scope -> Map.of());
  }

  @Test
  void loadsEnablesAndUnloadsInOrder() throws Exception {
    Files.writeString(directory.resolve("b.fake"), "");
    Files.writeString(directory.resolve("a.fake"), "");
    Files.writeString(directory.resolve("ignored.txt"), "");

    manager.loadAll();
    manager.enableAll();
    assertEquals(List.of("load a.fake", "load b.fake", "enable a.fake", "enable b.fake"), log);

    log.clear();
    manager.close();
    assertEquals(List.of("disable b.fake", "disable a.fake", "released a.fake", "close a.fake", "released b.fake",
        "close b.fake"), log);
  }

  @Test
  void reloadReleasesTheOldScopeFirst() throws Exception {
    Files.writeString(directory.resolve("a.fake"), "");
    manager.loadAll();
    manager.enableAll();
    log.clear();

    assertTrue(manager.reload("a.fake"));
    assertEquals(List.of("disable a.fake", "released a.fake", "close a.fake", "load a.fake", "enable a.fake"), log);
  }

  @Test
  void failedLoadsAreNotRegisteredAndReleaseEverything() throws Exception {
    Files.writeString(directory.resolve("bad.fake"), "fail");
    manager.loadAll();
    assertTrue(manager.list().isEmpty());
    assertEquals(List.of("close bad.fake"), log);
  }

  @Test
  void reloadRejectsPathsOutsideTheDirectory() {
    assertFalse(manager.reload("../escape.fake"));
  }
}
