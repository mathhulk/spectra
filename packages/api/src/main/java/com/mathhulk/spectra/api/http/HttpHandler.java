package com.mathhulk.spectra.api.http;

import java.util.concurrent.CompletionStage;

/**
 * Handles tunneled requests under a route prefix. Return {@code null} to let the next matching route try.
 * Called on the main thread; the returned stage may complete on any thread.
 */
@FunctionalInterface
public interface HttpHandler {
  CompletionStage<HttpResponse> handle(HttpRequest request);
}
