package com.mathhulk.spectra.fabric;

import com.mathhulk.spectra.fabric.browser.BrowserHost;
import com.mathhulk.spectra.fabric.resources.ObjectStore;
import com.mathhulk.spectra.fabric.resources.ResourceSync;
import com.mathhulk.spectra.fabric.views.ViewManager;
import com.mathhulk.spectra.protocol.Capability;
import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.Protocol;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import com.mathhulk.spectra.protocol.clientbound.*;
import com.mathhulk.spectra.protocol.serverbound.Hello;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** State for one connection to a Spectra server. Client thread only. */
final class ServerConnection {
  private enum State {
    CONNECTED, HELLO_SENT, READY, REJECTED
  }

  private final BrowserHost browsers;
  private final ObjectStore store;
  private final Consumer<ServerboundPacket> send;
  private final Logger logger;
  private final String modVersion;
  private State state = State.CONNECTED;
  private String origin;
  private ResourceSync resources;
  private ViewManager views;

  ServerConnection(BrowserHost browsers, ObjectStore store, Consumer<ServerboundPacket> send, Logger logger,
      String modVersion) {
    this.browsers = browsers;
    this.store = store;
    this.send = send;
    this.logger = logger;
    this.modVersion = modVersion;
  }

  ViewManager views() {
    return views;
  }

  /** Called once the server announces it can receive the Spectra channel. */
  void sendHello() {
    if (state != State.CONNECTED) {
      return;
    }
    Set<String> capabilities = new HashSet<>(Set.of(Capability.RESOURCES));
    if (browsers.available()) {
      capabilities.addAll(Set.of(Capability.SCREENS, Capability.LAYERS, Capability.MESSAGES));
    }
    send.accept(new Hello(Protocol.VERSION, modVersion, capabilities));
    state = State.HELLO_SENT;
  }

  void handle(ClientboundPacket packet) {
    if (packet instanceof Welcome welcome) {
      handleWelcome(welcome);
      return;
    }
    if (state != State.READY) {
      return;
    }
    switch (packet) {
      case ResourceManifest manifest -> resources.handleManifest(manifest);
      case ResourceChunk chunk -> resources.handleChunk(chunk);
      case OpenView open -> views.open(open);
      case SetViewVisible visible -> views.setVisible(visible.viewId(), visible.visible());
      case CloseView close -> views.close(close.viewId());
      case ViewMessage message -> views.message(message.viewId(), message.json());
      case Welcome ignored -> {
      }
    }
  }

  private void handleWelcome(Welcome welcome) {
    if (state != State.HELLO_SENT) {
      return;
    }
    if (!welcome.accepted()) {
      state = State.REJECTED;
      logger.warn("Spectra server rejected this client: {}", welcome.reason());
      return;
    }
    origin = BrowserHost.hostFor(welcome.serverId());
    resources = new ResourceSync(store, Util.ioPool(), Minecraft.getInstance(), send, logger);
    views = new ViewManager(browsers, origin, send);
    if (browsers.available()) {
      browsers.install();
      browsers.serve(origin, resources);
    }
    state = State.READY;
  }

  void dispose() {
    if (views != null) {
      views.dispose();
    }
    if (origin != null) {
      browsers.stopServing(origin);
    }
  }
}
