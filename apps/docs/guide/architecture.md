# Architecture

## Modules

| Module | Role |
|---|---|
| `packages/protocol` | The wire protocol between servers and clients. Has no dependencies, so every platform shares it. See [Protocol](/reference/protocol). |
| `packages/api` | The public Java API that other plugins compile against. See [Java API](/java/). |
| `packages/core` | Platform-independent server logic. It implements the API: handshake and capabilities, resource sync, views, the HTTP and WebSocket tunnels, and the script lifecycle with hot reload. |
| `packages/runtime-js` | GraalJS runtime: one shared engine, one ES-module context per script, plus the web and Node compatibility layer. |
| `packages/typegen` | Generates TypeScript declarations from Java classes. |
| `apps/paper` | The Paper 26.3 plugin, an adapter over core. |
| `apps/fabric` | The Fabric 26.3 client mod that renders views. |
| `apps/spectra-sdk`, `apps/create-spectra` | npm SDK (dev server, bundling, typings) and project templates. |

Platform adapters are deliberately thin, so adding Velocity, Minestom or NeoForge means writing an adapter, not reimplementing Spectra.

## Threading model

- **Each script has its own JavaScript context**, and contexts are single-threaded.
- **Every entry into a script goes through that script's lock.** That covers lifecycle hooks, timers, event handlers, commands, view requests and world generation.
- **Almost everything runs on the server's main thread.** Asynchronous events (such as chat) run on their own thread, like in Java plugins, and simply wait for the lock.
- **World generation runs on Paper's worker threads.** If the main thread is inside a script and waiting for a chunk that the same script generates, the worker hands its call to the main thread instead of deadlocking. See [World generation](/scripting/world-generation).

## Server and client

```
Paper plugin ───── spectra:main plugin channel ───── Fabric client mod
   │  handshake + capabilities                         │  embedded Chromium (Rinku)
   │  resource sync (SHA-256)                          │  screens + HUD overlay
   │  views, messages                                  │  https://<hash>.spectra.invalid/
   │  HTTP + WebSocket tunnels                         │  fetch('/api/…'), WebSocket('/api/…')
```

See [Server-driven UI](/ui/) and [Protocol](/reference/protocol).
