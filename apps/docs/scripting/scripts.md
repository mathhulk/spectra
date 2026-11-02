# Scripts and lifecycle

## Where scripts live

The `scripts/` folder in the server directory holds your scripts.

- **Every top-level `.js` or `.mjs` file is a script.** Scripts load in file-name order.
- **Subfolders hold shared modules and assets.** Scripts import them with relative imports.

With the SDK you write TypeScript in `src/`, and esbuild bundles it into `scripts/` for you.

## Modules and exports

Scripts are ES modules. Top-level code runs when the script loads. These exports are optional hooks:

| Export | When it is called |
|---|---|
| `onEnable()` | After the script loads, once the server is ready (and after each reload) |
| `onDisable()` | Before the script unloads or reloads, and on shutdown |
| `getDefaultWorldGenerator(worldName, id)` | When a world uses `generator: Spectra[:id]`. See [World generation](/scripting/world-generation) |
| `getDefaultBiomeProvider(worldName, id)` | Same, for biome providers |

```ts
export function onEnable() {
  console.log("enabled");
}

export function onDisable() {
  console.log("disabled");
}
```

`console.log` and friends go to the server log, prefixed with the script name.

## Globals

| Global | Description |
|---|---|
| `addEventListener`, `removeEventListener` | [Events](/scripting/events) (Java event classes, plus the `fetch` and `websocket` view events) |
| `addCommand`, `removeCommand` | [Commands](/scripting/commands) |
| `setTimeout`, `setInterval`, `clearTimeout`, `clearInterval`, `setImmediate`, `queueMicrotask` | [Timers](/scripting/timers) |
| `spectra` | Client capabilities and views. See [Script API reference](/reference/script-api) |
| `plugin`, `server` | The Spectra `Plugin` and the Bukkit `Server`, for APIs that need them |
| `fetch`, `URL`, `TextEncoder`, `EventTarget`, `WebSocket`, `crypto`, `process`, ... | [Web APIs](/scripting/web-apis) |
| Java package globals (`org`, `java`, `io`, ...) and `Java.type` | [Java interop](/scripting/java) |

## Hot reload

The plugin watches `scripts/` (configurable with `scripts.watch`).

- **Saving a script** reloads that script: `onDisable` runs, everything it registered is released, the file is evaluated again, and `onEnable` runs.
- **Changing a file in a subfolder** reloads every script, because any of them may import it.
- **Saves are debounced**, so a single save or rebuild produces one reload.

Reloads happen on the main thread.

## Automatic cleanup

Everything a script registers belongs to that script and is released when it unloads, in reverse order:

- event listeners;
- commands, including their aliases;
- timers;
- view listeners;
- HTTP and WebSocket handlers;
- outgoing WebSockets.

A reload can't leak registrations.

## Errors

- **A script that fails to load** is not registered. The error is logged with the script name, and other scripts still load.
- **An error thrown from a handler, timer or command** is logged and doesn't affect anything else.

## Managing scripts in-game

`/spectra` (permission `spectra.admin`):

| Command | Description |
|---|---|
| `/spectra info` | Version, script engine and mode, script folder |
| `/spectra scripts` | Loaded scripts and whether they are enabled |
| `/spectra reload [name]` | Reload one script, or all of them |
| `/spectra watch [on\|off]` | Toggle hot reload |
| `/spectra resources` | Rescan UI resources |
| `/spectra client <player>` | A player's client status and capabilities |
