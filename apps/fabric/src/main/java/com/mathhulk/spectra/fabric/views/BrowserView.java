package com.mathhulk.spectra.fabric.views;

import com.mathhulk.spectra.fabric.browser.BrowserHost;
import com.mathhulk.spectra.fabric.browser.Bridge;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.clientbound.Layout;
import com.mathhulk.spectra.protocol.clientbound.OpenView;
import com.mathhulk.spectra.protocol.serverbound.ViewEvent;
import com.mathhulk.spectra.protocol.serverbound.ViewPost;
import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/** One server-opened view and its browser. Client thread, except the {@link BrowserHost.Owner} callbacks. */
public final class BrowserView implements BrowserHost.Owner {
  private final OpenView spec;
  private final String origin;
  private final BrowserHost host;
  private final Consumer<ServerboundPacket> send;
  private final RinkuBrowser browser;
  private boolean visible;
  private volatile boolean closed;
  private boolean reportedOpen;
  private int pixelWidth = -1;
  private int pixelHeight = -1;

  BrowserView(OpenView spec, String origin, BrowserHost host, Consumer<ServerboundPacket> send) {
    this.spec = spec;
    this.origin = origin;
    this.host = host;
    this.send = send;
    this.visible = spec.visible();
    this.browser = Rinku.createBrowser("https://" + origin + "/" + spec.resource(), true);
    host.track(browser, this);
  }

  public String id() {
    return spec.viewId();
  }

  public OpenView spec() {
    return spec;
  }

  public boolean visible() {
    return visible;
  }

  void setVisible(boolean visible) {
    this.visible = visible;
  }

  RinkuBrowser browser() {
    return browser;
  }

  @Override
  public String origin() {
    return origin;
  }

  @Override
  public void onPost(String json) {
    Minecraft.getInstance().execute(() -> {
      if (!closed) {
        send.accept(new ViewPost(spec.viewId(), json));
      }
    });
  }

  @Override
  public void onLoaded(int httpStatus) {
    Minecraft.getInstance().execute(() -> {
      if (closed || reportedOpen) {
        return;
      }
      reportedOpen = true;
      send.accept(httpStatus >= 400
          ? new ViewEvent(spec.viewId(), ViewEvent.Type.FAILED, "HTTP " + httpStatus)
          : new ViewEvent(spec.viewId(), ViewEvent.Type.OPENED, ""));
    });
  }

  @Override
  public void onFailed(String reason) {
    Minecraft.getInstance().execute(() -> {
      if (!closed) {
        reportedOpen = true;
        send.accept(new ViewEvent(spec.viewId(), ViewEvent.Type.FAILED, truncate(reason)));
      }
    });
  }

  private static String truncate(String text) {
    return text.length() > 200 ? text.substring(0, 200) : text;
  }

  /** Delivers a server message to the page's {@code window.spectra.onMessage} listeners. */
  void post(String json) {
    browser.executeJavaScript("window.__spectraReceive && window.__spectraReceive(" + Bridge.jsString(json) + ");",
        browser.getURL(), 0);
  }

  /** Resizes the browser to {@code width}x{@code height} GUI pixels at the current GUI scale. */
  void resize(int width, int height) {
    double scale = Minecraft.getInstance().getWindow().getGuiScale();
    int w = Math.max(1, (int) Math.round(width * scale));
    int h = Math.max(1, (int) Math.round(height * scale));
    if (w != pixelWidth || h != pixelHeight) {
      pixelWidth = w;
      pixelHeight = h;
      browser.resize(w, h);
    }
  }

  void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
    resize(width, height);
    if (!browser.isTextureReady()) {
      return;
    }
    Identifier texture = browser.getTextureIdentifier();
    if (texture != null) {
      graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, width, height);
    }
  }

  /** Where this view goes on a {@code screenWidth}x{@code screenHeight} GUI, as {x, y, width, height}. */
  static int[] place(Layout layout, int screenWidth, int screenHeight) {
    int width = layout.width() == 0 ? screenWidth : Math.min(layout.width(), screenWidth);
    int height = layout.height() == 0 ? screenHeight : Math.min(layout.height(), screenHeight);
    int column = layout.anchor().ordinal() % 3;
    int row = layout.anchor().ordinal() / 3;
    int x = switch (column) {
      case 0 -> layout.x();
      case 1 -> (screenWidth - width) / 2 + layout.x();
      default -> screenWidth - width - layout.x();
    };
    int y = switch (row) {
      case 0 -> layout.y();
      case 1 -> (screenHeight - height) / 2 + layout.y();
      default -> screenHeight - height - layout.y();
    };
    return new int[] {x, y, width, height};
  }

  void close() {
    if (closed) {
      return;
    }
    closed = true;
    host.untrack(browser);
    browser.close();
  }
}
