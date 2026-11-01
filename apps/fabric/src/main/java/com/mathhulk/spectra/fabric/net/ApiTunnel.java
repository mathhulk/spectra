package com.mathhulk.spectra.fabric.net;

import com.mathhulk.spectra.protocol.Header;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.clientbound.HttpResponse;
import com.mathhulk.spectra.protocol.clientbound.HttpResponseBody;
import com.mathhulk.spectra.protocol.clientbound.SocketClosed;
import com.mathhulk.spectra.protocol.clientbound.SocketMessage;
import com.mathhulk.spectra.protocol.clientbound.SocketOpened;
import com.mathhulk.spectra.protocol.serverbound.HttpRequest;
import com.mathhulk.spectra.protocol.serverbound.HttpRequestBody;
import com.mathhulk.spectra.protocol.serverbound.SocketClose;
import com.mathhulk.spectra.protocol.serverbound.SocketOpen;
import com.mathhulk.spectra.protocol.serverbound.SocketSend;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client end of the HTTP and WebSocket tunnels for one server connection. Client thread only; results are handed
 * to callbacks that may forward them to CEF threads.
 */
public final class ApiTunnel {
  /** A completed response: status, headers (lower-case names), body. */
  public record Response(int status, Map<String, List<String>> headers, byte[] body) {
  }

  /** Events for one tunneled socket, delivered on the client thread. */
  public interface SocketEvents {
    void opened(String protocol);

    void message(boolean binary, byte[] data);

    void closed(int code, String reason);
  }

  private static final class PendingResponse {
    final String viewId;
    final Consumer<Response> callback;
    HttpResponse head;
    final ByteArrayOutputStream body = new ByteArrayOutputStream();

    PendingResponse(String viewId, Consumer<Response> callback) {
      this.viewId = viewId;
      this.callback = callback;
    }
  }

  private static final class Socket {
    final String viewId;
    final SocketEvents events;
    final ByteArrayOutputStream partial = new ByteArrayOutputStream();
    boolean partialBinary;

    Socket(String viewId, SocketEvents events) {
      this.viewId = viewId;
      this.events = events;
    }
  }

  private static final int MAX_RESPONSE = 64 * 1024 * 1024;

  private final Consumer<ServerboundPacket> send;
  private final Map<Integer, PendingResponse> requests = new HashMap<>();
  private final Map<Integer, Socket> sockets = new LinkedHashMap<>();
  private int nextRequest = 1;
  private int nextSocket = 1;

  public ApiTunnel(Consumer<ServerboundPacket> send) {
    this.send = send;
  }

  public void request(String viewId, String method, String path, Map<String, String> headers, byte[] body,
      Consumer<Response> callback) {
    if (requests.size() >= 32) {
      callback.accept(new Response(429, Map.of(), "Too many requests".getBytes()));
      return;
    }
    int id = nextRequest++;
    requests.put(id, new PendingResponse(viewId, callback));
    List<Header> list = new ArrayList<>();
    headers.forEach((name, value) -> {
      if (list.size() < Limits.MAX_HEADERS && name.length() <= Limits.MAX_HEADER_NAME
          && value.length() <= Limits.MAX_HEADER_VALUE) {
        list.add(new Header(name, value));
      }
    });
    int first = Math.min(body.length, Limits.MAX_SERVERBOUND_BODY);
    send.accept(new HttpRequest(id, viewId, method, path, list, Arrays.copyOf(body, first), first == body.length));
    for (int offset = first; offset < body.length; offset += Limits.MAX_SERVERBOUND_BODY) {
      int end = Math.min(body.length, offset + Limits.MAX_SERVERBOUND_BODY);
      send.accept(new HttpRequestBody(id, Arrays.copyOfRange(body, offset, end), end == body.length));
    }
  }

  public void handle(HttpResponse response) {
    PendingResponse pending = requests.get(response.requestId());
    if (pending == null || pending.head != null) {
      return;
    }
    pending.head = response;
    pending.body.writeBytes(response.body());
    if (response.last()) {
      finish(response.requestId(), pending);
    }
  }

  public void handle(HttpResponseBody body) {
    PendingResponse pending = requests.get(body.requestId());
    if (pending == null || pending.head == null) {
      return;
    }
    if (pending.body.size() + body.data().length > MAX_RESPONSE) {
      requests.remove(body.requestId());
      pending.callback.accept(new Response(502, Map.of(), "Response too large".getBytes()));
      return;
    }
    pending.body.writeBytes(body.data());
    if (body.last()) {
      finish(body.requestId(), pending);
    }
  }

  private void finish(int id, PendingResponse pending) {
    requests.remove(id);
    Map<String, List<String>> headers = new LinkedHashMap<>();
    for (Header header : pending.head.headers()) {
      headers.computeIfAbsent(header.name(), name -> new ArrayList<>()).add(header.value());
    }
    pending.callback.accept(new Response(pending.head.status(), headers, pending.body.toByteArray()));
  }

  public int openSocket(String viewId, String path, List<String> protocols, SocketEvents events) {
    if (sockets.size() >= 32) {
      events.closed(1013, "Too many sockets");
      return -1;
    }
    int id = nextSocket++;
    sockets.put(id, new Socket(viewId, events));
    send.accept(new SocketOpen(id, viewId, path, protocols.stream().limit(Limits.MAX_SOCKET_PROTOCOLS).toList()));
    return id;
  }

  public void sendSocket(int id, boolean binary, byte[] data) {
    if (!sockets.containsKey(id)) {
      return;
    }
    int offset = 0;
    do {
      int end = Math.min(data.length, offset + Limits.MAX_SERVERBOUND_SOCKET_DATA);
      send.accept(new SocketSend(id, binary, Arrays.copyOfRange(data, offset, end), end == data.length));
      offset = end;
    } while (offset < data.length);
  }

  public void closeSocket(int id, int code, String reason) {
    Socket socket = sockets.remove(id);
    if (socket != null) {
      send.accept(new SocketClose(id, code, reason.length() > 120 ? reason.substring(0, 120) : reason));
      socket.events.closed(code, reason);
    }
  }

  public void handle(SocketOpened opened) {
    Socket socket = sockets.get(opened.socketId());
    if (socket != null) {
      socket.events.opened(opened.protocol());
    }
  }

  public void handle(SocketMessage message) {
    Socket socket = sockets.get(message.socketId());
    if (socket == null) {
      return;
    }
    if (socket.partial.size() == 0) {
      socket.partialBinary = message.binary();
    }
    socket.partial.writeBytes(message.data());
    if (message.fin()) {
      byte[] data = socket.partial.toByteArray();
      socket.partial.reset();
      socket.events.message(socket.partialBinary, data);
    }
  }

  public void handle(SocketClosed closed) {
    Socket socket = sockets.remove(closed.socketId());
    if (socket != null) {
      socket.events.closed(closed.code(), closed.reason());
    }
  }

  /** Closes everything belonging to a view (or everything, when {@code viewId} is null). */
  public void closeView(String viewId) {
    requests.values().removeIf(pending -> viewId == null || pending.viewId.equals(viewId));
    for (Map.Entry<Integer, Socket> entry : List.copyOf(sockets.entrySet())) {
      if (viewId == null || entry.getValue().viewId.equals(viewId)) {
        closeSocket(entry.getKey(), 1001, "View closed");
      }
    }
  }
}
