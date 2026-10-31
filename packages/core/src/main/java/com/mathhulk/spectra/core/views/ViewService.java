package com.mathhulk.spectra.core.views;

import com.mathhulk.spectra.core.Listeners;
import com.mathhulk.spectra.core.clients.ClientInfo;
import com.mathhulk.spectra.core.clients.ClientSessions;
import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.platform.Platform;
import com.mathhulk.spectra.core.resources.ResourceService;
import com.mathhulk.spectra.protocol.Capability;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.clientbound.CloseView;
import com.mathhulk.spectra.protocol.clientbound.OpenView;
import com.mathhulk.spectra.protocol.clientbound.SetViewVisible;
import com.mathhulk.spectra.protocol.clientbound.ViewKind;
import com.mathhulk.spectra.protocol.clientbound.ViewMessage;
import com.mathhulk.spectra.protocol.serverbound.ViewEvent;
import com.mathhulk.spectra.protocol.serverbound.ViewPost;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Opens, updates, and closes views for players, and routes messages in both directions. Main thread only.
 *
 * <p>Views requested before a player's resources are synced are queued and sent once sync completes, so scripts
 * can open a view right after join without racing the download.
 */
public final class ViewService {
  /** Result of an open request. Scripts use it to decide on a fallback. */
  public enum OpenResult {
    /** Sent to the client. */
    SENT,
    /** Will be sent when the player's resources finish syncing. */
    QUEUED,
    /** The player's client cannot show this kind of view. */
    UNSUPPORTED
  }

  /** A view sent JSON to the server. The JSON is untrusted input. */
  public record Post(UUID player, String viewId, String json) {
  }

  /** A view lifecycle event reported by the client. */
  public record Event(UUID player, String viewId, ViewEvent.Type type, String detail) {
  }

  private static final class PlayerViews {
    final Map<String, ViewSpec> open = new LinkedHashMap<>();
    final Map<String, ViewSpec> queued = new LinkedHashMap<>();
  }

  private final Platform platform;
  private final ClientSessions sessions;
  private final ResourceService resources;
  private final Map<UUID, PlayerViews> players = new HashMap<>();
  private final Listeners<Post> posts;
  private final Listeners<Event> events;

  public ViewService(Platform platform, ClientSessions sessions, ResourceService resources) {
    this.platform = platform;
    this.sessions = sessions;
    this.resources = resources;
    this.posts = new Listeners<>(platform.logger(), "view message");
    this.events = new Listeners<>(platform.logger(), "view event");
    resources.onSynced(synced -> flush(synced.player()));
  }

  public Cancellable onPost(Consumer<? super Post> listener) {
    return posts.add(listener);
  }

  public Cancellable onEvent(Consumer<? super Event> listener) {
    return events.add(listener);
  }

  private static String capabilityFor(ViewKind kind) {
    return kind == ViewKind.SCREEN ? Capability.SCREENS : Capability.LAYERS;
  }

  /** True when the player's client can show views of {@code kind} right now or once synced. */
  public boolean supports(UUID player, ViewKind kind) {
    ClientInfo info = sessions.info(player);
    return info.has(capabilityFor(kind)) && info.has(Capability.RESOURCES);
  }

  public OpenResult open(UUID player, String viewId, ViewSpec spec) {
    requireId(viewId);
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
    platform.sendFrame(player, Packets.encode(new OpenView(viewId, spec.kind(), spec.resource(), spec.layout(),
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

  public boolean setVisible(UUID player, String viewId, boolean visible) {
    PlayerViews views = players.get(player);
    if (views == null) {
      return false;
    }
    ViewSpec queued = views.queued.get(viewId);
    if (queued != null) {
      views.queued.put(viewId, new ViewSpec(queued.kind(), queued.resource(), queued.layout(), queued.zIndex(),
          visible, queued.after()));
      return true;
    }
    ViewSpec open = views.open.get(viewId);
    if (open == null) {
      return false;
    }
    views.open.put(viewId, new ViewSpec(open.kind(), open.resource(), open.layout(), open.zIndex(), visible,
        open.after()));
    platform.sendFrame(player, Packets.encode(new SetViewVisible(viewId, visible)));
    return true;
  }

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

  /** Sends JSON to one view, or to every open view when {@code viewId} is empty. */
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

  public Map<String, ViewSpec> openViews(UUID player) {
    PlayerViews views = players.get(player);
    if (views == null) {
      return Map.of();
    }
    Map<String, ViewSpec> all = new LinkedHashMap<>(views.open);
    all.putAll(views.queued);
    return Map.copyOf(all);
  }

  public Optional<ViewSpec> view(UUID player, String viewId) {
    return Optional.ofNullable(openViews(player).get(viewId));
  }

  public void handlePost(UUID player, ViewPost post) {
    PlayerViews views = players.get(player);
    if (views == null || !views.open.containsKey(post.viewId())) {
      return; // Only views the server opened may talk to it.
    }
    posts.fire(new Post(player, post.viewId(), post.json()));
  }

  public void handleEvent(UUID player, ViewEvent event) {
    PlayerViews views = players.get(player);
    if (views == null || !views.open.containsKey(event.viewId())) {
      return;
    }
    if (event.type() == ViewEvent.Type.CLOSED || event.type() == ViewEvent.Type.FAILED) {
      views.open.remove(event.viewId());
    }
    events.fire(new Event(player, event.viewId(), event.type(), event.detail()));
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
