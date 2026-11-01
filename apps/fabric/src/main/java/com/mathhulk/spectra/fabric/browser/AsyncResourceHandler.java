package com.mathhulk.spectra.fabric.browser;

import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.LongRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A resource handler whose response may be produced later on another thread (a tunneled server response, data
 * gathered on the client thread). Implements JCEF's synchronous handler methods, which are deprecated but still
 * abstract in this JCEF build; {@code callback.Continue()} is invoked once the response is ready.
 */
@SuppressWarnings("deprecation")
public final class AsyncResourceHandler implements CefResourceHandler {
  /** Produces the response for {@code request} and completes {@code handler} (now or later, any thread). */
  @FunctionalInterface
  public interface Producer {
    void produce(CefRequest request, AsyncResourceHandler handler);
  }

  private final Producer producer;
  private volatile CefCallback callback;
  private volatile boolean ready;
  private int status = 200;
  private String mimeType = "text/plain";
  private Map<String, String> headers = Map.of();
  private byte[] body = new byte[0];
  private int offset;

  public AsyncResourceHandler(Producer producer) {
    this.producer = producer;
  }

  public static AsyncResourceHandler of(int status, String mimeType, byte[] body) {
    return new AsyncResourceHandler((request, handler) -> handler.complete(status, mimeType, Map.of(), body));
  }

  public static AsyncResourceHandler text(int status, String text) {
    return of(status, "text/plain", text.getBytes(StandardCharsets.UTF_8));
  }

  /** Supplies the response. Safe to call from any thread, once. */
  public synchronized void complete(int status, String mimeType, Map<String, String> headers, byte[] body) {
    if (ready) {
      return;
    }
    this.status = status;
    this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
    this.headers = headers;
    this.body = body;
    ready = true;
    if (callback != null) {
      callback.Continue();
    }
  }

  @Override
  public boolean processRequest(CefRequest request, CefCallback callback) {
    synchronized (this) {
      this.callback = callback;
    }
    producer.produce(request, this);
    synchronized (this) {
      if (ready) {
        callback.Continue();
      }
    }
    return true;
  }

  private void headers(CefResponse response) {
    response.setMimeType(mimeType);
    response.setStatus(status);
    if (!headers.isEmpty()) {
      Map<String, String> map = new HashMap<>(headers);
      map.remove("content-length");
      response.setHeaderMap(map);
    }
  }

  @Override
  public void getResponseHeaders(CefResponse response, IntRef length, StringRef redirectUrl) {
    headers(response);
    length.set(body.length);
  }

  @Override
  public void getResponseHeaders(CefResponse response, LongRef length, StringRef redirectUrl) {
    headers(response);
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
