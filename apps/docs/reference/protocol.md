# Protocol

The protocol between servers and the client mod is specified in [`packages/protocol/PROTOCOL.md`](https://github.com/mathhulk/spectra/blob/nui-next/packages/protocol/PROTOCOL.md). The Java implementation in `packages/protocol` is the source of truth, and every server adapter and client depends on it.

## Summary

- **Channel.** A single plugin-message channel, `spectra:main`, carries one packet per frame.
- **Versioning.** The current protocol version is 2. Unknown packets are errors, and incompatible changes bump the version.
- **Handshake.** The client sends `Hello` (version, capabilities) only after the server registers the channel. The server answers with `Welcome` (accepted, server id, capabilities) and, if supported, `NetworkPolicy`. The protocol enforces nothing.
- **Resource sync.** The server sends a manifest of paths, hashes and sizes. The client requests what it's missing, the server sends it in chunks, and the client reports `ResourcesSynced`.
- **Views.** `OpenView`, `SetViewVisible`, `CloseView`, `ViewMessage`, plus `ViewPost` and `ViewEvent` from the client.
- **HTTP tunnel.** `HttpRequest`/`HttpRequestBody` from the client, and `HttpResponse`/`HttpResponseBody` from the server.
- **WebSocket tunnel.** `SocketOpen`, `SocketSend`, `SocketClose` from the client, and `SocketOpened`, `SocketMessage`, `SocketClosed` from the server.

## Limits

| Limit | Value |
|---|---|
| Frame, server to client / client to server | 1 MiB / 32 KiB (vanilla limits) |
| View id | 128 bytes |
| View message, to view / from view | 512 KiB / 30 KiB |
| Resource chunk | 256 KiB max (Spectra sends 128 KiB) |
| HTTP request / response body | 8 MiB / 32 MiB |
| WebSocket message | 4 MiB |
| In flight per player | 32 requests, 32 sockets |
