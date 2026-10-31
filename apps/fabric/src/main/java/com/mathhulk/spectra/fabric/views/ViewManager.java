package com.mathhulk.spectra.fabric.views;

import com.mathhulk.spectra.fabric.browser.BrowserHost;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.clientbound.OpenView;
import com.mathhulk.spectra.protocol.clientbound.ViewKind;
import com.mathhulk.spectra.protocol.serverbound.ViewEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Every view one server opened. Client thread only. */
public final class ViewManager {
  private final BrowserHost host;
  private final String origin;
  private final Consumer<ServerboundPacket> send;
  private final Map<String, BrowserView> views = new LinkedHashMap<>();

  public ViewManager(BrowserHost host, String origin, Consumer<ServerboundPacket> send) {
    this.host = host;
    this.origin = origin;
    this.send = send;
  }

  public void open(OpenView spec) {
    close(spec.viewId());
    host.install();
    if (!host.available()) {
      send.accept(new ViewEvent(spec.viewId(), ViewEvent.Type.FAILED, "browser unavailable"));
      return;
    }
    BrowserView view = new BrowserView(spec, origin, host, send);
    views.put(spec.viewId(), view);
    if (spec.kind() == ViewKind.SCREEN) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gui.screen() instanceof ViewScreen current) {
        current.markClosedByServer();
        views.remove(current.view().id());
        current.view().close();
      }
      minecraft.gui.setScreen(new ViewScreen(view, this));
    }
  }

  public void setVisible(String viewId, boolean visible) {
    BrowserView view = views.get(viewId);
    if (view != null) {
      view.setVisible(visible);
    }
  }

  public void close(String viewId) {
    BrowserView view = views.remove(viewId);
    if (view == null) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.gui.screen() instanceof ViewScreen screen && screen.view() == view) {
      screen.markClosedByServer();
      minecraft.gui.setScreen(null);
    }
    view.close();
  }

  /** Delivers JSON to one view, or to every view when {@code viewId} is empty. */
  public void message(String viewId, String json) {
    if (viewId.isEmpty()) {
      views.values().forEach(view -> view.post(json));
    } else {
      BrowserView view = views.get(viewId);
      if (view != null) {
        view.post(json);
      }
    }
  }

  void screenRemoved(BrowserView view, boolean closedByServer) {
    if (closedByServer) {
      return;
    }
    // The player closed the screen (Escape, or another screen replaced it).
    views.remove(view.id(), view);
    view.close();
    send.accept(new ViewEvent(view.id(), ViewEvent.Type.CLOSED, "player"));
  }

  /** Draws visible layers attached after {@code after} (null = on top of the HUD), lowest zIndex first. */
  public void drawLayers(GuiGraphicsExtractor graphics, String after) {
    List<BrowserView> layers = new ArrayList<>();
    for (BrowserView view : views.values()) {
      if (view.spec().kind() == ViewKind.LAYER && view.visible()
          && Objects.equals(HudLayers.slotFor(view.spec().after().orElse(null)), after)) {
        layers.add(view);
      }
    }
    layers.sort(Comparator.comparingInt(view -> view.spec().zIndex()));
    for (BrowserView view : layers) {
      int[] rect = BrowserView.place(view.spec().layout(), graphics.guiWidth(), graphics.guiHeight());
      view.draw(graphics, rect[0], rect[1], rect[2], rect[3]);
    }
  }

  public void dispose() {
    Minecraft minecraft = Minecraft.getInstance();
    Screen screen = minecraft.gui.screen();
    if (screen instanceof ViewScreen viewScreen) {
      viewScreen.markClosedByServer();
      minecraft.gui.setScreen(null);
    }
    views.values().forEach(BrowserView::close);
    views.clear();
  }
}
