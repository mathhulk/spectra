package com.mathhulk.spectra.fabric.views;

import com.mathhulk.spectra.fabric.browser.BrowserHost;
import com.mathhulk.spectra.fabric.net.ApiTunnel;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.clientbound.OpenView;
import com.mathhulk.spectra.protocol.clientbound.ViewKind;
import com.mathhulk.spectra.protocol.serverbound.ViewEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Every view one server opened: screens (one browser each) and HUD layers (iframes in one shared overlay browser
 * per HUD slot). Client thread only, except {@link #viewIdFor}.
 */
public final class ViewManager {
  private static final String TOP = "top";

  private final BrowserHost host;
  private final String origin;
  private final Consumer<ServerboundPacket> send;
  private final ApiTunnel tunnel;
  private final Queries queries;
  private final Predicate<String> network;
  private final Map<String, BrowserView> screens = new LinkedHashMap<>();
  private final Map<String, HudOverlay> overlays = new HashMap<>();
  /** Screen browser id -> view id, for requests arriving on CEF threads. */
  private final Map<Integer, String> screenBrowsers = new java.util.concurrent.ConcurrentHashMap<>();

  public ViewManager(BrowserHost host, String origin, Consumer<ServerboundPacket> send, ApiTunnel tunnel,
      Predicate<String> network) {
    this.host = host;
    this.origin = origin;
    this.send = send;
    this.tunnel = tunnel;
    this.queries = new Queries(send, tunnel);
    this.network = network;
  }

  /** Which view a request came from: the iframe name for layers, the screen's view id otherwise. */
  public String viewIdFor(CefBrowser browser, CefFrame frame) {
    if (frame != null && !frame.isMain() && !frame.getName().isEmpty()) {
      return frame.getName();
    }
    return screenBrowsers.get(browser.getIdentifier());
  }

  private static String slot(OpenView spec) {
    return Objects.requireNonNullElse(HudLayers.slotFor(spec.after().orElse(null)), TOP);
  }

  public void open(OpenView spec) {
    close(spec.viewId());
    host.install();
    if (!host.available()) {
      send.accept(new ViewEvent(spec.viewId(), ViewEvent.Type.FAILED, "browser unavailable"));
      return;
    }
    if (spec.kind() == ViewKind.LAYER) {
      overlays.computeIfAbsent(slot(spec), key -> new HudOverlay(origin, host, send, queries, network)).put(spec);
      return;
    }
    BrowserView view = new BrowserView(spec, origin, host, send, queries, network);
    screens.put(spec.viewId(), view);
    screenBrowsers.put(view.browser().getIdentifier(), spec.viewId());
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.gui.screen() instanceof ViewScreen current) {
      current.markClosedByServer();
      closeScreen(current.view());
    }
    minecraft.gui.setScreen(new ViewScreen(view, this));
  }

  public void setVisible(String viewId, boolean visible) {
    BrowserView screen = screens.get(viewId);
    if (screen != null) {
      screen.setVisible(visible);
    }
    overlays.values().forEach(overlay -> overlay.setVisible(viewId, visible));
  }

  public void close(String viewId) {
    tunnel.closeView(viewId);
    BrowserView screen = screens.get(viewId);
    if (screen != null) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gui.screen() instanceof ViewScreen current && current.view() == screen) {
        current.markClosedByServer();
        minecraft.gui.setScreen(null);
      }
      closeScreen(screen);
    }
    overlays.entrySet().removeIf(entry -> {
      HudOverlay overlay = entry.getValue();
      if (overlay.remove(viewId) && overlay.isEmpty()) {
        overlay.close();
        return true;
      }
      return false;
    });
  }

  private void closeScreen(BrowserView view) {
    screens.remove(view.id(), view);
    screenBrowsers.remove(view.browser().getIdentifier());
    view.close();
  }

  /** Delivers JSON to one view, or to every view when {@code viewId} is empty. */
  public void message(String viewId, String json) {
    if (viewId.isEmpty()) {
      screens.values().forEach(view -> view.post(json));
      overlays.values().forEach(overlay -> overlay.viewIds().forEach(id -> overlay.post(id, json)));
      return;
    }
    BrowserView screen = screens.get(viewId);
    if (screen != null) {
      screen.post(json);
    }
    overlays.values().forEach(overlay -> overlay.post(viewId, json));
  }

  void screenRemoved(BrowserView view, boolean closedByServer) {
    if (closedByServer) {
      return;
    }
    // The player closed the screen (Escape, or another screen replaced it).
    tunnel.closeView(view.id());
    closeScreen(view);
    send.accept(new ViewEvent(view.id(), ViewEvent.Type.CLOSED, "player"));
  }

  /** Draws the overlay for a HUD slot ({@code null} = on top of the HUD). */
  public void drawLayers(GuiGraphicsExtractor graphics, String slot) {
    HudOverlay overlay = overlays.get(slot == null ? TOP : slot);
    if (overlay != null) {
      overlay.draw(graphics);
    }
  }

  public void dispose() {
    tunnel.closeView(null);
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.gui.screen() instanceof ViewScreen viewScreen) {
      viewScreen.markClosedByServer();
      minecraft.gui.setScreen(null);
    }
    screens.values().forEach(BrowserView::close);
    screens.clear();
    screenBrowsers.clear();
    overlays.values().forEach(HudOverlay::close);
    overlays.clear();
  }
}
