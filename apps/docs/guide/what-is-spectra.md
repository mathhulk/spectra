# What is Spectra?

Spectra is two things that work together:

1. **A scripting runtime for Minecraft: Java Edition servers.** It lets you write gameplay in JavaScript or TypeScript instead of Java. Scripts run inside the server on [GraalJS](https://www.graalvm.org/latest/reference-manual/js/) and call the server's Java API (Paper, and later others) directly. They reload as soon as you save.
2. **A system for server-driven UIs.** A client mod renders HTML/CSS/JS sent by the server: full-screen *screens* and *HUD layers*. Servers can ship and update interfaces while players are connected, much like FiveM's NUI.

```ts
addEventListener(org.bukkit.event.player.PlayerJoinEvent, (event) => {
  const player = event.getPlayer();

  spectra.whenClientResolved(player, (client) => {
    if (client.has("ui.screens")) {
      spectra.openScreen(player, "welcome", "welcome/index.html");
    } else {
      player.sendMessage("Welcome! Install the Spectra client mod for the full experience.");
    }
  });
});
```

## Why

Java plugins give the best performance and the deepest integration, but every change needs a compile and a restart, and the language has a learning curve. Many experiences don't need that: minigames, events, tools and server-specific mechanics. Spectra trades some raw performance for a much faster feedback loop and a stack millions of developers already know.

The same idea applies to UIs. Vanilla Minecraft has very limited server-defined UI, while the web platform has everything needed to build rich interfaces. Spectra lets servers use it.

## Design principles

- **Feel native to JavaScript developers.**
  - Scripts are ES modules.
  - Timers take milliseconds.
  - `fetch`, `URL`, `EventTarget` and `WebSocket` exist.
  - `node:path` or `fs` imports work, so code written for the web, Node, Bun or Deno mostly runs as-is. See [Web APIs](/scripting/web-apis) and [Node.js compatibility](/scripting/node).
- **Feel native to web developers, inside views too.** A view calls its server with `fetch('/api/...')` and `new WebSocket('/api/...')`, as if the Minecraft server were a web backend. See [HTTP and WebSockets from views](/ui/http-and-websockets).
- **Types from reality.** Types aren't hand-written for one Minecraft version. The server generates them from the exact classes it runs, other plugins included. See [TypeScript types](/scripting/types).
- **Opt-in enforcement.** Players without the client mod can still join. Each plugin or script decides whether to require it, offer a fallback, or ignore it. See [Client capabilities](/ui/capabilities).
- **A platform, not just a plugin.** Everything Spectra's scripts use is also a [public Java API](/java/) that other plugins can depend on.

## What Spectra is not

Spectra doesn't replace Java plugins. If you need maximum performance at large scale, or very deep integration with server internals, write Java. You can still use the [Spectra Java API](/java/) for UIs.

## How it fits together

See [Architecture](/guide/architecture) for the modules, the protocol, and the threading model.
