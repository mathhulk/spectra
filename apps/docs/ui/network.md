# Network access

By default a view can only reach its own origin: its resources, the [API tunnel](/ui/http-and-websockets), and the [game data endpoints](/ui/game-data). That protects players. A server's page can't track their IP address or contact arbitrary hosts.

Servers that run web services views should reach directly (map tiles, a leaderboard API, a CDN) list them in `config.yml`:

```yaml
client:
  network-allow:
    - "https://api.example.com"
    - "https://*.tiles.example.com"
```

- **Scheme.** Only `https` origins (and `wss` for WebSockets) are allowed.
- **Port.** It must match: 443 unless given.
- **Wildcards.** `*.` matches one or more subdomain labels, but not the bare domain.
- **What it covers:** everything a page loads, including `fetch`, images, scripts, styles and WebSockets.

The list is sent to clients during the handshake (capability `network`).

## Players stay in control

Players can turn direct network access off in `config/spectra.json`:

```json
{ "allowServerNetwork": false }
```

Views keep working, and requests to allowlisted origins simply fail.

## Navigation and links

- **Views can't navigate away from their origin.**
- **Clicked `https` links** open in the player's browser after Minecraft's standard "open this link?" confirmation.
- **Popups, and navigation from scripts,** are blocked.
