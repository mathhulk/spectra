package com.mathhulk.spectra.api.views;

import com.mathhulk.spectra.api.Registration;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Server-driven UI. View ids are chosen by the caller; prefix them with your plugin name to avoid clashes. */
public interface Views {
  enum OpenResult {
    /** Sent to the client. */
    SENT,
    /** Sent once the player's resources finish syncing. */
    QUEUED,
    /** The player's client cannot show this kind of view. Fall back (vanilla dialog, chat, ...) or enforce. */
    UNSUPPORTED
  }

  /** A view sent JSON with {@code window.spectra.send}. The JSON is untrusted player input. */
  record Message(UUID player, String viewId, String json) {
  }

  enum EventType {
    OPENED, CLOSED, FAILED
  }

  record Event(UUID player, String viewId, EventType type, String detail) {
  }

  boolean supports(UUID player, ViewKind kind);

  OpenResult open(UUID player, String viewId, ViewSpec spec);

  boolean setVisible(UUID player, String viewId, boolean visible);

  boolean close(UUID player, String viewId);

  /** Sends JSON to one view, or to every open view of the player when {@code viewId} is empty. */
  boolean send(UUID player, String viewId, String json);

  Map<String, ViewSpec> open(UUID player);

  Registration onMessage(Consumer<Message> listener);

  Registration onEvent(Consumer<Event> listener);
}
