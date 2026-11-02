# Game data and minimaps

Some HUDs need data the client already has, such as the player's position or the terrain around them. Asking the server for it would add latency and load. Spectra exposes it to pages directly, on the view's origin:

| Endpoint | Returns |
|---|---|
| `/_spectra/game/player` | JSON: `x`, `y`, `z`, `yaw`, `pitch`, `dimension`, `health`, `maxHealth`, `food`, `time` |
| `/_spectra/game/map.png?radius=64` | A top-down PNG of loaded terrain around the player, in vanilla map colors with height shading. North is up, one pixel per block, and the player is at the center. Unloaded areas are transparent. `radius` ranges from 1 to 128 |

Both are also on `window.spectra.game`:

```js
const player = await window.spectra.game.player();
image.src = window.spectra.game.mapUrl({ radius: 64 }); // adds a cache-busting parameter
```

## A minimap layer

```html
<!doctype html>
<html>
  <body style="margin:0">
    <div style="position:relative;width:100%;height:100%;border-radius:50%;overflow:hidden">
      <img id="map" style="width:100%;height:100%;image-rendering:pixelated" />
      <div id="arrow" style="position:absolute;left:50%;top:50%;width:6px;height:6px;margin:-3px;background:white"></div>
    </div>
    <script type="module">
      const map = document.getElementById("map");
      const arrow = document.getElementById("arrow");
      async function refresh() {
        const player = await window.spectra.game.player();
        arrow.style.transform = `rotate(${player.yaw}deg)`;
        map.src = window.spectra.game.mapUrl({ radius: 48 });
      }
      setInterval(refresh, 500);
      refresh();
    </script>
  </body>
</html>
```

```ts
spectra.openLayer(player, "minimap", "hud/minimap.html", { anchor: "top_right", x: 4, y: 4, width: 80, height: 80 });
```

## Cost

The map is rendered on the client's main thread, and its cost grows with the square of the radius. Refresh it a few times per second at most, and keep the radius modest. Server-side data (waypoints, other players) can come through [messages](/ui/messaging) or [HTTP](/ui/http-and-websockets). Tiles from your own map service can come through [network access](/ui/network).
