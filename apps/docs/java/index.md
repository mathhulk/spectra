# Java API

Everything Spectra's scripts can do with clients and views is a public Java API. Other plugins can use it too, so they get server-driven UIs and the handshake without writing scripts. Spectra's own `spectra` script global is built on the same API.

## Depending on Spectra

```kotlin
// build.gradle.kts
dependencies {
    compileOnly("com.mathhulk.spectra:api:0.1.0-SNAPSHOT")
}
```

::: warning Not published yet
The API module (`packages/api`) isn't on a Maven repository yet. Until it is, build it from source with `./gradlew :api:publishToMavenLocal`. See [Project status](/guide/status).
:::

```yaml
# plugin.yml
depend: [Spectra]
```

```java
Spectra spectra = Spectra.get();
// Also available from Bukkit's services:
Spectra viaServices = Bukkit.getServicesManager().load(Spectra.class);
```

Call the API on the main thread. Callbacks run on the main thread too.

## Overview

| Part | What it does |
|---|---|
| `spectra.clients()` | Handshake state and capabilities per player. See [Clients](/java/clients) |
| `spectra.views()` | Open, update and close screens and layers; messages and events. See [Views](/java/views) |
| `spectra.http()` | Routes for `fetch('/api/...')` from views. See [HTTP and WebSockets](/java/http) |
| `spectra.sockets()` | Routes for `new WebSocket('/api/...')` from views |
| `spectra.resources()` | Ship your plugin's UI files under a namespace. See [Resources](/java/resources) |

Every `register`/`route`/`on*` method returns a `Registration`. Call `unregister()` (or use it with try-with-resources) to undo it, typically in `onDisable`.

## Example

```java
public final class ShopPlugin extends JavaPlugin implements Listener {
  private final List<Registration> registrations = new ArrayList<>();

  @Override
  public void onEnable() {
    Spectra spectra = Spectra.get();
    registrations.add(spectra.resources().register("shop", getDataFolder().toPath().resolve("ui")));
    registrations.add(spectra.http().route("/shop/", this::handle));
    getServer().getPluginManager().registerEvents(this, this);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    UUID player = event.getPlayer().getUniqueId();
    Spectra.get().clients().resolve(player).thenAccept(client -> {
      if (client.has(Capability.SCREENS)) {
        Spectra.get().views().open(player, "shop:main", ViewSpec.screen("shop/index.html"));
      }
    });
  }

  private CompletionStage<HttpResponse> handle(HttpRequest request) {
    return CompletableFuture.completedFuture(HttpResponse.json(200, "{\"items\":[]}"));
  }

  @Override
  public void onDisable() {
    registrations.forEach(Registration::unregister);
  }
}
```
