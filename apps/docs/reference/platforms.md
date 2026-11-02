# Platforms

| Platform | Status |
|---|---|
| **Paper 26.3** | Supported: the primary target |
| Purpur | Expected to work through Paper's API (not tested yet) |
| Spigot / Bukkit | Not supported. Paper has hard-forked from Spigot, and the plugin uses Paper APIs |
| Folia | Not supported. Folia has no main thread, which doesn't fit single-threaded script contexts yet |
| Velocity | Planned, to replace BungeeCord support |
| BungeeCord / Waterfall | Retired (Waterfall is end-of-life) |
| Minestom | Being considered for scripting-first servers |
| **Fabric client, Minecraft 26.3** | Supported |
| NeoForge client | Planned |

Server adapters are thin layers over the platform-independent core. See [Architecture](/guide/architecture).
