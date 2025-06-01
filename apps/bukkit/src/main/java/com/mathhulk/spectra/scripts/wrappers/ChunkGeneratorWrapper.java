package com.mathhulk.spectra.scripts.wrappers;

import java.util.List;
import java.util.Random;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class ChunkGeneratorWrapper extends ChunkGenerator {
  private final Context context;
  private final Value generator;

  public ChunkGeneratorWrapper(Context context, Value generator) {
    this.context = context;
    this.generator = generator;
  }

  @Override
  public Location getFixedSpawnLocation(World world, Random random) {
    synchronized (context) {
      if (!generator.canInvokeMember("getFixedSpawnLocation")) {
        return super.getFixedSpawnLocation(world, random);
      }

      try {
        Value value = generator.getMember("getFixedSpawnLocation").execute(world,
            random);

        if (!value.isHostObject() || !(value.asHostObject() instanceof Location)) {
          return super.getFixedSpawnLocation(world, random);
        }

        return value.as(Location.class);
      } catch (Exception e) {
        return super.getFixedSpawnLocation(world, random);
      }
    }
  }

  @Override
  public boolean canSpawn(World world, int x, int z) {
    synchronized (context) {
      if (!generator.canInvokeMember("canSpawn")) {
        return super.canSpawn(world, x, z);
      }

      try {
        Value value = generator.getMember("canSpawn").execute(world, x, z);

        if (!value.isBoolean()) {
          return super.canSpawn(world, x, z);
        }

        return value.asBoolean();
      } catch (Exception e) {
        return super.canSpawn(world, x, z);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<BlockPopulator> getDefaultPopulators(World world) {
    synchronized (context) {
      if (!generator.canInvokeMember("getDefaultPopulators")) {
        return super.getDefaultPopulators(world);
      }

      try {
        Value value = generator.getMember("getDefaultPopulators").execute(world);

        if (!value.hasArrayElements()) {
          return super.getDefaultPopulators(world);
        }

        return value.as(List.class);
      } catch (Exception e) {
        return super.getDefaultPopulators(world);
      }
    }
  }

  @Override
  public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
    synchronized (context) {
      if (!generator.canInvokeMember("getDefaultBiomeProvider")) {
        return super.getDefaultBiomeProvider(worldInfo);
      }

      try {
        Value value = generator.getMember("getDefaultBiomeProvider").execute(worldInfo);

        if (!value.hasMembers()) {
          return super.getDefaultBiomeProvider(worldInfo);
        }

        return new BiomeProviderWrapper(context, value);
      } catch (Exception e) {
        return super.getDefaultBiomeProvider(worldInfo);
      }
    }
  }

  @Override
  public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z,
      HeightMap heightMap) {
    synchronized (context) {
      if (!generator.canInvokeMember("getBaseHeight")) {
        return super.getBaseHeight(worldInfo, random, x, z, heightMap);
      }

      try {
        Value value = generator.getMember("getBaseHeight").execute(worldInfo, random,
            x, z, heightMap);

        if (!value.isNumber()) {
          return super.getBaseHeight(worldInfo, random, x, z, heightMap);
        }

        return value.asInt();
      } catch (Exception e) {
        return super.getBaseHeight(worldInfo, random, x, z, heightMap);
      }
    }
  }

  @Override
  public void generateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
    synchronized (context) {
      if (!generator.canInvokeMember("generateCaves")) {
        super.generateNoise(worldInfo, random, chunkX, chunkZ, chunkData);

        return;
      }

      try {
        generator.getMember("generateCaves").execute(worldInfo, random, chunkX,
            chunkZ, chunkData);
      } catch (Exception e) {
        return;
      }
    }
  }

  @Override
  public void generateBedrock(WorldInfo worldInfo, Random random, int chunkX,
      int chunkZ, ChunkData chunkData) {
    synchronized (context) {
      if (!generator.canInvokeMember("generateBedrock")) {
        super.generateBedrock(worldInfo, random, chunkX, chunkZ, chunkData);

        return;
      }

      try {
        generator.getMember("generateBedrock").execute(worldInfo, random, chunkX,
            chunkZ, chunkData);
      } catch (Exception e) {
        return;
      }
    }
  }

  @Override
  public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
    synchronized (context) {
      if (!generator.canInvokeMember("generateNoise")) {
        super.generateNoise(worldInfo, random, chunkX, chunkZ, chunkData);

        return;
      }

      try {
        generator.getMember("generateNoise").execute(worldInfo, random, chunkX,
            chunkZ, chunkData);
      } catch (Exception e) {
        return;
      }
    }
  }

  @Override
  public void generateSurface(WorldInfo worldInfo, Random random, int chunkX,
      int chunkZ, ChunkData chunkData) {
    synchronized (context) {
      if (!generator.canInvokeMember("generateSurface")) {
        super.generateSurface(worldInfo, random, chunkX, chunkZ, chunkData);

        return;
      }

      try {
        generator.getMember("generateSurface").execute(worldInfo, random, chunkX,
            chunkZ, chunkData);
      } catch (Exception e) {
        return;
      }
    }
  }
}