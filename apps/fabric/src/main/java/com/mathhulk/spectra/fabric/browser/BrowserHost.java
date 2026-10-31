package com.mathhulk.spectra.fabric.browser;

import com.mathhulk.spectra.fabric.resources.ResourceSync;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ResourceHash;
import de.keksuccino.rinku.Rinku;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.slf4j.Logger;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide glue between Spectra and the embedded browser. Spectra pages live at
 * {@code https://<server-specific host>.spectra.invalid/}: a real https origin (so relative URLs, fetch, modules,
 * and storage behave normally) that never touches the network, isolated per server.
 *
 * <p>For browsers Spectra created, navigation is pinned to the page's own origin, every network request outside
 * it is cancelled, and external links only open after the player confirms them. Browsers created by other mods
 * are left alone.
 */
public final class BrowserHost {
  private static final String DOMAIN = ".spectra.invalid";

  /** A Spectra browser's link back to its view. Callbacks arrive on CEF threads. */
  public interface Owner {
    String origin();

    void onPost(String json);

    void onLoaded(int httpStatus);

    void onFailed(String reason);
  }

  private final Logger logger;
  private final Map<Integer, Owner> owners = new ConcurrentHashMap<>();
  private final Map<String, ResourceSync> hosts = new ConcurrentHashMap<>();
  private boolean installed;

  public BrowserHost(Logger logger) {
    this.logger = logger;
  }

  /** Stable host name for a server id; derived by hashing so server-supplied ids never reach a URL raw. */
  public static String hostFor(String serverId) {
    String hex = ResourceHash.compute(serverId.getBytes(StandardCharsets.UTF_8)).toHex();
    return "s" + hex.substring(0, 24) + DOMAIN;
  }

  public boolean available() {
    return Rinku.isInitialized();
  }

  /** Installs the router, request handler, and load handler. Call on the client thread after Rinku is ready. */
  public void install() {
    if (installed || !available()) {
      return;
    }
    CefClient client = Rinku.getClient().getHandle();
    CefMessageRouter router = CefMessageRouter.create();
    router.addHandler(new Router(), true);
    client.addMessageRouter(router);
    client.addRequestHandler(new Requests());
    Rinku.getClient().addLoadHandler(new Loads());
    installed = true;
  }

  public void serve(String host, ResourceSync resources) {
    if (hosts.put(host, resources) == null) {
      Rinku.getApp().getHandle().registerSchemeHandlerFactory("https", host,
          (browser, frame, scheme, request) -> {
            ResourceSync current = hosts.get(host);
            return current == null ? null : new ResourceHandler(current);
          });
    }
  }

  public void stopServing(String host) {
    hosts.remove(host);
  }

  public void track(CefBrowser browser, Owner owner) {
    owners.put(browser.getIdentifier(), owner);
  }

  public void untrack(CefBrowser browser) {
    owners.remove(browser.getIdentifier());
  }

  private static boolean sameOrigin(String url, String origin) {
    try {
      URI uri = URI.create(url);
      return "https".equals(uri.getScheme()) && origin.equalsIgnoreCase(uri.getHost()) && uri.getPort() == -1;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean inlineScheme(String url) {
    String lower = url.toLowerCase(Locale.ROOT);
    return lower.startsWith("data:") || lower.startsWith("blob:") || lower.equals("about:blank");
  }

  private final class Router extends CefMessageRouterHandlerAdapter {
    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent,
        CefQueryCallback callback) {
      Owner owner = owners.get(browser.getIdentifier());
      if (owner == null) {
        return false;
      }
      if (request.getBytes(StandardCharsets.UTF_8).length > Limits.MAX_SERVERBOUND_JSON) {
        callback.failure(413, "Message too large");
        return true;
      }
      owner.onPost(request);
      callback.success("");
      return true;
    }
  }

  private final class Requests extends CefRequestHandlerAdapter {
    private final CefResourceRequestHandler blockOutside = new CefResourceRequestHandlerAdapter() {
      @Override
      public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
        Owner owner = owners.get(browser.getIdentifier());
        String url = request.getURL();
        // Returning true cancels the request.
        return owner != null && !sameOrigin(url, owner.origin()) && !inlineScheme(url);
      }
    };

    @Override
    public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean userGesture,
        boolean isRedirect) {
      Owner owner = owners.get(browser.getIdentifier());
      if (owner == null || sameOrigin(request.getURL(), owner.origin())) {
        return false;
      }
      String url = request.getURL();
      if (userGesture && url.toLowerCase(Locale.ROOT).startsWith("https://")) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> ConfirmLinkScreen.confirmLinkNow(minecraft.gui.screen(), URI.create(url)));
      } else {
        logger.debug("Blocked navigation of a Spectra view to {}", url);
      }
      return true;
    }

    @Override
    public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String url, boolean userGesture) {
      return owners.containsKey(browser.getIdentifier());
    }

    @Override
    public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame,
        CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator,
        BoolRef disableDefaultHandling) {
      return owners.containsKey(browser.getIdentifier()) ? blockOutside : null;
    }
  }

  private final class Loads extends CefLoadHandlerAdapter {
    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
      Owner owner = owners.get(browser.getIdentifier());
      if (owner != null && frame.isMain()) {
        owner.onLoaded(httpStatusCode);
      }
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText,
        String failedUrl) {
      Owner owner = owners.get(browser.getIdentifier());
      if (owner != null && frame.isMain() && errorCode != CefLoadHandler.ErrorCode.ERR_ABORTED) {
        owner.onFailed(errorCode + " " + errorText);
      }
    }
  }
}
