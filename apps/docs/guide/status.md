# Project status

This page tracks what is built and how it was verified, plus the open decisions where feedback is most useful.

## Verified

| Area | How it was verified |
|---|---|
| Protocol: framing, limits, path validation | Unit tests |
| Handshake, capabilities, resource sync, views, HTTP/WebSocket tunnels (server side) | Unit tests against a fake platform |
| Scripts, timers, hot reload, events, commands | Real Paper 26.3 server |
| World generators, biome providers, populators, fixed spawn | Real Paper 26.3 server, including a script reading blocks its own generator produces |
| Web APIs, Node builtins, outgoing `fetch` | Unit tests, plus `fetch` to the internet from a real server |
| `fetch` and `websocket` events answering view requests | Unit tests through the full server stack |
| TypeScript type generation | Unit tests. On a real server: 2,116 classes in about 3 s |
| Protocol v2 handshake and resource sync with the real client | Fabric 26.3 client joining Paper 26.3 headless |

## Built but not yet verified end to end

These need Chromium, which could not be downloaded in the environment used so far:

- rendering screens and HUD layers;
- the shared HUD overlay;
- `window.spectra` messaging;
- the `fetch` and WebSocket tunnel from inside a page;
- network allowlist enforcement;
- the `/_spectra/game/*` endpoints.

## Open decisions

- **Browser backend.** The client currently depends on Rinku 3.0.5 (Chromium 151). The plan is a Spectra fork combining:
  - Rinku's Minecraft integration,
  - CCBlueX's CEF 152 with GPU shared textures,
  - self-hosted native builds.

  No fork ships proprietary codecs (H.264, AAC).
- **API publishing.** The Java API (`com.mathhulk.spectra:api`) is not on a Maven repository yet.
- **Next platforms.** Velocity (proxy), Minestom, NeoForge client.
- **A versioned script API.** The `spectra` global is a first slice. A curated, versioned API, with raw Java access as an escape hatch, is planned. Luau support comes after it.
- **World generation performance.** Calls into one script are serialized, and in interpreter mode biome lookups (thousands per chunk) are slow. Options are GraalVM, caching, or a dedicated generator context.
