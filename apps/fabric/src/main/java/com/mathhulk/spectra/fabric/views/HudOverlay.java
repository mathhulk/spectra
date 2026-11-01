package com.mathhulk.spectra.fabric.views;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mathhulk.spectra.fabric.browser.Bridge;
import com.mathhulk.spectra.fabric.browser.BrowserHost;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.clientbound.OpenView;
import com.mathhulk.spectra.protocol.serverbound.ViewEvent;
import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * One transparent, screen-sized browser that hosts every HUD layer of a HUD slot as an iframe (the FiveM NUI
 * model). Compared with a browser per layer this costs one renderer process, one texture upload, and one draw
 * per slot, however many layers a server opens.
 */
final class HudOverlay implements BrowserHost.Owner {
  /** The overlay page; layers are same-origin iframes named after their view id. */
  static final String PAGE = """
      <!doctype html><html><head><style>
        html, body { margin: 0; background: transparent; overflow: hidden; width: 100%; height: 100%; }
        iframe { position: absolute; border: 0; background: transparent; color-scheme: normal; }
      </style></head><body><script>
        const frames = new Map();
        window.__spectraOverlay = {
          update(layers) {
            const keep = new Set();
            for (const l of layers) {
              keep.add(l.id);
              let frame = frames.get(l.id);
              if (!frame) {
                frame = document.createElement('iframe');
                frame.name = l.id;
                frame.src = l.src;
                frame.addEventListener('load', () => window.cefQuery({ request: JSON.stringify({ t: 'loaded', view: l.id }) }));
                frames.set(l.id, frame);
                document.body.appendChild(frame);
              }
              Object.assign(frame.style, { left: l.x + 'px', top: l.y + 'px', width: l.w + 'px', height: l.h + 'px',
                zIndex: String(l.z), display: l.visible ? 'block' : 'none' });
            }
            for (const [id, frame] of frames) if (!keep.has(id)) { frame.remove(); frames.delete(id); }
          },
          post(id, text) { frames.get(id)?.contentWindow?.__spectraReceive?.(text); },
        };
        window.cefQuery({ request: JSON.stringify({ t: 'ready' }) });
      </script></body></html>
      """;

  static final class Layer {
    final OpenView spec;
    boolean visible;
    boolean opened;

    Layer(OpenView spec) {
      this.spec = spec;
      this.visible = spec.visible();
    }
  }

  private final String origin;
  private final BrowserHost host;
  private final Consumer<ServerboundPacket> send;
  private final Queries queries;
  private final Predicate<String> network;
  private final RinkuBrowser browser;
  private final Map<String, Layer> layers = new LinkedHashMap<>();
  /** Layer ids, readable from CEF threads. */
  private final Set<String> ids = ConcurrentHashMap.newKeySet();
  private volatile boolean ready;
  private String lastState = "";
  private int pixelWidth = -1;
  private int pixelHeight = -1;

  HudOverlay(String origin, BrowserHost host, Consumer<ServerboundPacket> send, Queries queries,
      Predicate<String> network) {
    this.origin = origin;
    this.host = host;
    this.send = send;
    this.queries = queries;
    this.network = network;
    this.browser = Rinku.createBrowser("https://" + origin + "/_spectra/overlay.html", true);
    host.track(browser, this);
  }

  void put(OpenView spec) {
    layers.put(spec.viewId(), new Layer(spec));
    ids.add(spec.viewId());
  }

  boolean remove(String viewId) {
    ids.remove(viewId);
    return layers.remove(viewId) != null;
  }

  boolean contains(String viewId) {
    return layers.containsKey(viewId);
  }

  boolean isEmpty() {
    return layers.isEmpty();
  }

  void setVisible(String viewId, boolean visible) {
    Layer layer = layers.get(viewId);
    if (layer != null) {
      layer.visible = visible;
    }
  }

  void post(String viewId, String json) {
    if (layers.containsKey(viewId)) {
      browser.executeJavaScript("__spectraOverlay.post(" + Bridge.jsString(viewId) + "," + Bridge.jsString(json) + ")",
          browser.getURL(), 0);
    }
  }

  Iterable<String> viewIds() {
    return List.copyOf(layers.keySet());
  }

  /** Lays out the iframes for the current GUI size and draws the overlay texture over the whole screen. */
  void draw(GuiGraphicsExtractor graphics) {
    int width = graphics.guiWidth();
    int height = graphics.guiHeight();
    double scale = Minecraft.getInstance().getWindow().getGuiScale();
    int w = Math.max(1, (int) Math.round(width * scale));
    int h = Math.max(1, (int) Math.round(height * scale));
    if (w != pixelWidth || h != pixelHeight) {
      pixelWidth = w;
      pixelHeight = h;
      browser.resize(w, h);
    }
    pushState(width, height, scale);
    if (!browser.isTextureReady() || layers.values().stream().noneMatch(layer -> layer.visible)) {
      return;
    }
    Identifier texture = browser.getTextureIdentifier();
    if (texture != null) {
      graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, width, height, width, height);
    }
  }

  private void pushState(int width, int height, double scale) {
    if (!ready) {
      return;
    }
    JsonArray state = new JsonArray();
    layers.values().stream().sorted(Comparator.comparingInt(layer -> layer.spec.zIndex())).forEach(layer -> {
      int[] rect = BrowserView.place(layer.spec.layout(), width, height);
      JsonObject entry = new JsonObject();
      entry.addProperty("id", layer.spec.viewId());
      entry.addProperty("src", "/" + layer.spec.resource());
      entry.addProperty("x", (int) Math.round(rect[0] * scale));
      entry.addProperty("y", (int) Math.round(rect[1] * scale));
      entry.addProperty("w", (int) Math.round(rect[2] * scale));
      entry.addProperty("h", (int) Math.round(rect[3] * scale));
      entry.addProperty("z", layer.spec.zIndex());
      entry.addProperty("visible", layer.visible);
      state.add(entry);
    });
    String json = state.toString();
    if (!json.equals(lastState)) {
      lastState = json;
      browser.executeJavaScript("__spectraOverlay.update(" + json + ")", browser.getURL(), 0);
    }
  }

  @Override
  public String origin() {
    return origin;
  }

  @Override
  public boolean allowsNetwork(String url) {
    return network.test(url);
  }

  @Override
  public void onQuery(CefFrame frame, String request, boolean persistent, CefQueryCallback callback) {
    if (!frame.isMain()) {
      String viewId = frame.getName();
      if (!ids.contains(viewId)) {
        callback.failure(404, "Unknown layer");
        return;
      }
      queries.handle(viewId, request, callback, () -> {
      });
      return;
    }
    JsonObject message = JsonParser.parseString(request).getAsJsonObject();
    String type = message.get("t").getAsString();
    if (type.equals("ready")) {
      ready = true;
      Minecraft.getInstance().execute(() -> lastState = "");
    } else if (type.equals("loaded")) {
      String viewId = message.get("view").getAsString();
      Minecraft.getInstance().execute(() -> {
        Layer layer = layers.get(viewId);
        if (layer != null && !layer.opened) {
          layer.opened = true;
          send.accept(new ViewEvent(viewId, ViewEvent.Type.OPENED, ""));
        }
      });
    }
    callback.success("");
  }

  @Override
  public void onLoaded(CefFrame frame, int httpStatus) {
  }

  @Override
  public void onFailed(CefFrame frame, String reason) {
  }

  void close() {
    host.untrack(browser);
    browser.close();
  }
}
