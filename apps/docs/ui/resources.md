# Resources

Resources are the files views are made of: HTML, JavaScript, CSS, images, fonts.

## Location

Put them in `plugins/Spectra/resources/` on the server. The path relative to that folder is the resource path you open:

```
plugins/Spectra/resources/
├─ shop/index.html     → spectra.openScreen(player, "shop", "shop/index.html")
├─ shop/app.js         → <script type="module" src="./app.js">
└─ hud/minimap.html
```

Build tools work as usual. Point Vite or esbuild's output at a folder in `resources/`.

## Syncing

- **Content-addressed.** Each file is identified by its SHA-256. Clients download only files they don't have, and verify each one before storing it. Identical files are stored once on the client, even across servers.
- **Live updates.** When files change (watching is on by default), the server pushes a new manifest to connected players, who download only what changed.
- **Upload pacing.** Uploads are limited per player per tick (`resources.bytes-per-tick`, 128 KiB by default, about 2.5 MiB/s), so syncing never stalls the server.
- **Size limit.** The total is capped at 256 MiB.

## Path rules

Resource paths are relative and use `/`. Each segment may contain only letters, digits and `. _ @ + -` and spaces. Files with other names are skipped with a warning.

## Plugin namespaces

Java plugins ship their own UI under a namespace:

```java
Spectra.get().resources().register("myplugin", getDataFolder().toPath().resolve("ui"));
// ui/shop/index.html is now available as "myplugin/shop/index.html"
```

A namespace takes precedence over a root folder with the same name. See [Java API](/java/).
