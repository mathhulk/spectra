# Client capabilities

Players without the Spectra client mod can still join a Spectra server. Every plugin and script decides for itself what to do about it: require the mod, offer a fallback, or do nothing.

## Checking a player

```ts
addEventListener(org.bukkit.event.player.PlayerJoinEvent, (event) => {
  const player = event.getPlayer();
  spectra.whenClientResolved(player, (client) => {
    if (client.has("ui.screens")) {
      spectra.openScreen(player, "welcome", "welcome/index.html");
    } else {
      player.sendMessage("Install the Spectra client mod for the full experience.");
    }
  });
});
```

- **`spectra.client(player)`** returns what is known *right now*. Its status is `PENDING` until the handshake finishes.
- **`spectra.whenClientResolved(player, callback)`** calls back once the status is known: immediately if it already is, otherwise after the handshake or the timeout (`client.handshake-timeout-ticks`, 5 s by default).
- **`spectra.onClientResolved(callback)`** fires for every player as their status becomes known.

## Statuses

| Status | Meaning |
|---|---|
| `PENDING` | Joined recently; waiting for the handshake |
| `SUPPORTED` | Compatible Spectra client |
| `UNSUPPORTED` | No handshake before the timeout: a vanilla or unrelated client |
| `INCOMPATIBLE` | Has the mod, but with a protocol version this server doesn't support |

## Capabilities

`client.has(capability)` is true only for supported clients that advertise the capability.

| Capability | Meaning |
|---|---|
| `resources` | Can sync UI resources |
| `ui.screens` | Can show screens |
| `ui.layers` | Can show HUD layers |
| `ui.messages` | Supports `window.spectra.send` and server messages |
| `http` | Tunnels `fetch('/api/...')` |
| `sockets` | Tunnels `new WebSocket('/api/...')` |
| `network` | Honors the server's [network policy](/ui/network) |

A client advertises the UI capabilities only if its browser engine is available.

## Enforcing the mod

**Per feature** (recommended): check and react in your own code, for example kick the player only when they enter a minigame that needs the UI:

```ts
spectra.whenClientResolved(player, (client) => {
  if (!client.has("ui.layers")) player.kick(net.kyori.adventure.text.Component.text("This minigame needs the Spectra mod."));
});
```

**Server-wide:** set `client.require: true` in `config.yml` to kick anyone without a compatible client. Optionally also set `client.min-protocol-version`. This is off by default.

## Fallbacks

Opening a view returns `"UNSUPPORTED"` when the player can't show it, so you can fall back right away, for example to chat or to Paper's vanilla dialogs:

```ts
if (spectra.openScreen(player, "shop", "shop/index.html") === "UNSUPPORTED") {
  player.sendMessage("Shop: /buy <item>");
}
```
