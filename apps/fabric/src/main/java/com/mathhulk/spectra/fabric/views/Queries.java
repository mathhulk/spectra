package com.mathhulk.spectra.fabric.views;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mathhulk.spectra.fabric.net.ApiTunnel;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.serverbound.ViewPost;
import net.minecraft.client.Minecraft;
import org.cef.callback.CefQueryCallback;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles the JSON envelopes the page bridge sends with {@code window.cefQuery}: messages for the server
 * ({@code post}) and tunneled WebSocket operations. Called on CEF threads; work hops to the client thread.
 */
final class Queries {
  private final Consumer<ServerboundPacket> send;
  private final ApiTunnel tunnel;
  /** Bridge socket id -> tunnel socket id, per view. */
  private final java.util.Map<String, Integer> sockets = new java.util.HashMap<>();

  Queries(Consumer<ServerboundPacket> send, ApiTunnel tunnel) {
    this.send = send;
    this.tunnel = tunnel;
  }

  void handle(String viewId, String request, CefQueryCallback callback, Runnable onLoaded) {
    JsonObject message;
    try {
      message = JsonParser.parseString(request).getAsJsonObject();
    } catch (RuntimeException e) {
      callback.failure(400, "Malformed message");
      return;
    }
    String type = message.has("t") ? message.get("t").getAsString() : "";
    Minecraft minecraft = Minecraft.getInstance();
    switch (type) {
      case "post" -> {
        String json = message.has("d") ? message.get("d").toString() : "null";
        if (json.getBytes(StandardCharsets.UTF_8).length > Limits.MAX_SERVERBOUND_JSON) {
          callback.failure(413, "Message too large");
          return;
        }
        minecraft.execute(() -> send.accept(new ViewPost(viewId, json)));
        callback.success("");
      }
      case "loaded" -> {
        onLoaded.run();
        callback.success("");
      }
      case "ws-open" -> {
        String key = viewId + "#" + message.get("id").getAsInt();
        String path = message.get("path").getAsString();
        List<String> protocols = new ArrayList<>();
        message.getAsJsonArray("protocols").forEach(p -> protocols.add(p.getAsString()));
        minecraft.execute(() -> {
          int id = tunnel.openSocket(viewId, path, protocols, new ApiTunnel.SocketEvents() {
            @Override
            public void opened(String protocol) {
              JsonObject event = new JsonObject();
              event.addProperty("e", "open");
              event.addProperty("protocol", protocol);
              callback.success(event.toString());
            }

            @Override
            public void message(boolean binary, byte[] data) {
              JsonObject event = new JsonObject();
              event.addProperty("e", "message");
              event.addProperty("binary", binary);
              if (binary) {
                event.addProperty("b64", Base64.getEncoder().encodeToString(data));
              } else {
                event.addProperty("text", new String(data, StandardCharsets.UTF_8));
              }
              callback.success(event.toString());
            }

            @Override
            public void closed(int code, String reason) {
              synchronized (sockets) {
                sockets.remove(key);
              }
              JsonObject event = new JsonObject();
              event.addProperty("e", "close");
              event.addProperty("code", code);
              event.addProperty("reason", reason);
              callback.success(event.toString());
              callback.failure(code, reason);
            }
          });
          synchronized (sockets) {
            sockets.put(key, id);
          }
        });
      }
      case "ws-send", "ws-close" -> {
        String key = viewId + "#" + message.get("id").getAsInt();
        minecraft.execute(() -> {
          Integer id;
          synchronized (sockets) {
            id = sockets.get(key);
          }
          if (id == null) {
            return;
          }
          if (type.equals("ws-close")) {
            tunnel.closeSocket(id, message.has("code") ? message.get("code").getAsInt() : 1000,
                message.has("reason") ? message.get("reason").getAsString() : "");
          } else if (message.has("text")) {
            tunnel.sendSocket(id, false, message.get("text").getAsString().getBytes(StandardCharsets.UTF_8));
          } else {
            tunnel.sendSocket(id, true, Base64.getDecoder().decode(message.get("b64").getAsString()));
          }
        });
        callback.success("");
      }
      default -> callback.failure(400, "Unknown message type");
    }
  }
}
