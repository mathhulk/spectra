package com.mathhulk.spectra.api.http;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A request a view made with {@code fetch('/api/...')}, tunneled over the Spectra protocol. Everything here is
 * untrusted player input. {@code path} includes the query string and is relative to the view's origin, e.g.
 * {@code "/api/shop/buy?item=sword"}. Header names are lower-case.
 */
public record HttpRequest(UUID player, String viewId, String method, String path,
    Map<String, List<String>> headers, byte[] body) {
  public HttpRequest {
    headers = Map.copyOf(headers);
  }

  public String pathWithoutQuery() {
    int query = path.indexOf('?');
    return query < 0 ? path : path.substring(0, query);
  }

  public URI uri() {
    return URI.create(path);
  }

  public Optional<String> header(String name) {
    List<String> values = headers.get(name.toLowerCase(java.util.Locale.ROOT));
    return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst());
  }

  public String text() {
    return new String(body, StandardCharsets.UTF_8);
  }
}
