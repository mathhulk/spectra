# World generation

Scripts can provide world generators, biome providers and block populators, the same as Java plugins.

## Selecting the generator

In `bukkit.yml`:

```yaml
worlds:
  my_world:
    generator: Spectra:mesa
```

The `Spectra` plugin loads at `STARTUP`, before worlds load, so scripts are ready when the world asks for its generator.

## Providing it

Export `getDefaultWorldGenerator(worldName, id)` and return an object, or `null` to let another script answer. The first script that returns a generator wins.

```ts
const Material = org.bukkit.Material;
const Biome = org.bukkit.block.Biome;

export function getDefaultWorldGenerator(worldName: string, id: string) {
  if (id !== "mesa") return null;
  return {
    generateNoise(info, random, chunkX, chunkZ, data) {
      data.setRegion(0, info.getMinHeight(), 0, 16, 64, 16, Material.STONE);
    },
    shouldGenerateCaves: false,
    getDefaultBiomeProvider: () => ({
      getBiome: () => Biome.DESERT,
      getBiomes: () => [Biome.DESERT],
    }),
    getDefaultPopulators: () => [
      (info, random, chunkX, chunkZ, region) =>
        region.setType(chunkX * 16 + 8, 64, chunkZ * 16 + 8, Material.GOLD_BLOCK),
    ],
    getFixedSpawnLocation: (world) => new org.bukkit.Location(world, 8, 65, 8),
  };
}
```

Every member is optional; anything missing falls back to Paper's default.

| Member | Notes |
|---|---|
| `generateNoise`, `generateSurface`, `generateBedrock`, `generateCaves` | `(worldInfo, random, chunkX, chunkZ, chunkData)` |
| `getBaseHeight` | `(worldInfo, random, x, z, heightMap)`, returns a number |
| `getDefaultBiomeProvider` | Returns an object with `getBiome(worldInfo, x, y, z)` and `getBiomes(worldInfo)`, or a Java `BiomeProvider` |
| `getDefaultPopulators` | An array of functions, objects with `populate(...)`, or Java `BlockPopulator`s |
| `getFixedSpawnLocation`, `canSpawn` | As in Bukkit |
| `shouldGenerateNoise`, `shouldGenerateSurface`, `shouldGenerateCaves`, `shouldGenerateDecorations`, `shouldGenerateMobs`, `shouldGenerateStructures` | Booleans or functions |

`getDefaultBiomeProvider(worldName, id)` can also be exported on its own, for `biome-provider: Spectra` setups.

## Errors

Errors in a generator are logged once per member, with the script name, and generation falls back to the default.

## Threading and performance

Paper generates chunks on several worker threads, but one script's code runs one call at a time. Two consequences:

- **Speed.** A script-defined generator is limited to one thread. Biome lookups happen thousands of times per chunk, so in interpreter mode they are the slowest part. Prefer simple biome logic, and run the server on GraalVM for compiled performance. See [Performance](/scripting/performance).
- **No deadlocks.** A script reading a block on the main thread can trigger generation that needs the same script. Spectra detects this and runs the generator call on the main thread while it waits.

::: warning Hot reload
A world keeps the generator object it was given. After the script reloads, that object belongs to the unloaded script, and generation falls back to defaults with a warning. Restart the server to use a changed generator.
:::
