package com.mathhulk.spectra.fabric;

import com.mathhulk.spectra.fabric.browser.AsyncResourceHandler;
import com.mathhulk.spectra.fabric.browser.Bridge;
import com.mathhulk.spectra.fabric.browser.BrowserHost;
import com.mathhulk.spectra.fabric.browser.GameData;
import com.mathhulk.spectra.fabric.net.ApiTunnel;
import com.mathhulk.spectra.fabric.resources.ObjectStore;
import com.mathhulk.spectra.fabric.resources.ResourceSync;
import com.mathhulk.spectra.fabric.views.ViewManager;
import com.mathhulk.spectra.protocol.Capability;
import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.Protocol;
import com.mathhulk.spectra.protocol.ResourcePath;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.clientbound.*;
import com.mathhulk.spectra.protocol.serverbound.Hello;
import de.keksuccino.rinku.MIMEUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefPostDataElement;
import org.cef.network.CefRequest;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;

/** State for one connection to a Spectra server. Client thread only, except {@link #handle(CefBrowser, CefFrame, CefRequest)}. */
final class ServerConnection implements BrowserHost.Origin {
  private enum State {
    CONNECTED, HELLO_SENT, READY, REJECTED
  }

  private final BrowserHost browsers;
  private final ObjectStore store;
  private final Consumer<ServerboundPacket> send;
  private final Logger logger;
  private final String modVersion;
  private final ClientSettings settings;
  private final ApiTunnel tunnel;
  private State state = State.CONNECTED;
  private String origin;
  private ResourceSync resources;
  private ViewManager views;
  private volatile NetworkPolicy policy = new NetworkPolicy(List.of());

  ServerConnection(BrowserHost browsers, ObjectStore store, Consumer<ServerboundPacket> send, Logger logger,
      String modVersion, ClientSettings settings) {
    this.browsers = browsers;
    this.store = store;
    this.send = send;
    this.logger = logger;
    this.modVersion = modVersion;
    this.settings = settings;
    this.tunnel = new ApiTunnel(send);
  }

  ViewManager views() {
    return views;
  }

  /** Called once the server announces it can receive the Spectra channel. */
  void sendHello() {
    if (state != State.CONNECTED) {
      return;
    }
    Set<String> capabilities = new HashSet<>(Set.of(Capability.RESOURCES, Capability.NETWORK));
    if (browsers.available()) {
      capabilities.addAll(Set.of(Capability.SCREENS, Capability.LAYERS, Capability.MESSAGES, Capability.HTTP,
          Capability.SOCKETS));
    }
    send.accept(new Hello(Protocol.VERSION, modVersion, capabilities));
    state = State.HELLO_SENT;
  }

  void handle(ClientboundPacket packet) {
    if (packet instanceof Welcome welcome) {
      handleWelcome(welcome);
      return;
    }
    if (state != State.READY) {
      return;
    }
    switch (packet) {
      case ResourceManifest manifest -> resources.handleManifest(manifest);
      case ResourceChunk chunk -> resources.handleChunk(chunk);
      case OpenView open -> views.open(open);
      case SetViewVisible visible -> views.setVisible(visible.viewId(), visible.visible());
      case CloseView close -> views.close(close.viewId());
      case ViewMessage message -> views.message(message.viewId(), message.json());
      case HttpResponse response -> tunnel.handle(response);
      case HttpResponseBody body -> tunnel.handle(body);
      case SocketOpened opened -> tunnel.handle(opened);
      case SocketMessage message -> tunnel.handle(message);
      case SocketClosed closed -> tunnel.handle(closed);
      case NetworkPolicy network -> policy = network;
      case Welcome ignored -> {
      }
    }
  }

  private boolean allowsNetwork(String url) {
    return settings.allowServerNetwork && policy.allows(url);
  }

  private void handleWelcome(Welcome welcome) {
    if (state != State.HELLO_SENT) {
      return;
    }
    if (!welcome.accepted()) {
      state = State.REJECTED;
      logger.warn("Spectra server rejected this client: {}", welcome.reason());
      return;
    }
    origin = BrowserHost.hostFor(welcome.serverId());
    resources = new ResourceSync(store, Util.ioPool(), Minecraft.getInstance(), send, logger);
    views = new ViewManager(browsers, origin, send, tunnel, this::allowsNetwork);
    if (browsers.available()) {
      browsers.install();
      browsers.serve(origin, this);
    }
    state = State.READY;
  }

  // ---- the origin: static resources, built-in endpoints, and the /api tunnel (CEF IO thread) ----

  private static Optional<String> path(String url) {
    try {
      return Optional.of(URLDecoder.decode(URI.create(url).getRawPath(), StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @Override
  public CefResourceHandler handle(CefBrowser browser, CefFrame frame, CefRequest request) {
    String path = path(request.getURL()).orElse("/");
    ResourceSync sync = resources;
    if (path.equals("/_spectra/overlay.html")) {
      return AsyncResourceHandler.of(200, "text/html",
          com.mathhulk.spectra.fabric.views.HudOverlayPage.html().getBytes(StandardCharsets.UTF_8));
    }
    if (path.startsWith("/_spectra/game/")) {
      return new AsyncResourceHandler((cefRequest, handler) -> Minecraft.getInstance().execute(() -> {
        try {
          if (path.equals("/_spectra/game/player")) {
            handler.complete(200, "application/json", Map.of("cache-control", "no-store"),
                GameData.player(Minecraft.getInstance()));
          } else if (path.equals("/_spectra/game/map.png")) {
            handler.complete(200, "image/png", Map.of("cache-control", "no-store"),
                GameData.map(Minecraft.getInstance(), queryOf(cefRequest.getURL())));
          } else {
            handler.complete(404, "text/plain", Map.of(), "Not found".getBytes(StandardCharsets.UTF_8));
          }
        } catch (IOException | RuntimeException e) {
          handler.complete(500, "text/plain", Map.of(), String.valueOf(e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
      }));
    }
    if (path.equals("/api") || path.startsWith("/api/")) {
      String viewId = views == null ? null : views.viewIdFor(browser, frame);
      if (viewId == null) {
        return AsyncResourceHandler.text(404, "Not found");
      }
      return new AsyncResourceHandler((cefRequest, handler) -> tunnel(viewId, cefRequest, handler));
    }
    return staticResource(sync, path);
  }

  private static Map<String, String> queryOf(String url) {
    Map<String, String> query = new HashMap<>();
    String raw = URI.create(url).getRawQuery();
    if (raw != null) {
      for (String pair : raw.split("&")) {
        int eq = pair.indexOf('=');
        if (eq > 0) {
          query.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
      }
    }
    return query;
  }

  private void tunnel(String viewId, CefRequest request, AsyncResourceHandler handler) {
    URI uri = URI.create(request.getURL());
    String pathAndQuery = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    Map<String, String> headers = new HashMap<>();
    request.getHeaderMap(headers);
    byte[] body = body(request);
    String method = request.getMethod().toUpperCase(Locale.ROOT);
    Minecraft.getInstance().execute(() -> tunnel.request(viewId, method, pathAndQuery, headers, body, response -> {
      Map<String, String> joined = new HashMap<>();
      response.headers().forEach((name, values) -> joined.put(name, String.join(", ", values)));
      String type = joined.getOrDefault("content-type", "application/octet-stream");
      handler.complete(response.status(), type.split(";")[0].trim(), joined, response.body());
    }));
  }

  private static byte[] body(CefRequest request) {
    if (request.getPostData() == null) {
      return new byte[0];
    }
    Vector<CefPostDataElement> elements = new Vector<>();
    request.getPostData().getElements(elements);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (CefPostDataElement element : elements) {
      if (element.getType() == CefPostDataElement.Type.PDE_TYPE_BYTES) {
        byte[] bytes = new byte[element.getBytesCount()];
        element.getBytes(bytes.length, bytes);
        out.writeBytes(bytes);
      }
    }
    return out.toByteArray();
  }

  private static CefResourceHandler staticResource(ResourceSync sync, String rawPath) {
    String path = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
    if (path.isEmpty() || path.endsWith("/")) {
      path += "index.html";
    }
    Optional<Path> file = sync == null || !ResourcePath.isValid(path) ? Optional.empty() : sync.resolve(path);
    if (file.isEmpty()) {
      return AsyncResourceHandler.text(404, "Not found");
    }
    try {
      byte[] bytes = Files.readAllBytes(file.get());
      int dot = path.lastIndexOf('.');
      String mime = MIMEUtil.mimeFromExtension(dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT));
      mime = mime == null ? "application/octet-stream" : mime;
      return AsyncResourceHandler.of(200, mime, mime.equals("text/html") ? Bridge.inject(bytes) : bytes);
    } catch (IOException e) {
      return AsyncResourceHandler.text(404, "Not found");
    }
  }

  void dispose() {
    if (views != null) {
      views.dispose();
    }
    if (origin != null) {
      browsers.stopServing(origin);
    }
  }
}
