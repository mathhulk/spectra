package com.mathhulk.spectra.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePathTest {
  @ParameterizedTest
  @ValueSource(strings = {"index.html", "shop/index.html", "assets/img/sword@2x.png", "a-b_c.d/e f.js"})
  void acceptsSafePaths(String path) {
    assertTrue(ResourcePath.isValid(path));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/etc/passwd", "../secret", "a/../../b", "a/./b", "a//b", "a\\b", "C:/x", "a/",
      "con:x", " a", "a /b", "\u0000"})
  void rejectsUnsafePaths(String path) {
    assertFalse(ResourcePath.isValid(path));
  }

  @Test
  void rejectsOverlongPaths() {
    assertFalse(ResourcePath.isValid("a".repeat(ResourcePath.MAX_LENGTH + 1)));
  }
}
