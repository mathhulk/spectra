package com.mathhulk.spectra.fabric.browser;

import com.mathhulk.spectra.fabric.resources.ResourceSync;
import com.mathhulk.spectra.protocol.ResourcePath;
import de.keksuccino.rinku.MIMEUtil;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.LongRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Serves one request for {@code https://<server host>/<path>} from the verified object store. Implements JCEF's
 * synchronous handler methods, which are deprecated but still abstract in this JCEF build.
 */
@SuppressWarnings("deprecation")
final class ResourceHandler implements CefResourceHandler {
  private final ResourceSync resources;
  private byte[] body = new byte[0];
  private int offset;
  private int status = 200;
  private String mimeType = "text/plain";

  ResourceHandler(ResourceSync resources) {
    this.resources = resources;
  }

  static Optional<String> resourcePath(String url) {
    try {
      String path = URLDecoder.decode(URI.create(url).getRawPath(), StandardCharsets.UTF_8);
      path = path.startsWith("/") ? path.substring(1) : path;
      if (path.isEmpty() || path.endsWith("/")) {
        path += "index.html";
      }
      return ResourcePath.isValid(path) ? Optional.of(path) : Optional.empty();
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean processRequest(CefRequest request, CefCallback callback) {
    Optional<Path> file = resourcePath(request.getURL()).flatMap(resources::resolve);
    String path = resourcePath(request.getURL()).orElse("");
    try {
      if (file.isEmpty()) {
        notFound();
      } else {
        byte[] bytes = Files.readAllBytes(file.get());
        int dot = path.lastIndexOf('.');
        String extension = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        String mime = MIMEUtil.mimeFromExtension(extension);
        mimeType = mime == null ? "application/octet-stream" : mime;
        body = mimeType.equals("text/html") ? Bridge.inject(bytes) : bytes;
      }
    } catch (IOException e) {
      notFound();
    }
    callback.Continue();
    return true;
  }

  private void notFound() {
    status = 404;
    mimeType = "text/plain";
    body = "Not found".getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public void getResponseHeaders(CefResponse response, IntRef length, StringRef redirectUrl) {
    response.setMimeType(mimeType);
    response.setStatus(status);
    length.set(body.length);
  }

  @Override
  public void getResponseHeaders(CefResponse response, LongRef length, StringRef redirectUrl) {
    response.setMimeType(mimeType);
    response.setStatus(status);
    length.set(body.length);
  }

  @Override
  public boolean readResponse(byte[] out, int bytesToRead, IntRef bytesRead, CefCallback callback) {
    int count = Math.min(bytesToRead, body.length - offset);
    if (count <= 0) {
      bytesRead.set(0);
      return false;
    }
    System.arraycopy(body, offset, out, 0, count);
    offset += count;
    bytesRead.set(count);
    return true;
  }

  @Override
  public void cancel() {
    body = new byte[0];
  }
}
