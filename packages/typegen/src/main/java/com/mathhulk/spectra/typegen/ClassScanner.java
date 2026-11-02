package com.mathhulk.spectra.typegen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Loads the public classes of jar files (without initializing them) for {@link Generator}. */
public final class ClassScanner {
  private ClassScanner() {
  }

  public static List<Class<?>> scan(Path jar, ClassLoader loader, Predicate<String> include) throws IOException {
    List<Class<?>> classes = new ArrayList<>();
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        String entry = entries.nextElement().getName();
        if (!entry.endsWith(".class") || entry.startsWith("META-INF/") || entry.endsWith("module-info.class")
            || entry.endsWith("package-info.class")) {
          continue;
        }
        String name = entry.substring(0, entry.length() - ".class".length()).replace('/', '.');
        if (name.matches(".*\\$\\d.*") || !include.test(name)) {
          continue;
        }
        try {
          Class<?> type = Class.forName(name, false, loader);
          if (Generator.eligible(type)) {
            classes.add(type);
          }
        } catch (ClassNotFoundException | LinkageError e) {
          // Optional dependency missing on this classpath: skip the class.
        }
      }
    }
    return classes;
  }
}
