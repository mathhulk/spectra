package com.mathhulk.spectra.core.http;

import com.mathhulk.spectra.api.Registration;
import com.mathhulk.spectra.api.http.SocketHandler;
import com.mathhulk.spectra.api.http.SocketRoutes;
import com.mathhulk.spectra.api.http.WebSocket;
import com.mathhulk.spectra.core.Listeners;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.core.views.ViewService;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.clientbound.SocketClosed;
import com.mathhulk.spectra.protocol.clientbound.SocketMessage;
import com.mathhulk.spectra.protocol.clientbound.SocketOpened;
import com.mathhulk.spectra.protocol.serverbound.SocketClose;
import com.mathhulk.spectra.protocol.serverbound.SocketOpen;
import com.mathhulk.spectra.protocol.serverbound.SocketSend;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

/** Server end of the WebSocket tunnel. Main thread only. */
public final class SocketService implements SocketRoutes {
  public static final int MAX_SOCKETS = 32;
  public static final int MAX_MESSAGE = 4 * 1024 * 1024;
  private static final int FRAGMENT = 256 * 1024;

  /** Close codes, as in the WebSocket spec plus 4404 for "no route". */
  public static final int GOING_AWAY = 1001;
  public static final int TOO_BIG = 1009;
  public static final int NO_ROUTE = 4404;

  private final class Socket implements WebSocket {
    final UUID player;
    final int id;
    final SocketOpen open;
    final Listeners<Message> messages;
    final Listeners<Close> closes;
    final ByteArrayOutputStream partial = new ByteArrayOutputStream();
    boolean partialBinary;
    boolean open_ = true;

    Socket(UUID player, SocketOpen open) {
      this.player = player;
      this.id = open.socketId();
      this.open = open;
      this.messages = new Listeners<>(platform.logger(), "socket message");
      this.closes = new Listeners<>(platform.logger(), "socket close");
    }

    @Override
    public UUID player() {
      return player;
    }

    @Override
    public String viewId() {
      return open.viewId();
    }

    @Override
    public String path() {
      return open.path();
    }

    @Override
    public boolean isOpen() {
      return open_;
    }

    @Override
    public void send(String text) {
      sendData(false, text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void send(byte[] binary) {
      sendData(true, binary);
    }

    private void sendData(boolean binary, byte[] data) {
      if (!open_) {
        throw new IllegalStateException("Socket is closed");
      }
      if (data.length > MAX_MESSAGE) {
        throw new IllegalArgumentException("Message exceeds " + MAX_MESSAGE + " bytes");
      }
      int offset = 0;
      do {
        int end = Math.min(data.length, offset + FRAGMENT);
        platform.sendFrame(player,
            Packets.encode(new SocketMessage(id, binary, Arrays.copyOfRange(data, offset, end), end == data.length)));
        offset = end;
      } while (offset < data.length);
    }

    @Override
    public void close(int code, String reason) {
      if (!open_) {
        return;
      }
      platform.sendFrame(player, Packets.encode(new SocketClosed(id, code, truncate(reason))));
      closed(code, reason);
    }

    void closed(int code, String reason) {
      if (!open_) {
        return;
      }
      open_ = false;
      Map<Integer, Socket> sockets = players.get(player);
      if (sockets != null) {
        sockets.remove(id, this);
      }
      closes.fire(new Close(code, reason));
    }

    void receive(SocketSend fragment) {
      if (partial.size() == 0) {
        partialBinary = fragment.binary();
      }
      if (partial.size() + fragment.data().length > MAX_MESSAGE) {
        close(TOO_BIG, "Message too big");
        return;
      }
      partial.writeBytes(fragment.data());
      if (fragment.fin()) {
        byte[] data = partial.toByteArray();
        partial.reset();
        messages.fire(new Message(partialBinary, data));
      }
    }

    @Override
    public Registration onMessage(Consumer<Message> listener) {
      return messages.add(listener);
    }

    @Override
    public Registration onClose(Consumer<Close> listener) {
      return closes.add(listener);
    }
  }

  private final Platform platform;
  private final ViewService views;
  private final Routes<SocketHandler> routes = new Routes<>();
  private final Map<UUID, Map<Integer, Socket>> players = new HashMap<>();

  public SocketService(Platform platform, ViewService views) {
    this.platform = platform;
    this.views = views;
  }

  @Override
  public Registration route(String prefix, SocketHandler handler) {
    return routes.add(prefix, handler);
  }

  private static String truncate(String reason) {
    return reason == null ? "" : reason.length() > 120 ? reason.substring(0, 120) : reason;
  }

  public void handleOpen(UUID player, SocketOpen open) {
    Map<Integer, Socket> sockets = players.computeIfAbsent(player, id -> new HashMap<>());
    String routePath = Routes.routePath(open.path());
    List<SocketHandler> handlers = routePath == null ? List.of() : routes.match(routePath);
    if (sockets.containsKey(open.socketId()) || sockets.size() >= MAX_SOCKETS || handlers.isEmpty()
        || !views.isOpen(player, open.viewId())) {
      platform.sendFrame(player, Packets.encode(new SocketClosed(open.socketId(), NO_ROUTE, "No route")));
      return;
    }
    Socket socket = new Socket(player, open);
    sockets.put(open.socketId(), socket);
    platform.sendFrame(player, Packets.encode(new SocketOpened(open.socketId(), "")));
    for (SocketHandler handler : handlers) {
      try {
        if (handler.handle(socket) || !socket.isOpen()) {
          return;
        }
      } catch (RuntimeException e) {
        platform.logger().log(Level.SEVERE, "WebSocket handler failed for " + open.path(), e);
        socket.close(1011, "Internal error");
        return;
      }
    }
    socket.close(NO_ROUTE, "No route");
  }

  public void handleSend(UUID player, SocketSend send) {
    Socket socket = find(player, send.socketId());
    if (socket != null) {
      socket.receive(send);
    }
  }

  public void handleClose(UUID player, SocketClose close) {
    Socket socket = find(player, close.socketId());
    if (socket != null) {
      socket.closed(close.code(), close.reason());
    }
  }

  private Socket find(UUID player, int id) {
    Map<Integer, Socket> sockets = players.get(player);
    return sockets == null ? null : sockets.get(id);
  }

  public void untrack(UUID player) {
    Map<Integer, Socket> sockets = players.remove(player);
    if (sockets != null) {
      List.copyOf(sockets.values()).forEach(socket -> socket.closed(GOING_AWAY, "Player left"));
    }
  }
}
