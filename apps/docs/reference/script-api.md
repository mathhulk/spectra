# Script API reference

Globals available to every script, in addition to the [web APIs](/scripting/web-apis) and [Node builtins](/scripting/node).

## Events

```ts
addEventListener<T>(event: JavaClass<T>, handler: (event: T) => void, priority?: EventPriority, ignoreCancelled?: boolean): Cancellable
addEventListener("fetch", handler: (event: SpectraFetchEvent) => void): Cancellable
addEventListener("websocket", handler: (event: SpectraWebSocketEvent) => void): Cancellable
removeEventListener(listener: Cancellable): void
removeEventListener(type: "fetch" | "websocket", handler): void
```

- `SpectraFetchEvent`: `request: Request`, `player`, `viewId`, `respondWith(Response | Promise<Response>)`
- `SpectraWebSocketEvent`: `request`, `player`, `viewId`, `accept(): SpectraViewSocket`
- `SpectraViewSocket`: an `EventTarget` with `message` and `close` events, `send(data)` and `close(code?, reason?)`

## Commands

```ts
addCommand(nameOrOptions: string | CommandOptions, handler: CommandHandler, tabCompleter?: TabCompleter): Command
removeCommand(command: Command): void
```

`CommandOptions` has these fields: `name`, `aliases`, `description`, `usage` and `permission`.

## `spectra`

| Member | Description |
|---|---|
| `engine(): string` | Script engine and mode, e.g. `GraalJS 25.4 (Interpreted)` |
| `api(): Spectra` | The underlying [Java API](/java/) |
| `client(player): ClientInfo` | Current client status. See [Client capabilities](/ui/capabilities) |
| `whenClientResolved(player, callback)` | Calls back once the status is known |
| `onClientResolved(callback): Cancellable` | For every player as they resolve |
| `openScreen(player, viewId, resource): OpenResult` | See [Opening views](/ui/views) |
| `openLayer(player, viewId, resource, options?): OpenResult` | `options`: `anchor`, `x`, `y`, `width`, `height`, `zIndex`, `visible`, `after` |
| `setViewVisible(player, viewId, visible): boolean` | |
| `closeView(player, viewId): boolean` | |
| `sendView(player, viewId, json): boolean` | `""` sends to all of the player's views |
| `onViewMessage(handler(player, viewId, json)): Cancellable` | See [Messaging](/ui/messaging) |
| `onViewEvent(handler(player, viewId, type, detail)): Cancellable` | `OPENED`, `CLOSED`, `FAILED` |

## Other globals

| Global | Description |
|---|---|
| `plugin` | The Spectra plugin (a Bukkit `Plugin`) |
| `server` | The Bukkit `Server` |
| `Java.type(name)` and package globals | See [Java interop](/scripting/java) |

## Page API (`window.spectra`, in views)

| Member | Description |
|---|---|
| `send(data): Promise<void>` | Send JSON to the server |
| `onMessage(listener): () => void` | Receive server messages. They also arrive as `spectra:message` window events |
| `game.player(): Promise<Player>` | See [Game data](/ui/game-data) |
| `game.mapUrl(options): string` | |
