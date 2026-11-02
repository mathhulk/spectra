# Views

```java
Views views = Spectra.get().views();

views.open(uuid, "shop:main", ViewSpec.screen("shop/index.html"));          // SENT, QUEUED or UNSUPPORTED
views.open(uuid, "shop:hud", new ViewSpec(ViewKind.LAYER, "shop/hud.html",
    new Layout(Anchor.TOP_RIGHT, 4, 4, 120, 40), 0, true, Optional.of("scoreboard")));

views.setVisible(uuid, "shop:hud", false);
views.send(uuid, "shop:main", "{\"coins\":42}");   // "" as the view id sends to all of the player's views
views.close(uuid, "shop:main");

Map<String, ViewSpec> open = views.open(uuid);      // views open or queued for the player
boolean canShow = views.supports(uuid, ViewKind.SCREEN);

views.onMessage(message -> { /* message.player(), viewId(), json(): untrusted */ });
views.onEvent(event -> { /* OPENED, CLOSED, FAILED */ });
```

The semantics (queueing until resources sync, replacing views, HUD slots) are the same as for scripts. See [Opening views](/ui/views).

Prefix view ids with your plugin name, since ids are shared by every plugin and script.
