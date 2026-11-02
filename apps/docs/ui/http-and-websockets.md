# HTTP and WebSockets from views

Views can treat the Minecraft server as their web backend. `fetch` and `WebSocket` to the view's own origin under `/api/` go to the server over the Spectra connection. They need no web server, open port or certificate, and they work behind any proxy the player is connected through.

## In the page

```js
const response = await fetch("/api/shop/items?category=weapons");
const items = await response.json();

await fetch("/api/shop/buy", {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ item: "sword" }),
});

const socket = new WebSocket("/api/chat"); // or `wss://${location.host}/api/chat`
socket.onmessage = (event) => console.log(event.data);
socket.onopen = () => socket.send("hello");
```

WebSockets to other URLs are normal browser WebSockets, subject to the [network policy](/ui/network).

## In a script

Scripts answer with the same `addEventListener` they use for game events, in the style of a service worker:

```ts
addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (url.pathname === "/api/shop/items") {
    event.respondWith(Response.json(listItems(url.searchParams.get("category"))));
  }
  if (url.pathname === "/api/shop/buy" && event.request.method === "POST") {
    event.respondWith((async () => {
      const { item } = await event.request.json();
      return buy(event.player, item) ? new Response(null, { status: 204 }) : new Response("Not enough coins", { status: 402 });
    })());
  }
  // Not calling respondWith passes the request to the next listener, or to other plugins; 404 if nobody answers.
});

addEventListener("websocket", (event) => {
  if (new URL(event.request.url).pathname !== "/api/chat") return; // leave it for someone else
  const socket = event.accept();
  socket.addEventListener("message", (message) => socket.send(`${event.player.getName()}: ${message.data}`));
  socket.addEventListener("close", () => console.log("chat closed"));
});
```

On both events, `event.player` is the Bukkit `Player` and `event.viewId` is the view that made the request. Both are reliable: Spectra sets them, not the page.

## In a Java plugin

```java
Spectra spectra = Spectra.get();
spectra.http().route("/myplugin/", request ->
    CompletableFuture.completedFuture(HttpResponse.json(200, "{\"ok\":true}")));
spectra.sockets().route("/myplugin/live", socket -> {
  socket.onMessage(message -> socket.send("echo: " + message.text()));
  return true; // accepted; return false to let the next route try
});
```

Routes match on the path without `/api`, longest prefix first. See [Java API](/java/).

## Behavior and limits

- **Origin check.** Only views the server opened for that player can make requests.
- **Request bodies** are capped at 8 MiB and response bodies at 32 MiB. Both are chunked on the wire.
- **Timeout.** A handler has 30 s to answer, or the view receives `504`.
- **Per player:** 32 requests in flight and 32 open sockets. Socket messages are capped at 4 MiB.
- **Closing a view closes its sockets.** When a player leaves, the server sees close code `1001`.
- **Not yet supported:** cookies, redirects and streaming bodies. Bodies are buffered.
