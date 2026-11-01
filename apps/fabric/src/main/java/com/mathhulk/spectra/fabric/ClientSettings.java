package com.mathhulk.spectra.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Player-side settings in {@code config/spectra.json}. */
public final class ClientSettings {
  /** Let server views contact internet origins the server allowlists (e.g. a map tile server). */
  public boolean allowServerNetwork = true;

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public static ClientSettings load(Logger logger) {
    Path file = FabricLoader.getInstance().getConfigDir().resolve("spectra.json");
    try {
      if (Files.isRegularFile(file)) {
        ClientSettings settings = GSON.fromJson(Files.readString(file), ClientSettings.class);
        if (settings != null) {
          return settings;
        }
      }
      ClientSettings defaults = new ClientSettings();
      Files.createDirectories(file.getParent());
      Files.writeString(file, GSON.toJson(defaults));
      return defaults;
    } catch (IOException | RuntimeException e) {
      logger.warn("Could not read {}; using defaults", file, e);
      return new ClientSettings();
    }
  }
}
