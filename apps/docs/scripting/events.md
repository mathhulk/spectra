# Events

## Minecraft events

Pass any Bukkit/Paper event class:

```ts
const listener = addEventListener(org.bukkit.event.player.PlayerJoinEvent, (event) => {
  event.getPlayer().sendMessage("Welcome!");
});

// Later, if you need to stop early (unloading the script does this automatically):
listener.cancel(); // or removeEventListener(listener)
```

Optional arguments are a priority and whether to skip cancelled events:

```ts
addEventListener(org.bukkit.event.block.BlockBreakEvent, (event) => {
  event.setCancelled(true);
}, "high", true);
```

Priorities, from first to run to last: `"lowest"`, `"low"`, `"normal"` (default), `"high"`, `"highest"`, `"monitor"`.

The handler's parameter is typed from the event class, using the [generated types](/scripting/types).

### Asynchronous events

Some events, such as chat, fire off the main thread. As in Java plugins, their handlers run on that thread. Don't touch world state from them; schedule onto the main thread with `setTimeout(fn, 0)` instead.

## View events

The same `addEventListener` also takes two string event types, for requests coming from [views](/ui/):

- `"fetch"`: a view called `fetch('/api/...')`. Answer with `event.respondWith(response)`.
- `"websocket"`: a view opened `new WebSocket('/api/...')`. Take it with `event.accept()`.

See [HTTP and WebSockets from views](/ui/http-and-websockets).
