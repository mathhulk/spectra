# Spectra protocol

The Spectra protocol is how a server adapter (Paper, Velocity, ...) talks to the Spectra client mod (Fabric,
NeoForge, ...). The Java implementation in this package is the source of truth. Every adapter and every client
depends on it, so the two sides can never drift apart.

## Transport

- A single plugin-message channel, **`spectra:main`**, is used in both directions.
- Every message is one *frame*: `VarInt packetId` followed by the packet body.
- Frame size limits come from vanilla Minecraft:
  - server → client: 1 MiB (`Frames.MAX_CLIENTBOUND_SIZE`)
  - client → server: 32 KiB (`Frames.MAX_SERVERBOUND_SIZE`)

  Packets that could exceed a limit are split by the sender (manifests, chunks, requests).
- Unknown packet IDs are a protocol error. A newer version must bump `Protocol.VERSION`.

### Primitive encodings

| Type | Encoding |
|---|---|
| `varint` | Minecraft-style LEB128, max 5 bytes |
| `varlong` | Minecraft-style LEB128, max 10 bytes |
| `bool` | 1 byte, `0` or `1` |
| `string` | `varint` UTF-8 byte length + bytes (each string field has its own max length) |
| `bytes` | `varint` length + bytes |
| `hash` | 32 raw bytes (SHA-256) |
| `list<T>` | `varint` count + elements (each list field has its own max count) |
| `optional<T>` | `bool` present + value |
| `enum` | `varint` ordinal |

## Handshake and capabilities

```
client                                   server
  | -- Hello(version, mod, capabilities) --> |
  | <-- Welcome(accepted, reason, serverId,  |
  |             capabilities)                |
```

- The client sends `Hello` as soon as it joins a server. Vanilla servers ignore the unknown channel.
- The server answers with `Welcome`.
  - If it does not support the client's `version`, `accepted` is `false`, `reason` explains why, and nothing else is sent.
- `serverId` is a stable, opaque identifier the server generates once and keeps.
  - Clients use it to name their per-server resource cache.
  - Clients never use the server's network address, because addresses aren't stable and aren't safe as folder names.
- Capabilities are plain strings (see `Capability`). Each side advertises what it supports, and features are only used when both sides list them.
- **Nothing is enforced by the protocol.** Server code decides whether a player without the client mod is kicked, gets a fallback, or simply sees nothing.
- A player whose client never sends `Hello` is reported as *unsupported* after a configurable timeout.

## Resource sync

Resources (HTML, JS, CSS, images, fonts, ...) are content-addressed by SHA-256.

```
server                                         client
  | -- ResourceManifest(part, parts, entries) --> |  entries = (path, hash, size)
  | <-- ResourceRequest(hashes) ------------------ |  only hashes the client does not have
  | -- ResourceChunk(hash, index, count, data) --> |  repeated
  | <-- ResourcesSynced(generation) -------------- |  once every requested hash is complete
```

- **Manifest.** Large manifests are split across several `ResourceManifest` packets. `generation` changes every time the server's resources change.
  - The client must not act on a generation until it has received all `parts` of it.
  - A newer generation replaces an older one, so the server can push updates at any time, for example on hot reload.
- **Requests.** The client asks only for hashes it doesn't already have, using as many `ResourceRequest` packets as needed to stay under the serverbound frame limit.
  - The server ignores hashes that aren't in its current manifest.
  - The server rate-limits requests per player.
- **Chunks.** Chunks are reassembled by `index`, not by arrival order.
  - The client checks the assembled bytes against the SHA-256 hash before storing them, and discards the data on a mismatch.
- **Paths.** They are relative, use `/` as the separator, and must not contain `..`, a leading `/`, a drive letter or a backslash. Both sides validate this (`ResourcePath`).
- **Completion.** `ResourcesSynced` tells the server the client can render that generation. Server code may queue view requests until it arrives.

## Views

A *view* is one piece of server-defined UI, identified by a server-chosen `viewId` string.

- `SCREEN`: a full-screen, focused UI. It replaces the current screen, and closing it returns to the game.
- `LAYER`: a HUD overlay that does not take input focus. The client may render all layers inside one shared browser.

```
server                                   client
  | -- OpenView(viewId, kind, resource,  --> |
  |             layout, zIndex, visible)     |
  | -- SetViewVisible(viewId, visible) ----> |
  | -- CloseView(viewId) ------------------> |
  | -- ViewMessage(viewId?, json) ---------> |  viewId empty = every open view
  | <-- ViewPost(viewId, json) ------------- |  from window.spectra.send(...)
  | <-- ViewEvent(viewId, type, detail) ---- |  OPENED, CLOSED, FAILED
```

- **Opening an existing `viewId`** replaces that view.
- **`layout`:**
  - `anchor`: one of nine positions.
  - `x`/`y` offsets and `width`/`height`, in GUI-scaled pixels.
  - Width or height `0` fills the screen on that axis.
- **`after`** is optional. It names the vanilla HUD element the layer should render after (for example `chat` or `hotbar`). If it's missing, the layer renders on top.
- **Messages** are JSON text. Each side checks the JSON size and must treat the other side's data as untrusted.
- **`ViewEvent(CLOSED)`** is sent whenever the player closes a screen themselves, for example with Escape, so server state stays accurate.
