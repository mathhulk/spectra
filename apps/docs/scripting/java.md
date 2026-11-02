# Java interop

Scripts call the server's Java API directly.

## Accessing classes

Java packages are globals, as in Nashorn and Rhino:

```ts
const Material = org.bukkit.Material;
const item = new org.bukkit.inventory.ItemStack(Material.DIAMOND, 3);
```

`Java.type("org.bukkit.Material")` works too. Nested classes use their binary names: `org.bukkit.generator.ChunkGenerator$ChunkData`.

## Values

- **Java objects:** call methods and read public fields as usual.
- **Numbers, strings and booleans** convert automatically.
- **Java arrays and `List`s** are indexable and have `.length` or `.size()`.
- **JavaScript functions** can be passed wherever Java expects a functional interface (`Runnable`, `Consumer`, `Predicate`, ...).
- **JavaScript objects** become `Map`s when Java expects one, and arrays become `List`s.
- **Instance checks:** `value instanceof org.bukkit.entity.Player` works.

## APIs that need a Plugin

Many Bukkit APIs take a `Plugin`. Use the `plugin` global (the Spectra plugin):

```ts
const key = new org.bukkit.NamespacedKey(plugin, "coins");
player.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.INTEGER, 10);
```

## Sandbox

Scripts are trusted operator code:

- **Allowed:** full access to Java classes, and read-only file access for module loading. `node:fs` can write.
- **Not allowed:** starting threads or loading native code.
