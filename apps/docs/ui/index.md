# Server-driven UI

Spectra lets a server show web-based interfaces to players with the [client mod](/ui/client) installed:

- **Screens:** full-screen, focused UIs such as shops, menus, dialogs and editors. Escape closes them.
- **HUD layers:** overlays drawn as part of the HUD, such as scoreboards, minimaps, objectives and timers. They don't take input.

Both are called **views**. A view is an HTML page from the server's [resources](/ui/resources), identified by a view id the server chooses.

```ts
spectra.openScreen(player, "shop", "shop/index.html");
spectra.openLayer(player, "minimap", "hud/minimap.html", { anchor: "top_right", width: 96, height: 96 });
```

## How it works

1. **Handshake.** When a player with the mod joins, client and server agree on a protocol version and [capabilities](/ui/capabilities).
2. **Resource sync.** The server's resources sync to the client: only files the client doesn't have yet, verified by SHA-256 and cached across sessions.
3. **Opening.** Views open on the client in an embedded Chromium.
4. **Serving pages.** Each server gets its own private origin, `https://<hash>.spectra.invalid/`. Relative URLs, ES modules, `fetch` and `localStorage` behave as on any website, but nothing touches the network.
5. **Talking to the server.** Views use [messages](/ui/messaging), or [HTTP and WebSockets](/ui/http-and-websockets) over the Spectra connection.

## In this section

- [Client capabilities](/ui/capabilities): who can see UIs, and whether to require the mod
- [Opening views](/ui/views): screens, layers, layout and HUD slots
- [Resources](/ui/resources): where UI files live, and plugin namespaces
- [Messaging](/ui/messaging): `window.spectra.send` and `onMessage`
- [HTTP and WebSockets from views](/ui/http-and-websockets)
- [Network access](/ui/network): contacting the internet from views
- [Game data and minimaps](/ui/game-data)
- [Client mod](/ui/client): installation, settings, browser engine
- [Security model](/ui/security)
