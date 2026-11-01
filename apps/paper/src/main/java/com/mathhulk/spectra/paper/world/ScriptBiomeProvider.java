package com.mathhulk.spectra.paper.world;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** A {@link BiomeProvider} implemented by a script object with {@code getBiome} and {@code getBiomes}. */
public final class ScriptBiomeProvider extends BiomeProvider {
  private final ScriptHooks hooks;

  ScriptBiomeProvider(ScriptHooks hooks) {
    this.hooks = hooks;
  }

  @Override
  public @NonNull Biome getBiome(@NonNull WorldInfo worldInfo, int x, int y, int z) {
    Biome biome = hooks.as(hooks.call("getBiome", worldInfo, x, y, z), Biome.class);
    return biome != null ? biome : Biome.PLAINS;
  }

  @Override
  public @NonNull List<Biome> getBiomes(@NonNull WorldInfo worldInfo) {
    Value value = hooks.call("getBiomes", worldInfo);
    List<Biome> biomes = new ArrayList<>();
    if (value != null) {
      hooks.read(() -> {
        for (long i = 0; value.hasArrayElements() && i < value.getArraySize(); i++) {
          Value element = value.getArrayElement(i);
          if (element.isHostObject() && element.asHostObject() instanceof Biome biome) {
            biomes.add(biome);
          }
        }
        return null;
      });
    }
    return biomes.isEmpty() ? List.of(Biome.PLAINS) : biomes;
  }
}
