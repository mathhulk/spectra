# Resources

```java
Registration registration = Spectra.get().resources()
    .register("myplugin", getDataFolder().toPath().resolve("ui"));
```

- **Paths.** Files under the directory appear as `myplugin/...` resource paths.
- **Name rules.** Namespaces match `[a-z0-9_.-]` (1 to 64 characters) and must be unique.
- **Watching.** The directory is watched when `scripts.watch` is on. Changes push to connected players.
- **Rescanning.** `resources().rescan()` forces a rescan, for example after extracting files from your jar.

See [Resources](/ui/resources) for syncing, limits and path rules.
