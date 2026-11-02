# HTTP and WebSockets

## HTTP routes

```java
Registration route = Spectra.get().http().route("/shop/", request -> {
  // request.player(), viewId(), method(), path() ("/api/shop/buy?x=1"), headers(), body(), text()
  if (!request.pathWithoutQuery().equals("/api/shop/buy")) {
    return null; // decline: the next matching route gets it (404 if none)
  }
  return CompletableFuture.supplyAsync(() -> loadPrices())   // any thread is fine
      .thenApply(prices -> HttpResponse.json(200, prices));
});
```

- **Matching.** Prefixes match the path without `/api`, longest first, then in registration order.
- **Threads.** Handlers are called on the main thread. The returned stage may complete on any thread.
- **Response helpers:** `HttpResponse.json`, `text`, `of(status, contentType, bytes)` and `notFound`.
- **Timeout.** After 30 s the view receives `504`.

## WebSocket routes

```java
Spectra.get().sockets().route("/shop/live", socket -> {
  socket.onMessage(message -> socket.send("got " + (message.binary() ? message.data().length + " bytes" : message.text())));
  socket.onClose(close -> getLogger().info("closed " + close.code()));
  return true; // false declines; unaccepted sockets are closed with 4404
});
```

A `WebSocket` gives you:

- `player()`, `viewId()`, `path()`, `isOpen()`
- `send(String)` and `send(byte[])`
- `close(code, reason)`
- `onMessage` and `onClose`

See [HTTP and WebSockets from views](/ui/http-and-websockets) for limits and the page side.
