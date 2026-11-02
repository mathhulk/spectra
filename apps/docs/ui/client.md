# Client mod

## Installing

For Minecraft 26.3 with Fabric Loader 0.20 or newer, install:

- **Fabric API**
- **[Rinku](https://modrinth.com/mod/rinku)** 3.0.5 or newer: the embedded Chromium browser
- **Spectra**

On first launch Rinku downloads Chromium for your platform (about 150 MB).

## Settings

`config/spectra.json`:

| Setting | Default | Description |
|---|---|---|
| `allowServerNetwork` | `true` | Let views contact internet origins the server allowlists. See [Network access](/ui/network) |

## Cache

Downloaded resources are stored by content hash in `spectra/objects/` in the game folder, and shared across servers. Deleting the folder is safe; files download again when needed.

## Browser engine

Spectra currently uses Rinku (Chromium 151, frame rate not capped at 30). The plan is a Spectra-maintained fork that adds GPU shared-texture rendering (from CCBlueX's java-cef) and newer Chromium releases. See [Project status](/guide/status).

**Known limitations:**

- **Proprietary codecs.** H.264 and AAC aren't supported. Use VP9, AV1 and Opus for video and audio.
- **Shared browser client.** The request handler that enforces Spectra's network rules lives on Rinku's shared browser client. A mod that replaces it would bypass those rules.
