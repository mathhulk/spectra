package com.mathhulk.spectra.core.http;

import com.mathhulk.spectra.api.Registration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Prefix routes, matched longest prefix first, then in registration order. */
final class Routes<H> {
  private record Route<H>(String prefix, long order, H handler) {
  }

  private final List<Route<H>> routes = new CopyOnWriteArrayList<>();
  private long nextOrder;

  Registration add(String prefix, H handler) {
    if (prefix == null || !prefix.startsWith("/")) {
      throw new IllegalArgumentException("Route prefixes start with '/': " + prefix);
    }
    Route<H> route = new Route<>(prefix, nextOrder++, handler);
    routes.add(route);
    return () -> routes.remove(route);
  }

  /** Handlers whose prefix matches {@code path} (relative to /api, without query), best match first. */
  List<H> match(String path) {
    List<Route<H>> matches = new ArrayList<>();
    for (Route<H> route : routes) {
      if (path.startsWith(route.prefix()) || path.equals(stripSlash(route.prefix()))) {
        matches.add(route);
      }
    }
    matches.sort(Comparator.<Route<H>>comparingInt(r -> -r.prefix().length()).thenComparingLong(Route::order));
    return matches.stream().map(Route::handler).toList();
  }

  private static String stripSlash(String prefix) {
    return prefix.length() > 1 && prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
  }

  /** "/api/shop/buy?x=1" -> "/shop/buy"; null when the path is not under /api. */
  static String routePath(String path) {
    int query = path.indexOf('?');
    String bare = query < 0 ? path : path.substring(0, query);
    if (bare.equals("/api")) {
      return "/";
    }
    return bare.startsWith("/api/") ? bare.substring(4) : null;
  }
}
