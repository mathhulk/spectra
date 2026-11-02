# Spectra client (Fabric)

The client mod renders server-driven web UIs for Minecraft 26.3. It handles:

- **Handshake.** It sends `Hello` only after the server registers `spectra:main`, so vanilla servers never see Spectra packets.
- **Resource sync.** Files go into a content-addressed, SHA-256-verified store at `spectra/objects/`, which every server shares.
- **Views.** It shows full-screen views and HUD layers. A layer can be placed after a vanilla HUD element, for example `after: "chat"`.
- **Pages.** Pages are served from a per-server `https://<hash>.spectra.invalid/` origin.
  - Navigation and network requests outside that origin are blocked.
  - External links open only after vanilla's link confirmation.
  - Pages talk to the server with `window.spectra.send(data)` and `window.spectra.onMessage(listener)`.

## Browser backend

The client currently uses [Rinku](https://modrinth.com/mod/rinku) 3.0.5 (Chromium 151, uncapped frame rate) from the Modrinth Maven repository. The plan is to replace it with Spectra's own fork, which combines:

- Rinku's Minecraft integration (Fabric and NeoForge, 26.x),
- CCBlueX's java-cef work (CEF 152 and GPU shared-texture "accelerated paint" on Windows, macOS and Linux),
- self-hosted, hash-pinned native builds.

No published fork ships proprietary codecs (H.264, AAC). Those require building CEF with `proprietary_codecs=true ffmpeg_branding=Chrome`.

### Known limitations until the fork lands

- **Shared request handler.** The request handler and message router are installed on Rinku's shared `CefClient`. Another mod that replaces the request handler would bypass Spectra's navigation lockdown.
- **One browser per layer.** Each HUD layer uses its own browser. A single shared overlay browser would be cheaper.
- **Natives from a third party.** Chromium natives are downloaded at first launch from Rinku's GitHub releases.

## Development

```bash
./gradlew :fabric:build
./gradlew :fabric:runClient --args="--quickPlayMultiplayer localhost:25565"
```
