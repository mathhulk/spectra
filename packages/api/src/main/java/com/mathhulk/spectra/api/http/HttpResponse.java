package com.mathhulk.spectra.api.http;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** A response to a tunneled request. Header names should be lower-case. */
public record HttpResponse(int status, Map<String, List<String>> headers, byte[] body) {
  public HttpResponse {
    if (status < 100 || status > 599) {
      throw new IllegalArgumentException("Invalid status " + status);
    }
    headers = Map.copyOf(headers);
  }

  public static HttpResponse of(int status, String contentType, byte[] body) {
    return new HttpResponse(status, Map.of("content-type", List.of(contentType)), body);
  }

  public static HttpResponse text(int status, String text) {
    return of(status, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
  }

  public static HttpResponse json(int status, String json) {
    return of(status, "application/json", json.getBytes(StandardCharsets.UTF_8));
  }

  public static HttpResponse notFound() {
    return text(404, "Not found");
  }
}
