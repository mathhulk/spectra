# Spectra protocol

The Spectra protocol is how a server adapter (Paper, Velocity, ...) talks to the Spectra client mod (Fabric,
NeoForge, ...). The Java implementation in this package is the source of truth. Every adapter and every client
depends on it, so the two sides can never drift apart.

## Transport

- A single plugin-message channel, **`spectra:main`**, is used in both directions.
- Every message is one *frame*: `VarInt packetId` followed by the packet body.
- Frame size limits come from vanilla Minecraft:
  - server → client: 1 MiB (`Limits.MAX_CLIENTBOUND_FRAME`)
  - client → server: 32 KiB (`Limits.MAX_SERVERBOUND_FRAME`)

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
- The client sends `Hello` only once the server has registered `spectra:main` (via `minecraft:register`), so vanilla servers never receive Spectra packets.
- Before the handshake completes, the server ignores every packet except `Hello`.
- If both sides support `network`, the server follows `Welcome` with `NetworkPolicy` (see below).

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

## HTTP tunnel

Views reach the server as if it were a web backend. A `fetch` to the view's own origin under `/api/` (for example `fetch('/api/shop/buy', { method: 'POST', body })`) never touches the network. The client tunnels it over the protocol:

```
client                                             server
  | -- HttpRequest(id, viewId, method, path, -->     |  path includes the query, e.g. /api/shop/buy?x=1
  |                headers, body, last)              |
  | -- HttpRequestBody(id, data, last) ------->      |  while the body continues
  | <-- HttpResponse(id, status, headers, -----      |
  |                  body, last)                     |
  | <-- HttpResponseBody(id, data, last) ------      |  while the body continues
```

- **Request ids** are chosen by the client and must be unique among its in-flight requests. The server allows 32 requests in flight per player and answers more with 429.
- **Routing.** The server routes on the path without `/api`, longest registered prefix first. A handler may decline, which passes the request to the next match. Anything unmatched gets 404.
- **Origin check.** Requests are only routed for views the server actually opened for that player; anything else gets 404.
- **Size limits.**
  - Request bodies are capped at 8 MiB and sent in 16 KiB pieces.
  - Response bodies are capped at 32 MiB and sent in 256 KiB pieces.
- **Timeout.** A handler that does not answer within 30 seconds produces a 504.

## WebSocket tunnel

`new WebSocket('/api/chat')` from a view (any URL on the view's own origin under `/api/`) is tunneled the same way:

```
client                                       server
  | -- SocketOpen(id, viewId, path, protocols) -> |
  | <-- SocketOpened(id, protocol) -------------- |  or SocketClosed(id, 4404, ...) if nothing handles it
  | -- SocketSend(id, binary, data, fin) -------> |  messages, fragmented; fin ends a message
  | <-- SocketMessage(id, binary, data, fin) ---- |
  | -- SocketClose(id, code, reason) -----------> |  either side may close
  | <-- SocketClosed(id, code, reason) ---------- |
```

- **Limits.** Messages are capped at 4 MiB. Each player may hold 32 sockets.
- **Cleanup.** Closing a view closes its sockets. When a player leaves, the server closes their sockets with code 1001.

## Network policy

`NetworkPolicy(allowedOrigins)` lists internet origins the server's views may contact directly, so that, for example, a minimap can load tiles from the server's own map service.

- Origins look like `https://api.example.com` or `https://*.tiles.example.com`. Only https/wss are allowed, and the port must match (443 when none is given).
- A wildcard matches one or more subdomain labels, never the bare domain.
- Without a policy, views can only reach their own origin.
- Clients may let players disable direct network access entirely.
