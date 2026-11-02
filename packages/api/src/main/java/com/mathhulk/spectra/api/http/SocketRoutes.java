package com.mathhulk.spectra.api.http;

import com.mathhulk.spectra.api.Registration;

/**
 * WebSocket endpoints views reach with {@code new WebSocket('/api/<prefix>...')}, matched like {@link HttpRoutes}.
 */
public interface SocketRoutes {
  Registration route(String prefix, SocketHandler handler);
}
