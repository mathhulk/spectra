package com.mathhulk.spectra.api.http;

import com.mathhulk.spectra.api.Registration;

import java.util.UUID;
import java.util.function.Consumer;

/** Server end of a WebSocket a view opened with {@code new WebSocket('/api/...')}. Main thread only. */
public interface WebSocket {
  /** A text or binary message. Untrusted player input. */
  record Message(boolean binary, byte[] data) {
    public String text() {
      return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  record Close(int code, String reason) {
  }

  UUID player();

  String viewId();

  /** Path including query, relative to the view origin (e.g. "/api/chat?room=1"). */
  String path();

  boolean isOpen();

  void send(String text);

  void send(byte[] binary);

  void close(int code, String reason);

  Registration onMessage(Consumer<Message> listener);

  Registration onClose(Consumer<Close> listener);
}
