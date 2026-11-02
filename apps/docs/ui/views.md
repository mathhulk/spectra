# Opening views

## Screens

```ts
spectra.openScreen(player, "shop", "shop/index.html"); // "SENT" | "QUEUED" | "UNSUPPORTED"
```

- **Opening a screen replaces the player's current screen.** If that was also a Spectra screen, it closes without a `CLOSED` event.
- **Escape closes the screen**, and the server receives a `CLOSED` [view event](#view-events).
- **Mouse, keyboard, scrolling and text input** go to the page.

## HUD layers

```ts
spectra.openLayer(player, "objectives", "hud/objectives.html", {
  anchor: "top_right", // where offsets are measured from
  x: 4, y: 4,          // offsets in GUI pixels, toward the screen's center
  width: 140,          // GUI pixels; 0 fills that axis
  height: 60,
  zIndex: 1,           // stacking order among layers in the same slot
  visible: true,
  after: "scoreboard", // render after a vanilla HUD element (optional)
});
```

Anchors: `top_left`, `top`, `top_right`, `left`, `center`, `right`, `bottom_left`, `bottom`, `bottom_right`.

**HUD slots.** `after` puts a layer right after a vanilla HUD element:

- `crosshair`, `hotbar`, `health`, `food`, `experience`
- `effects`, `boss_bar`, `scoreboard`, `title`
- `chat`, `player_list`, `subtitles`, `misc_overlays`

Without `after` (or with an unknown name), the layer renders on top of the HUD.

**Performance.** All layers in the same slot share **one transparent browser**, each layer an iframe inside it. Ten layers cost about as much as one. Layers in different slots use one browser per slot, so keep to a few slots.

## Updating and closing

```ts
spectra.setViewVisible(player, "objectives", false); // hide without destroying
spectra.closeView(player, "objectives");
```

Opening a view id that is already open replaces that view.

## Open results

| Result | Meaning |
|---|---|
| `SENT` | Sent to the client |
| `QUEUED` | The player's resources are still syncing; the view opens as soon as they finish |
| `UNSUPPORTED` | The player's client can't show this kind of view. See [Client capabilities](/ui/capabilities) |

Because of queueing, it's safe to open a view right when a player joins.

## View events

```ts
spectra.onViewEvent((player, viewId, type, detail) => {
  // type: "OPENED" (page loaded), "CLOSED" (player closed it), "FAILED" (load error; detail explains)
});
```

## View ids

View ids are chosen by the server, up to 128 bytes. Prefix them with your plugin or feature name (`shop:main`) to avoid clashes between scripts and plugins.
