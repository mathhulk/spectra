package com.mathhulk.spectra.core.http;

import com.mathhulk.spectra.api.Registration;
import com.mathhulk.spectra.api.http.HttpHandler;
import com.mathhulk.spectra.api.http.HttpRequest;
import com.mathhulk.spectra.api.http.HttpResponse;
import com.mathhulk.spectra.api.http.HttpRoutes;
import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.core.views.ViewService;
import com.mathhulk.spectra.protocol.Header;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.clientbound.HttpResponseBody;
import com.mathhulk.spectra.protocol.serverbound.HttpRequestBody;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;

/**
 * Server end of the HTTP tunnel: reassembles requests views made to {@code /api/...}, routes them to registered
 * handlers, and streams responses back. Main thread only.
 */
public final class HttpService implements HttpRoutes {
  public static final int MAX_REQUEST_BODY = 8 * 1024 * 1024;
  public static final int MAX_RESPONSE_BODY = 32 * 1024 * 1024;
  public static final int MAX_IN_FLIGHT = 32;
  public static final long TIMEOUT_TICKS = 30 * 20;
  private static final int RESPONSE_CHUNK = 256 * 1024;

  private static final class Pending {
    final com.mathhulk.spectra.protocol.serverbound.HttpRequest start;
    final ByteArrayOutputStream body = new ByteArrayOutputStream();
    boolean complete;
    boolean answered;
    Cancellable timeout;

    Pending(com.mathhulk.spectra.protocol.serverbound.HttpRequest start) {
      this.start = start;
      body.writeBytes(start.body());
      complete = start.last();
    }
  }

  private final Platform platform;
  private final ViewService views;
  private final Routes<HttpHandler> routes = new Routes<>();
  private final Map<UUID, Map<Integer, Pending>> players = new HashMap<>();

  public HttpService(Platform platform, ViewService views) {
    this.platform = platform;
    this.views = views;
  }

  @Override
  public Registration route(String prefix, HttpHandler handler) {
    return routes.add(prefix, handler);
  }

  public void handleRequest(UUID player, com.mathhulk.spectra.protocol.serverbound.HttpRequest packet) {
    Map<Integer, Pending> pending = players.computeIfAbsent(player, id -> new HashMap<>());
    if (pending.containsKey(packet.requestId())) {
      return; // Duplicate id: ignore rather than mixing bodies.
    }
    if (pending.size() >= MAX_IN_FLIGHT) {
      respond(player, packet.requestId(), HttpResponse.text(429, "Too many requests"));
      return;
    }
    Pending request = new Pending(packet);
    pending.put(packet.requestId(), request);
    if (request.complete) {
      dispatch(player, request);
    }
  }

  public void handleBody(UUID player, HttpRequestBody packet) {
    Map<Integer, Pending> pending = players.get(player);
    Pending request = pending == null ? null : pending.get(packet.requestId());
    if (request == null || request.complete) {
      return;
    }
    if (request.body.size() + packet.data().length > MAX_REQUEST_BODY) {
      request.complete = true;
      respond(player, packet.requestId(), HttpResponse.text(413, "Request body too large"));
      return;
    }
    request.body.writeBytes(packet.data());
    if (packet.last()) {
      request.complete = true;
      dispatch(player, request);
    }
  }

  private void dispatch(UUID player, Pending pending) {
    var start = pending.start;
    String routePath = Routes.routePath(start.path());
    if (routePath == null || !views.isOpen(player, start.viewId())) {
      respond(player, start.requestId(), HttpResponse.notFound());
      return;
    }
    Map<String, List<String>> headers = new LinkedHashMap<>();
    for (Header header : start.headers()) {
      headers.computeIfAbsent(header.name(), name -> new ArrayList<>()).add(header.value());
    }
    HttpRequest request = new HttpRequest(player, start.viewId(), start.method(), start.path(), headers,
        pending.body.toByteArray());

    pending.timeout = platform.mainThread().runLater(
        () -> respond(player, start.requestId(), HttpResponse.text(504, "Handler timed out")), TIMEOUT_TICKS);
    Iterator<HttpHandler> handlers = routes.match(routePath).iterator();
    tryNext(player, start.requestId(), request, handlers);
  }

  private void tryNext(UUID player, int requestId, HttpRequest request, Iterator<HttpHandler> handlers) {
    while (handlers.hasNext()) {
      CompletionStage<HttpResponse> stage;
      try {
        stage = handlers.next().handle(request);
      } catch (RuntimeException e) {
        platform.logger().log(Level.SEVERE, "HTTP handler failed for " + request.path(), e);
        respond(player, requestId, HttpResponse.text(500, "Internal error"));
        return;
      }
      if (stage == null) {
        continue;
      }
      stage.whenComplete((response, error) -> platform.mainThread().execute(() -> {
        if (error != null) {
          platform.logger().log(Level.SEVERE, "HTTP handler failed for " + request.path(), error);
          respond(player, requestId, HttpResponse.text(500, "Internal error"));
        } else if (response == null) {
          tryNext(player, requestId, request, handlers);
        } else {
          respond(player, requestId, response);
        }
      }));
      return;
    }
    respond(player, requestId, HttpResponse.notFound());
  }

  private void respond(UUID player, int requestId, HttpResponse response) {
    Map<Integer, Pending> pending = players.get(player);
    Pending request = pending == null ? null : pending.get(requestId);
    if (request != null) {
      if (request.answered) {
        return;
      }
      request.answered = true;
      if (request.timeout != null) {
        request.timeout.cancel();
      }
      pending.remove(requestId);
    }
    if (!platform.isOnline(player)) {
      return;
    }
    byte[] body = response.body();
    if (body.length > MAX_RESPONSE_BODY) {
      response = HttpResponse.text(500, "Response body too large");
      body = response.body();
    }
    List<Header> headers = new ArrayList<>();
    response.headers().forEach((name, values) -> values.forEach(value -> headers.add(new Header(name, value))));
    int first = Math.min(body.length, RESPONSE_CHUNK);
    platform.sendFrame(player, Packets.encode(new com.mathhulk.spectra.protocol.clientbound.HttpResponse(requestId,
        response.status(), headers, Arrays.copyOf(body, first), first == body.length)));
    for (int offset = first; offset < body.length; offset += RESPONSE_CHUNK) {
      int end = Math.min(body.length, offset + RESPONSE_CHUNK);
      platform.sendFrame(player, Packets.encode(
          new HttpResponseBody(requestId, Arrays.copyOfRange(body, offset, end), end == body.length)));
    }
  }

  public void untrack(UUID player) {
    Map<Integer, Pending> pending = players.remove(player);
    if (pending != null) {
      pending.values().forEach(request -> {
        if (request.timeout != null) {
          request.timeout.cancel();
        }
      });
    }
  }
}
