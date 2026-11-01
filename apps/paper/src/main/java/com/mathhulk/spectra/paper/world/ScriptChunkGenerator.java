package com.mathhulk.spectra.paper.world;

import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * A {@link ChunkGenerator} implemented by a script object. Every method is optional: implement
 * {@code generateNoise}, {@code generateSurface}, {@code generateBedrock}, {@code generateCaves},
 * {@code getBaseHeight}, {@code getDefaultBiomeProvider}, {@code getDefaultPopulators}, {@code getFixedSpawnLocation},
 * {@code canSpawn}, and the {@code shouldGenerate*} flags (booleans or functions) as needed.
 *
 * <p>Paper generates chunks on several threads; calls into one script are serialized by its lock, so heavy
 * generation in a script runs one chunk at a time.
 */
public final class ScriptChunkGenerator extends ChunkGenerator {
  private final ScriptHooks hooks;
  private final Logger logger;

  public ScriptChunkGenerator(ScriptScope scope, Value generator, Logger logger) {
    this.hooks = new ScriptHooks(scope, generator, logger);
    this.logger = logger;
  }

  @Override
  public void generateNoise(@NonNull WorldInfo worldInfo, @NonNull Random random, int chunkX, int chunkZ,
      @NonNull ChunkData chunkData) {
    hooks.call("generateNoise", worldInfo, random, chunkX, chunkZ, chunkData);
  }

  @Override
  public void generateSurface(@NonNull WorldInfo worldInfo, @NonNull Random random, int chunkX, int chunkZ,
      @NonNull ChunkData chunkData) {
    hooks.call("generateSurface", worldInfo, random, chunkX, chunkZ, chunkData);
  }

  @Override
  public void generateBedrock(@NonNull WorldInfo worldInfo, @NonNull Random random, int chunkX, int chunkZ,
      @NonNull ChunkData chunkData) {
    hooks.call("generateBedrock", worldInfo, random, chunkX, chunkZ, chunkData);
  }

  @Override
  public void generateCaves(@NonNull WorldInfo worldInfo, @NonNull Random random, int chunkX, int chunkZ,
      @NonNull ChunkData chunkData) {
    hooks.call("generateCaves", worldInfo, random, chunkX, chunkZ, chunkData);
  }

  @Override
  public int getBaseHeight(@NonNull WorldInfo worldInfo, @NonNull Random random, int x, int z,
      @NonNull HeightMap heightMap) {
    Value value = hooks.call("getBaseHeight", worldInfo, random, x, z, heightMap);
    Integer height = hooks.as(value, Integer.class);
    return height != null ? height : super.getBaseHeight(worldInfo, random, x, z, heightMap);
  }

  @Override
  public BiomeProvider getDefaultBiomeProvider(@NonNull WorldInfo worldInfo) {
    Value value = hooks.call("getDefaultBiomeProvider", worldInfo);
    if (value == null) {
      return null;
    }
    if (hooks.isHost(value)) {
      return hooks.as(value, BiomeProvider.class);
    }
    return new ScriptBiomeProvider(new ScriptHooks(hooks.scope(), value, logger));
  }

  @Override
  public @NonNull List<BlockPopulator> getDefaultPopulators(@NonNull World world) {
    Value value = hooks.call("getDefaultPopulators", world);
    List<BlockPopulator> populators = new ArrayList<>();
    if (value != null) {
      hooks.read(() -> {
        for (long i = 0; value.hasArrayElements() && i < value.getArraySize(); i++) {
          Value element = value.getArrayElement(i);
          if (element.isHostObject() && element.asHostObject() instanceof BlockPopulator populator) {
            populators.add(populator);
          } else {
            populators.add(new ScriptBlockPopulator(new ScriptHooks(hooks.scope(), element, logger)));
          }
        }
        return null;
      });
    }
    return populators;
  }

  @Override
  public Location getFixedSpawnLocation(@NonNull World world, @NonNull Random random) {
    Location location = hooks.as(hooks.call("getFixedSpawnLocation", world, random), Location.class);
    return location != null ? location : super.getFixedSpawnLocation(world, random);
  }

  @Override
  public boolean canSpawn(@NonNull World world, int x, int z) {
    Boolean value = hooks.as(hooks.call("canSpawn", world, x, z), Boolean.class);
    return value != null ? value : super.canSpawn(world, x, z);
  }

  private boolean flag(String member, boolean fallback) {
    Boolean value = hooks.as(hooks.call(member), Boolean.class);
    return value != null ? value : fallback;
  }

  @Override
  public boolean shouldGenerateNoise() {
    return flag("shouldGenerateNoise", super.shouldGenerateNoise());
  }

  @Override
  public boolean shouldGenerateSurface() {
    return flag("shouldGenerateSurface", super.shouldGenerateSurface());
  }

  @Override
  public boolean shouldGenerateCaves() {
    return flag("shouldGenerateCaves", super.shouldGenerateCaves());
  }

  @Override
  public boolean shouldGenerateDecorations() {
    return flag("shouldGenerateDecorations", super.shouldGenerateDecorations());
  }

  @Override
  public boolean shouldGenerateMobs() {
    return flag("shouldGenerateMobs", super.shouldGenerateMobs());
  }

  @Override
  public boolean shouldGenerateStructures() {
    return flag("shouldGenerateStructures", super.shouldGenerateStructures());
  }

  /** Tells world hooks which thread is the server thread and how to queue work on it (see ScriptHooks). */
  public static void serverThread(Thread thread,
      java.util.function.Supplier<java.util.Collection<java.util.concurrent.Executor>> worlds,
      java.util.concurrent.Executor fallback) {
    ScriptHooks.serverThread(new ScriptHooks.ServerThread(thread, worlds, fallback));
  }

  /** Called from code known to run on the server thread, so world hooks can recognize it. */
  public static void observeServerThread() {
    if (org.bukkit.Bukkit.isPrimaryThread()) {
      ScriptHooks.observeServerThread(Thread.currentThread());
    }
  }

  /** Wraps a biome provider object returned by a script's {@code getDefaultBiomeProvider} export. */
  public static BiomeProvider biomeProvider(ScriptScope scope, Value provider, Logger logger) {
    return new ScriptBiomeProvider(new ScriptHooks(scope, provider, logger));
  }
}
