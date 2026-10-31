# Spectra

Spectra lets you write Minecraft: Java Edition server gameplay in TypeScript or JavaScript and ship web-based
UIs (screens and HUD layers) to players on the fly.

## Layout

| Path | What it is |
|---|---|
| `packages/protocol` | Wire protocol shared by every server and client. Has no dependencies. See [PROTOCOL.md](packages/protocol/PROTOCOL.md). |
| `packages/core` | Platform-independent server logic: client handshakes and capabilities, resource sync, views, and the script lifecycle with hot reload. |
| `packages/runtime-js` | GraalJS runtime: a shared engine, one ES-module context per script, web-style timers. |
| `apps/paper` | Paper 26.3 plugin (the primary server target). |
| `apps/fabric` | Fabric 26.3 client mod that renders the UIs. See [its README](apps/fabric/README.md). |
| `apps/spectra-sdk`, `apps/create-spectra` | npm SDK and project templates: a dev server, esbuild watch, and typings. |
| `apps/docs` | Documentation site. |

## Building

Java modules need nothing installed beyond a JDK to launch Gradle. Gradle provisions Java 25 automatically.

```bash
./gradlew build
```

```bash
npm ci && npm run build:js
```

The Paper plugin is written to `apps/paper/build/libs/spectra-paper-<version>.jar`, and the client mod to `apps/fabric/build/libs/`.

## Scripts and performance

Scripts live in `<server>/scripts` and UI resources in `plugins/Spectra/resources`. Both reload when their files change.

GraalJS runs scripts in interpreter mode on a standard JDK. For compiled performance, run the server on GraalVM. Spectra logs which mode is active at startup.
