# Web APIs

Every script starts with a web-platform layer, so code written for browsers or for Deno and Bun works without changes.

| API | Notes |
|---|---|
| `fetch`, `Request`, `Response`, `Headers` | HTTP and HTTPS through Java's HTTP client. Follows redirects. Supports `AbortSignal` and a non-standard `timeout` option (ms). Bodies are strings, `ArrayBuffer`s or typed arrays, and `URLSearchParams` |
| `URL`, `URLSearchParams` | `URL.canParse`, `URL.parse`. Search params stay in sync with the URL |
| `TextEncoder`, `TextDecoder` | UTF-8 |
| `atob`, `btoa` | |
| `EventTarget`, `Event`, `CustomEvent`, `MessageEvent`, `CloseEvent`, `ErrorEvent` | `once` and `signal` listener options |
| `AbortController`, `AbortSignal` | `AbortSignal.timeout`, `AbortSignal.any` |
| `WebSocket` | Outgoing `ws://` and `wss://` connections. For sockets from your views, see [HTTP and WebSockets from views](/ui/http-and-websockets) |
| `crypto` | `randomUUID`, `getRandomValues`, `subtle.digest` (SHA-1/256/384/512) |
| `performance` | `now()`, `timeOrigin` |
| `structuredClone`, `queueMicrotask` | |
| `setTimeout`, `setInterval`, `setImmediate` (and the `clear*` functions) | Milliseconds, run on the server thread. See [Timers](/scripting/timers) |
| `console` | Goes to the server log |
| `navigator` | `userAgent`, `language`, `hardwareConcurrency` |

```ts
const response = await fetch("https://api.example.com/leaderboard", {
  signal: AbortSignal.timeout(5000),
});
const leaderboard = await response.json();
```

Callbacks (`fetch` results, socket messages) are delivered on the server's main thread, so it's safe to touch the world from them.

## Not available

The DOM: `document`, `window`, layout, rendering. `localStorage`, `IndexedDB`, `Blob`/`File`, `FormData`, and streaming request or response bodies aren't implemented yet. Bodies are buffered.
