package com.mathhulk.spectra.core.views;

import com.mathhulk.spectra.api.Registration;
import com.mathhulk.spectra.api.clients.ClientInfo;
import com.mathhulk.spectra.api.views.ViewKind;
import com.mathhulk.spectra.api.views.ViewSpec;
import com.mathhulk.spectra.api.views.Views;
import com.mathhulk.spectra.core.Listeners;
import com.mathhulk.spectra.core.clients.ClientSessions;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.core.resources.ResourceService;
import com.mathhulk.spectra.protocol.Capability;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.ResourcePath;
import com.mathhulk.spectra.protocol.clientbound.Anchor;
import com.mathhulk.spectra.protocol.clientbound.CloseView;
import com.mathhulk.spectra.protocol.clientbound.Layout;
import com.mathhulk.spectra.protocol.clientbound.OpenView;
import com.mathhulk.spectra.protocol.clientbound.SetViewVisible;
import com.mathhulk.spectra.protocol.clientbound.ViewMessage;
import com.mathhulk.spectra.protocol.serverbound.ViewEvent;
import com.mathhulk.spectra.protocol.serverbound.ViewPost;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Opens, updates, and closes views, and routes messages both ways. Main thread only. Views requested before a
 * player's resources are synced are queued and sent once sync completes.
 */
public final class ViewService implements Views {
  private static final class PlayerViews {
    final Map<String, ViewSpec> open = new LinkedHashMap<>();
    final Map<String, ViewSpec> queued = new LinkedHashMap<>();
  }

  private final Platform platform;
  private final ClientSessions sessions;
  private final ResourceService resources;
  private final Map<UUID, PlayerViews> players = new HashMap<>();
  private final Listeners<Message> messages;
  private final Listeners<Event> events;

  public ViewService(Platform platform, ClientSessions sessions, ResourceService resources) {
    this.platform = platform;
    this.sessions = sessions;
    this.resources = resources;
    this.messages = new Listeners<>(platform.logger(), "view message");
    this.events = new Listeners<>(platform.logger(), "view event");
    resources.onSynced(synced -> flush(synced.player()));
  }

  @Override
  public Registration onMessage(Consumer<Message> listener) {
    return messages.add(listener);
  }

  @Override
  public Registration onEvent(Consumer<Event> listener) {
    return events.add(listener);
  }

  @Override
  public boolean supports(UUID player, ViewKind kind) {
    ClientInfo info = sessions.info(player);
    return info.has(kind == ViewKind.SCREEN ? Capability.SCREENS : Capability.LAYERS) && info.has(Capability.RESOURCES);
  }

  /** True when {@code viewId} is open (sent, not merely queued) for the player. */
  public boolean isOpen(UUID player, String viewId) {
    PlayerViews views = players.get(player);
    return views != null && views.open.containsKey(viewId);
  }

  @Override
  public OpenResult open(UUID player, String viewId, ViewSpec spec) {
    requireId(viewId);
    ResourcePath.requireValid(spec.resource());
    if (!supports(player, spec.kind())) {
      return OpenResult.UNSUPPORTED;
    }
    PlayerViews views = players.computeIfAbsent(player, id -> new PlayerViews());
    if (!resources.isSynced(player)) {
      views.queued.put(viewId, spec);
      return OpenResult.QUEUED;
    }
    send(player, viewId, spec);
    views.open.put(viewId, spec);
    return OpenResult.SENT;
  }

  private void send(UUID player, String viewId, ViewSpec spec) {
    var layout = spec.layout();
    platform.sendFrame(player, Packets.encode(new OpenView(viewId,
        spec.kind() == ViewKind.SCREEN ? com.mathhulk.spectra.protocol.clientbound.ViewKind.SCREEN
            : com.mathhulk.spectra.protocol.clientbound.ViewKind.LAYER,
        spec.resource(),
        new Layout(Anchor.valueOf(layout.anchor().name()), layout.x(), layout.y(), layout.width(), layout.height()),
        spec.zIndex(), spec.visible(), spec.after())));
  }

  private void flush(UUID player) {
    PlayerViews views = players.get(player);
    if (views == null || views.queued.isEmpty()) {
      return;
    }
    Map<String, ViewSpec> queued = new LinkedHashMap<>(views.queued);
    views.queued.clear();
    queued.forEach((viewId, spec) -> {
      send(player, viewId, spec);
      views.open.put(viewId, spec);
    });
  }

  @Override
  public boolean setVisible(UUID player, String viewId, boolean visible) {
    PlayerViews views = players.get(player);
    if (views == null) {
      return false;
    }
    if (views.queued.containsKey(viewId)) {
      views.queued.put(viewId, views.queued.get(viewId).withVisible(visible));
      return true;
    }
    ViewSpec open = views.open.get(viewId);
    if (open == null) {
      return false;
    }
    views.open.put(viewId, open.withVisible(visible));
    platform.sendFrame(player, Packets.encode(new SetViewVisible(viewId, visible)));
    return true;
  }

  @Override
  public boolean close(UUID player, String viewId) {
    PlayerViews views = players.get(player);
    if (views == null) {
      return false;
    }
    if (views.queued.remove(viewId) != null) {
      return true;
    }
    if (views.open.remove(viewId) == null) {
      return false;
    }
    platform.sendFrame(player, Packets.encode(new CloseView(viewId)));
    return true;
  }

  @Override
  public boolean send(UUID player, String viewId, String json) {
    if (json.getBytes(StandardCharsets.UTF_8).length > Limits.MAX_CLIENTBOUND_JSON) {
      throw new IllegalArgumentException("Message exceeds " + Limits.MAX_CLIENTBOUND_JSON + " bytes");
    }
    PlayerViews views = players.get(player);
    if (views == null || (!viewId.isEmpty() && !views.open.containsKey(viewId))) {
      return false;
    }
    if (!sessions.info(player).has(Capability.MESSAGES)) {
      return false;
    }
    platform.sendFrame(player, Packets.encode(new ViewMessage(viewId, json)));
    return true;
  }

  @Override
  public Map<String, ViewSpec> open(UUID player) {
    PlayerViews views = players.get(player);
    if (views == null) {
      return Map.of();
    }
    Map<String, ViewSpec> all = new LinkedHashMap<>(views.open);
    all.putAll(views.queued);
    return Map.copyOf(all);
  }

  public void handlePost(UUID player, ViewPost post) {
    if (isOpen(player, post.viewId())) {
      messages.fire(new Message(player, post.viewId(), post.json()));
    }
  }

  public void handleEvent(UUID player, ViewEvent event) {
    PlayerViews views = players.get(player);
    if (views == null || !views.open.containsKey(event.viewId())) {
      return;
    }
    if (event.type() == ViewEvent.Type.CLOSED || event.type() == ViewEvent.Type.FAILED) {
      views.open.remove(event.viewId());
    }
    events.fire(new Event(player, event.viewId(), EventType.valueOf(event.type().name()), event.detail()));
  }

  public void untrack(UUID player) {
    players.remove(player);
  }

  private static void requireId(String viewId) {
    if (viewId.isEmpty() || viewId.getBytes(StandardCharsets.UTF_8).length > Limits.MAX_ID) {
      throw new IllegalArgumentException("View ids must be 1-" + Limits.MAX_ID + " bytes");
    }
  }
}
