
package com.mathhulk.spectra.scripts.wrappers;

import java.util.List;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeParameterPoint;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class BiomeProviderWrapper extends BiomeProvider {
  private final Context context;
  private final Value provider;

  public BiomeProviderWrapper(Context context, Value provider) {
    this.context = context;
    this.provider = provider;
  }

  @Override
  public Biome getBiome(WorldInfo worldInfo, int x, int y, int z, BiomeParameterPoint biomeParameterPoint) {
    synchronized (context) {
      if (!provider.canInvokeMember("getBiome")) {
        return null;
      }

      try {
        Value value = provider.getMember("getBiome").execute(worldInfo, x, y, z, biomeParameterPoint);

        if (!value.isHostObject() || !(value.asHostObject() instanceof Biome)) {
          return null;
        }

        return value.as(Biome.class);
      } catch (Exception e) {
        return null;
      }
    }
  }

  @Override
  public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
    synchronized (context) {
      if (!provider.canInvokeMember("getBiome")) {
        return null;
      }

      try {
        Value value = provider.getMember("getBiome").execute(worldInfo, x, y, z);

        if (!value.isHostObject() || !(value.asHostObject() instanceof Biome)) {
          return null;
        }

        return value.as(Biome.class);
      } catch (Exception e) {
        return null;
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Biome> getBiomes(WorldInfo worldInfo) {
    synchronized (context) {
      if (!provider.canInvokeMember("getBiomes")) {
        return null;
      }

      try {
        Value value = provider.getMember("getBiomes").execute(worldInfo);

        if (!value.hasArrayElements()) {
          return null;
        }

        return value.as(List.class);
      } catch (Exception e) {
        return null;
      }
    }
  }
}