package com.mathhulk.spectra.api.http;

/**
 * Handles a new socket under a route prefix. Return {@code false} to decline and let the next matching route try;
 * a socket nobody accepts is closed with code 4404. Called on the main thread.
 */
@FunctionalInterface
public interface SocketHandler {
  boolean handle(WebSocket socket);
}
