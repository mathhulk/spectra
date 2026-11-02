# Getting started

## Requirements

- **Node.js** 20 or newer.
- **Java 25** to run the server. Minecraft 26.1 and newer require it.
- For server-driven UIs: **Minecraft 26.3** with Fabric, the Spectra client mod, and [Rinku](https://modrinth.com/mod/rinku) (the embedded browser).

## Create a project

```sh
npm create spectra@latest my-server
cd my-server
npm install
```

Set `eula: true` in `spectra.config.mjs` (after reading the [Minecraft EULA](https://aka.ms/MinecraftEULA)), then start developing:

```sh
npm run dev
```

`spectra-sdk dev` does the following:

1. Downloads the Paper server you configured (checksum-verified) and the Spectra plugin.
2. Starts the server.
3. Bundles `src/main.ts` with esbuild in watch mode, and links the output into the server's `scripts/` folder.
4. Copies the TypeScript types the server generated into `.spectra/types/` and keeps them updated. See [TypeScript types](/scripting/types).

Save a file and the script reloads in the running server.

## Project layout

```
my-server/
├─ src/main.ts               your script
├─ spectra.config.mjs        server type, version, Java options, build options
├─ tsconfig.json             DOM + Node types, generated Java types
├─ .spectra/types/           generated; do not edit or commit
└─ .minecraft/               the development server (scripts/, plugins/Spectra/, worlds)
```

## Your first script

```ts
export function onEnable() {
  console.log("Hello from Spectra!");
}

addCommand({ name: "hello", description: "Say hello" }, (sender) => {
  sender.sendMessage(`Hello, ${sender.getName()}!`);
});

addEventListener(org.bukkit.event.player.PlayerJoinEvent, (event) => {
  event.getPlayer().sendMessage("Welcome!");
});
```

## Next steps

- [Scripts and lifecycle](/scripting/scripts)
- [Events](/scripting/events), [Commands](/scripting/commands), [Timers](/scripting/timers)
- [Server-driven UI overview](/ui/)
