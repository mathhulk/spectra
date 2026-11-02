package com.mathhulk.spectra.api.http;

import com.mathhulk.spectra.api.Registration;

/**
 * HTTP endpoints views reach with {@code fetch('/api/<prefix>...')} on their own origin, as if the server were a
 * web backend. Routes match by path prefix (without the {@code /api} part), longest prefix first; unmatched
 * requests get 404.
 */
public interface HttpRoutes {
  /** {@code prefix} starts with "/", e.g. "/shop/". Use your plugin name to avoid clashes. */
  Registration route(String prefix, HttpHandler handler);
}
