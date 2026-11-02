package com.mathhulk.spectra.paper.world;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jspecify.annotations.NonNull;

import java.util.Random;

/** A {@link BlockPopulator} implemented by a script object with a {@code populate} method (or a function). */
public final class ScriptBlockPopulator extends BlockPopulator {
  private final ScriptHooks hooks;

  ScriptBlockPopulator(ScriptHooks hooks) {
    this.hooks = hooks;
  }

  @Override
  public void populate(@NonNull WorldInfo worldInfo, @NonNull Random random, int chunkX, int chunkZ,
      @NonNull LimitedRegion region) {
    hooks.callSelfOr("populate", worldInfo, random, chunkX, chunkZ, region);
  }
}
