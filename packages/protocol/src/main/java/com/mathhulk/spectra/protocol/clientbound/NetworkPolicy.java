package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Internet origins this server's views may contact directly (fetch, WebSocket, images, ...), e.g.
 * {@code "https://api.example.com"} or {@code "https://*.example.com"}. Only https/wss origins are allowed. Players
 * can still turn direct network access off on their side.
 */
public record NetworkPolicy(List<String> allowedOrigins) implements ClientboundPacket {
  public NetworkPolicy {
    allowedOrigins = List.copyOf(allowedOrigins);
  }

  public static NetworkPolicy read(FrameReader reader) {
    return new NetworkPolicy(reader.readList(Limits.MAX_NETWORK_ORIGINS, r -> r.readString(Limits.MAX_SHORT_TEXT)));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeList(allowedOrigins, Limits.MAX_NETWORK_ORIGINS, (w, o) -> w.writeString(o, Limits.MAX_SHORT_TEXT));
  }

  /** Whether {@code url} matches one of the allowed origins. Wildcards match one or more subdomain labels. */
  public boolean allows(String url) {
    URI uri;
    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException e) {
      return false;
    }
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!scheme.equals("https") && !scheme.equals("wss") || uri.getHost() == null) {
      return false;
    }
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    int port = uri.getPort() == -1 ? 443 : uri.getPort();
    for (String pattern : allowedOrigins) {
      URI allowed;
      try {
        allowed = URI.create(pattern.replace("*.", "wildcard-placeholder."));
      } catch (IllegalArgumentException e) {
        continue;
      }
      if (allowed.getHost() == null || !"https".equalsIgnoreCase(allowed.getScheme())) {
        continue;
      }
      int allowedPort = allowed.getPort() == -1 ? 443 : allowed.getPort();
      String allowedHost = allowed.getHost().toLowerCase(Locale.ROOT);
      boolean hostMatches = allowedHost.startsWith("wildcard-placeholder.")
          ? host.endsWith(allowedHost.substring("wildcard-placeholder".length()))
          : host.equals(allowedHost);
      if (hostMatches && port == allowedPort) {
        return true;
      }
    }
    return false;
  }
}
