package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.core.SpectraServer;
import com.mathhulk.spectra.core.clients.ClientInfo;
import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import com.mathhulk.spectra.core.views.ViewService;
import com.mathhulk.spectra.core.views.ViewSpec;
import com.mathhulk.spectra.protocol.clientbound.Anchor;
import com.mathhulk.spectra.protocol.clientbound.Layout;
import com.mathhulk.spectra.protocol.clientbound.ViewKind;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The {@code spectra} global: client capability checks and server-driven views. Every method is meant to be
 * called from the main thread; callbacks are delivered on it, under the script's lock.
 *
 * <p>This is the first slice of the curated, versioned API. Raw Paper access stays available alongside it.
 */
public final class SpectraApi {
  /** Callback for {@link #onViewMessage}. */
  @FunctionalInterface
  public interface ViewMessageHandler {
    void handle(Player player, String viewId, String json);
  }

  /** Callback for {@link #onViewEvent}. */
  @FunctionalInterface
  public interface ViewEventHandler {
    void handle(Player player, String viewId, String type, String detail);
  }

  private final SpectraServer server;
  private final ScriptScope scope;
  private final String engine;

  SpectraApi(SpectraServer server, ScriptScope scope, String engine) {
    this.server = server;
    this.scope = scope;
    this.engine = engine;
  }

  /** Description of the script engine, e.g. "GraalJS 25.4 (Interpreted)". */
  public String engine() {
    return engine;
  }

  /** What is currently known about a player's client. Status is PENDING until the handshake resolves. */
  public ClientInfo client(Player player) {
    return server.clients().info(player.getUniqueId());
  }

  /**
   * Calls {@code callback} with the player's client info once known: immediately if it already is, otherwise
   * after the handshake or its timeout. Use this to enforce or offer custom UIs on demand.
   */
  public void whenClientResolved(Player player, Consumer<ClientInfo> callback) {
    server.clients().resolve(player.getUniqueId()).thenAccept(info -> server.platform().mainThread()
        .execute(() -> scope.invoke("whenClientResolved", () -> callback.accept(info))));
  }

  /** Fires for every player whose client status becomes known after this call. */
  public Cancellable onClientResolved(Consumer<ClientInfo> callback) {
    return scope.track(server.clients().onResolved(info -> scope.invoke("onClientResolved",
        () -> callback.accept(info))));
  }

  /** Opens a full-screen view. Returns SENT, QUEUED (until resources sync), or UNSUPPORTED. */
  public String openScreen(Player player, String viewId, String resource) {
    return server.views().open(player.getUniqueId(), viewId, ViewSpec.screen(resource)).name();
  }

  /**
   * Opens a HUD layer. {@code options} may contain anchor (e.g. "top_right"), x, y, width, height (GUI pixels, 0 =
   * fill), zIndex, visible, and after (a vanilla HUD element name).
   */
  public String openLayer(Player player, String viewId, String resource, Map<String, Object> options) {
    Map<String, Object> o = options == null ? Map.of() : options;
    Layout layout = new Layout(
        Anchor.valueOf(String.valueOf(o.getOrDefault("anchor", "top_left")).toUpperCase(Locale.ROOT)),
        integer(o.get("x")), integer(o.get("y")), integer(o.get("width")), integer(o.get("height")));
    ViewSpec spec = new ViewSpec(ViewKind.LAYER, resource, layout, integer(o.get("zIndex")),
        !Boolean.FALSE.equals(o.get("visible")),
        Optional.ofNullable(o.get("after")).map(String::valueOf));
    return server.views().open(player.getUniqueId(), viewId, spec).name();
  }

  public String openLayer(Player player, String viewId, String resource) {
    return openLayer(player, viewId, resource, Map.of());
  }

  public boolean setViewVisible(Player player, String viewId, boolean visible) {
    return server.views().setVisible(player.getUniqueId(), viewId, visible);
  }

  public boolean closeView(Player player, String viewId) {
    return server.views().close(player.getUniqueId(), viewId);
  }

  /** Sends a JSON string to one view, or to all of the player's views when {@code viewId} is "". */
  public boolean sendView(Player player, String viewId, String json) {
    return server.views().send(player.getUniqueId(), viewId, json);
  }

  /** Messages views send with {@code window.spectra.send(...)}. The JSON is untrusted player input. */
  public Cancellable onViewMessage(ViewMessageHandler handler) {
    return scope.track(server.views().onPost((ViewService.Post post) -> {
      Player player = Bukkit.getPlayer(post.player());
      if (player != null) {
        scope.invoke("onViewMessage", () -> handler.handle(player, post.viewId(), post.json()));
      }
    }));
  }

  /** View lifecycle events: OPENED, CLOSED (including the player pressing Escape), FAILED. */
  public Cancellable onViewEvent(ViewEventHandler handler) {
    return scope.track(server.views().onEvent((ViewService.Event event) -> {
      Player player = Bukkit.getPlayer(event.player());
      if (player != null) {
        scope.invoke("onViewEvent",
            () -> handler.handle(player, event.viewId(), event.type().name(), event.detail()));
      }
    }));
  }

  private static int integer(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    return value == null ? 0 : Integer.parseInt(String.valueOf(value));
  }
}
