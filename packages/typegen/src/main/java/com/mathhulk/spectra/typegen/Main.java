package com.mathhulk.spectra.typegen;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Build-time entry point.
 *
 * <pre>
 * --classpath a.jar:b.jar   classes to load from
 * --jar a.jar               jar(s) whose classes to declare (repeatable)
 * --sources a-sources.jar   sources for names and docs (repeatable)
 * --docs-out docs.tsv.gz    write the docs index
 * --out types.d.ts          write declarations
 * </pre>
 */
public final class Main {
  public static void main(String[] args) throws Exception {
    List<Path> classpath = new ArrayList<>();
    List<Path> jars = new ArrayList<>();
    List<Path> sources = new ArrayList<>();
    Path docsOut = null;
    Path out = null;
    for (int i = 0; i < args.length; i += 2) {
      String value = args[i + 1];
      switch (args[i]) {
        case "--classpath" -> {
          for (String entry : value.split(java.io.File.pathSeparator)) {
            if (!entry.isBlank()) {
              classpath.add(Path.of(entry));
            }
          }
        }
        case "--jar" -> jars.add(Path.of(value));
        case "--sources" -> sources.add(Path.of(value));
        case "--docs-out" -> docsOut = Path.of(value);
        case "--out" -> out = Path.of(value);
        default -> throw new IllegalArgumentException("Unknown option " + args[i]);
      }
    }
    Docs docs = SourceDocs.read(sources);
    if (docsOut != null) {
      Files.createDirectories(docsOut.toAbsolutePath().getParent());
      try (OutputStream stream = Files.newOutputStream(docsOut)) {
        docs.write(stream);
      }
      System.out.println("Docs index: " + docs.size() + " entries -> " + docsOut);
    }
    if (out != null) {
      List<URL> urls = new ArrayList<>();
      for (Path path : classpath) {
        urls.add(path.toUri().toURL());
      }
      for (Path jar : jars) {
        urls.add(jar.toUri().toURL());
      }
      try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), Main.class.getClassLoader())) {
        List<Class<?>> classes = new ArrayList<>();
        for (Path jar : jars) {
          classes.addAll(ClassScanner.scan(jar, loader, name -> true));
        }
        String declarations = new Generator(docs).generate(classes);
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, declarations);
        System.out.println("Declarations: " + classes.size() + " classes -> " + out);
      }
    }
  }
}
