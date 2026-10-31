package com.mathhulk.spectra.fabric;

import com.mathhulk.spectra.fabric.browser.BrowserHost;
import com.mathhulk.spectra.fabric.net.SpectraPayload;
import com.mathhulk.spectra.fabric.resources.ObjectStore;
import com.mathhulk.spectra.fabric.views.HudLayers;
import com.mathhulk.spectra.fabric.views.ViewManager;
import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.ProtocolException;
import com.mathhulk.spectra.protocol.ServerboundPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entry point: registers the Spectra channel and HUD slots, and tracks the current server connection. */
public final class SpectraClient implements ClientModInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger("Spectra");

  private BrowserHost browsers;
  private ObjectStore store;
  private String modVersion;
  private ServerConnection connection;

  @Override
  public void onInitializeClient() {
    modVersion = FabricLoader.getInstance().getModContainer("spectra")
        .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    browsers = new BrowserHost(LOGGER);
    store = new ObjectStore(FabricLoader.getInstance().getGameDir().resolve("spectra").resolve("objects"));

    PayloadTypeRegistry.clientboundPlay().registerLarge(SpectraPayload.TYPE, SpectraPayload.CODEC,
        Limits.MAX_CLIENTBOUND_FRAME);
    PayloadTypeRegistry.serverboundPlay().register(SpectraPayload.TYPE, SpectraPayload.CODEC);

    ClientPlayNetworking.registerGlobalReceiver(SpectraPayload.TYPE, (payload, context) -> receive(payload));

    ClientPlayConnectionEvents.INIT.register((handler, client) -> connect());
    // Say hello only to servers that registered the Spectra channel, so vanilla servers never see our packets.
    ServerboundPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
      if (channels.contains(SpectraPayload.TYPE.id()) && connection != null) {
        client.execute(connection::sendHello);
      }
    });
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::disconnect));

    HudLayers.register(this::currentViews);
  }

  private ViewManager currentViews() {
    return connection == null ? null : connection.views();
  }

  private void connect() {
    Minecraft.getInstance().execute(() -> {
      disconnect();
      connection = new ServerConnection(browsers, store, SpectraClient::send, LOGGER, modVersion);
    });
  }

  private void disconnect() {
    if (connection != null) {
      connection.dispose();
      connection = null;
    }
  }

  private void receive(SpectraPayload payload) {
    ClientboundPacket packet;
    try {
      packet = Packets.decodeClientbound(payload.frame());
    } catch (ProtocolException e) {
      LOGGER.warn("Ignoring malformed Spectra packet: {}", e.getMessage());
      return;
    }
    Minecraft.getInstance().execute(() -> {
      if (connection != null) {
        connection.handle(packet);
      }
    });
  }

  private static void send(ServerboundPacket packet) {
    if (ClientPlayNetworking.canSend(SpectraPayload.TYPE)) {
      ClientPlayNetworking.send(new SpectraPayload(Packets.encode(packet)));
    }
  }
}
