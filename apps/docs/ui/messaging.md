# Messaging

Messages are small JSON payloads between a view and the server, good for events and state updates. For request and response patterns, prefer [HTTP](/ui/http-and-websockets).

## From the page

Every Spectra page has `window.spectra`, defined before your scripts run:

```js
// Send any JSON-serializable value to the server. Resolves once delivered to the client.
await window.spectra.send({ type: "buy", item: "sword" });

// Receive messages from the server.
const stop = window.spectra.onMessage((data) => render(data));

// The same messages also arrive as a DOM event.
window.addEventListener("spectra:message", (event) => render(event.data));
```

## On the server

```ts
spectra.onViewMessage((player, viewId, json) => {
  const message = JSON.parse(json); // untrusted: validate it
  if (viewId === "shop" && message.type === "buy") {
    // ...
    spectra.sendView(player, "shop", JSON.stringify({ type: "bought", item: message.item }));
  }
});

// An empty view id sends to every view the player has open.
spectra.sendView(player, "", JSON.stringify({ type: "coins", value: 42 }));
```

## Limits

- Messages from a view are capped at 30 KiB, and messages to a view at 512 KiB.
- Only views the server opened can send messages, and only while they are open.
- Treat everything a view sends as player input: never trust it.
