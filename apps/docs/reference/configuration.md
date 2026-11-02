# Configuration

## Server: `plugins/Spectra/config.yml`

| Key | Default | Description |
|---|---|---|
| `client.require` | `false` | Kick players without a compatible client. Prefer per-feature checks; see [Client capabilities](/ui/capabilities) |
| `client.min-protocol-version` | `0` | With `require`, also kick clients older than this protocol version |
| `client.kick-message` | `This server requires the Spectra client mod.` | Message for players kicked because of `require` |
| `client.handshake-timeout-ticks` | `100` | How long to wait for a handshake before treating a player as unsupported |
| `client.network-allow` | `[]` | Internet origins views may contact. See [Network access](/ui/network) |
| `scripts.watch` | `true` | Hot-reload scripts and resources when files change |
| `resources.bytes-per-tick` | `131072` | Resource upload budget per player per tick |
| `types.generate` | `true` | Generate TypeScript types at startup. See [TypeScript types](/scripting/types) |

Changes take effect on restart.

## Server: files and folders

| Path | Purpose |
|---|---|
| `<server>/scripts/` | Scripts. See [Scripts and lifecycle](/scripting/scripts) |
| `plugins/Spectra/resources/` | UI resources. See [Resources](/ui/resources) |
| `plugins/Spectra/server-id` | A random id identifying this server to clients (resource cache, origin). Keep it to preserve players' caches |
| `<server>/.spectra/types/server.d.ts` | Generated TypeScript types |
| `bukkit.yml` `worlds.<name>.generator: Spectra[:id]` | Use a script's [world generator](/scripting/world-generation) |

## Client: `config/spectra.json`

| Key | Default | Description |
|---|---|---|
| `allowServerNetwork` | `true` | Let views contact allowlisted internet origins |

## SDK: `spectra.config.mjs`

```js
import { defineConfig } from "spectra-sdk";

export default defineConfig({
  server: {
    eula: false,             // must be true to run a server
    type: "paper",           // "paper" | "spigot" | "purpur"
    version: "26.3",         // defaults to the latest release
    dir: ".minecraft",
    java: { path: "java", args: ["-Xmx2G", "-Xms2G"] },
    jar: { path: undefined, args: ["nogui"] }, // use a local server jar instead of downloading
  },
  build: { /* esbuild options: entryPoints, outdir, format, ... */ },
});
```
