package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.typegen.ClassScanner;
import com.mathhulk.spectra.typegen.Docs;
import com.mathhulk.spectra.typegen.Generator;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates TypeScript declarations for exactly what this server runs: Paper API plus the public API of every
 * other installed plugin. Written to {@code <server>/.spectra/types/server.d.ts}, where {@code spectra-sdk dev}
 * picks it up. Runs off the main thread at startup.
 */
final class TypeGeneration {
  private TypeGeneration() {
  }

  static void run(Plugin spectra, Path serverDirectory, Logger logger) {
    long start = System.nanoTime();
    try {
      Docs docs = Docs.EMPTY;
      try (InputStream in = TypeGeneration.class.getResourceAsStream("/spectra/paper-api-docs.tsv.gz")) {
        if (in != null) {
          docs = Docs.read(in);
        }
      }
      List<Class<?>> classes = new ArrayList<>();
      Path api = jarOf(Bukkit.class);
      if (api != null) {
        classes.addAll(ClassScanner.scan(api, Bukkit.class.getClassLoader(), name ->
            name.startsWith("org.bukkit.") || name.startsWith("io.papermc.paper.") || name.startsWith("com.destroystokyo.")
                || name.startsWith("net.kyori.adventure.")));
      }
      for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
        if (plugin == spectra) {
          continue;
        }
        Path jar = jarOf(plugin.getClass());
        String main = plugin.getClass().getPackageName();
        String[] segments = main.split("\\.");
        String prefix = segments.length >= 2 ? segments[0] + "." + segments[1] + "." : main + ".";
        if (jar != null) {
          classes.addAll(ClassScanner.scan(jar, plugin.getClass().getClassLoader(), name -> name.startsWith(prefix)));
        }
      }
      String declarations = new Generator(docs).generate(classes);
      Path output = serverDirectory.resolve(".spectra/types/server.d.ts");
      Files.createDirectories(output.getParent());
      Files.writeString(output, declarations);
      logger.info("Generated TypeScript types for " + classes.size() + " classes in "
          + (System.nanoTime() - start) / 1_000_000 + " ms: " + output);
    } catch (Exception | LinkageError e) {
      logger.log(Level.WARNING, "Could not generate TypeScript types", e);
    }
  }

  private static Path jarOf(Class<?> type) {
    try {
      var source = type.getProtectionDomain().getCodeSource();
      if (source == null) {
        return null;
      }
      Path path = Path.of(source.getLocation().toURI());
      return Files.isRegularFile(path) && path.toString().endsWith(".jar") ? path : null;
    } catch (Exception e) {
      return null;
    }
  }
}
