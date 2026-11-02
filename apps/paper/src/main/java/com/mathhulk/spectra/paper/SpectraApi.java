package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.api.Registration;
import com.mathhulk.spectra.api.Spectra;
import com.mathhulk.spectra.api.clients.ClientInfo;
import com.mathhulk.spectra.api.views.Anchor;
import com.mathhulk.spectra.api.views.Layout;
import com.mathhulk.spectra.api.views.ViewKind;
import com.mathhulk.spectra.api.views.ViewSpec;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The {@code spectra} script global: a Player-friendly face over the public {@link Spectra} API that third-party
 * plugins use too. Registrations are released when the script unloads. Main thread only; callbacks run under the
 * script's lock.
 */
public final class SpectraApi {
  @FunctionalInterface
  public interface ViewMessageHandler {
    void handle(Player player, String viewId, String json);
  }

  @FunctionalInterface
  public interface ViewEventHandler {
    void handle(Player player, String viewId, String type, String detail);
  }

  private final Spectra spectra;
  private final ScriptScope scope;
  private final String engine;

  SpectraApi(Spectra spectra, ScriptScope scope, String engine) {
    this.spectra = spectra;
    this.scope = scope;
    this.engine = engine;
  }

  /** The underlying Java API, for anything this facade does not cover. */
  public Spectra api() {
    return spectra;
  }

  public String engine() {
    return engine;
  }

  public ClientInfo client(Player player) {
    return spectra.clients().info(player.getUniqueId());
  }

  public void whenClientResolved(Player player, Consumer<ClientInfo> callback) {
    spectra.clients().resolve(player.getUniqueId())
        .thenAccept(info -> scope.invoke("whenClientResolved", () -> callback.accept(info)));
  }

  public Registration onClientResolved(Consumer<ClientInfo> callback) {
    return scope.track(spectra.clients().onResolved(info -> scope.invoke("onClientResolved",
        () -> callback.accept(info))));
  }

  public String openScreen(Player player, String viewId, String resource) {
    return spectra.views().open(player.getUniqueId(), viewId, ViewSpec.screen(resource)).name();
  }

  public String openLayer(Player player, String viewId, String resource) {
    return openLayer(player, viewId, resource, Map.of());
  }

  /** {@code options}: anchor ("top_right"), x, y, width, height (GUI pixels, 0 = fill), zIndex, visible, after. */
  public String openLayer(Player player, String viewId, String resource, Map<String, Object> options) {
    Map<String, Object> o = options == null ? Map.of() : options;
    Layout layout = new Layout(
        Anchor.valueOf(String.valueOf(o.getOrDefault("anchor", "top_left")).toUpperCase(Locale.ROOT)),
        integer(o.get("x")), integer(o.get("y")), integer(o.get("width")), integer(o.get("height")));
    ViewSpec spec = new ViewSpec(ViewKind.LAYER, resource, layout, integer(o.get("zIndex")),
        !Boolean.FALSE.equals(o.get("visible")), Optional.ofNullable(o.get("after")).map(String::valueOf));
    return spectra.views().open(player.getUniqueId(), viewId, spec).name();
  }

  public boolean setViewVisible(Player player, String viewId, boolean visible) {
    return spectra.views().setVisible(player.getUniqueId(), viewId, visible);
  }

  public boolean closeView(Player player, String viewId) {
    return spectra.views().close(player.getUniqueId(), viewId);
  }

  public boolean sendView(Player player, String viewId, String json) {
    return spectra.views().send(player.getUniqueId(), viewId, json);
  }

  public Registration onViewMessage(ViewMessageHandler handler) {
    return scope.track(spectra.views().onMessage(message -> {
      Player player = Bukkit.getPlayer(message.player());
      if (player != null) {
        scope.invoke("onViewMessage", () -> handler.handle(player, message.viewId(), message.json()));
      }
    }));
  }

  public Registration onViewEvent(ViewEventHandler handler) {
    return scope.track(spectra.views().onEvent(event -> {
      Player player = Bukkit.getPlayer(event.player());
      if (player != null) {
        scope.invoke("onViewEvent", () -> handler.handle(player, event.viewId(), event.type().name(),
            event.detail()));
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
