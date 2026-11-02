package com.mathhulk.spectra.api;

import com.mathhulk.spectra.api.clients.Clients;
import com.mathhulk.spectra.api.http.HttpRoutes;
import com.mathhulk.spectra.api.http.SocketRoutes;
import com.mathhulk.spectra.api.resources.Resources;
import com.mathhulk.spectra.api.views.Views;

import java.util.Objects;

/**
 * Entry point for plugins that build on Spectra: client capabilities, server-driven views, HTTP and WebSocket
 * endpoints that views reach over the Spectra protocol, and UI resources.
 *
 * <p>Unless documented otherwise, methods must be called on the server's main thread and callbacks run on it.
 *
 * <pre>{@code
 * Spectra spectra = Spectra.get();
 * spectra.clients().resolve(player.getUniqueId()).thenAccept(info -> {
 *   if (info.has(Capability.SCREENS)) {
 *     spectra.views().open(player.getUniqueId(), "shop", ViewSpec.screen("myplugin/shop/index.html"));
 *   }
 * });
 * spectra.http().route("/myplugin/", request -> CompletableFuture.completedFuture(HttpResponse.json(200, "{}")));
 * }</pre>
 */
public interface Spectra {
  /** Version of this API. Follows semantic versioning. */
  String API_VERSION = "0.1.0";

  Clients clients();

  Views views();

  HttpRoutes http();

  SocketRoutes sockets();

  Resources resources();

  /** The running Spectra instance. Throws if Spectra is not enabled yet. */
  static Spectra get() {
    Spectra instance = Holder.instance;
    if (instance == null) {
      throw new IllegalStateException("Spectra is not enabled; declare `depend: [Spectra]` in your plugin.yml");
    }
    return instance;
  }

  /** For the platform adapter only. */
  final class Holder {
    private static volatile Spectra instance;

    private Holder() {
    }

    public static void set(Spectra spectra) {
      instance = spectra;
    }

    public static void clear(Spectra spectra) {
      if (Objects.equals(instance, spectra)) {
        instance = null;
      }
    }
  }
}
