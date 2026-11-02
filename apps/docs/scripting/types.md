# TypeScript types

Spectra doesn't ship hand-written types for one Minecraft version. **The server generates them from the classes it actually runs**: Paper's API, plus the public API of every other installed plugin.

## How it works

1. **At startup, after all plugins are enabled,** the Spectra plugin generates `<server>/.spectra/types/server.d.ts` off the main thread. On Paper 26.3 that's about 2,100 classes in about 3 seconds.
2. **Parameter names and Javadoc** come from an index the Spectra build extracts from Paper API's sources jar. Hover documentation and real parameter names work in your editor.
3. **`spectra-sdk dev` copies the file** into your project's `.spectra/types/` and updates it whenever the server regenerates it.

Turn generation off with `types.generate: false` in `config.yml`.

## What is declared

- **Every public class** as an interface: instance methods, overloads and generics, with fields as properties.
- **A static side** (`const Material`) with constructors, static methods and enum constants.
- **Supertypes**, so `Player` has everything from `HumanEntity`, `Entity`, and so on.
- **JDK types** that are referenced directly (`java.util.List`, `UUID`, `CompletableFuture`, ...). Other types fall back to `any`.
- **`JavaClass<T>`.** Passing `org.bukkit.event.player.PlayerJoinEvent` to `addEventListener` types the handler's `event`.

Spectra's own globals (`spectra`, the `fetch`/`websocket` events, `addCommand`, ...) come from `spectra-sdk/types`, which is hand-written and documented.

## Your tsconfig

The template sets this up:

```json
{
  "compilerOptions": {
    "lib": ["ESNext", "DOM"],
    "types": ["node"],
    "skipLibCheck": true
  },
  "include": ["src", ".spectra/**/*.d.ts"]
}
```

- **`DOM` and `@types/node`** describe the [web](/scripting/web-apis) and [Node](/scripting/node) APIs Spectra provides.
- **`skipLibCheck`** is needed because Java allows things TypeScript can't express exactly, such as covariant overrides. Your own code is still fully checked.
- **The `include` entry is explicit:** TypeScript skips folders starting with `.` unless they are listed.

## Generating types without a server

The Spectra build can also generate declarations for Paper API alone (`./gradlew :paper:generatePaperTypes`). The output is meant as a fallback the SDK can ship, but the SDK doesn't publish it yet.
