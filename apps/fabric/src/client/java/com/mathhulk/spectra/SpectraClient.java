package com.mathhulk.spectra;

import com.mathhulk.spectra.browser.BrowserLayer;
import com.mathhulk.spectra.browser.BrowserScreen;
import com.mathhulk.spectra.ui.*;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SpectraClient implements ClientModInitializer {
  private static final Logger log = LoggerFactory.getLogger(SpectraClient.class);

  private static final ResourceLocation EXAMPLE_LAYER = ResourceLocation.fromNamespaceAndPath("spectra", "hud-example-layer");

  private final static ArrayList<BrowserLayer> browserLayers = new ArrayList<>();

  private static ResourceManager resourceManager;

  @Override
  public void onInitializeClient() {
    PayloadTypeRegistry.playC2S().register(HandshakeC2SPayload.TYPE, HandshakeC2SPayload.CODEC);

    PayloadTypeRegistry.playS2C().register(ResourcesS2CPayload.TYPE, ResourcesS2CPayload.CODEC);
    PayloadTypeRegistry.playC2S().register(ResourcesC2SPayload.TYPE, ResourcesC2SPayload.CODEC);
    PayloadTypeRegistry.playS2C().register(ResourceS2CPayload.TYPE, ResourceS2CPayload.CODEC);

    PayloadTypeRegistry.playS2C().register(OpenBrowserS2CPayload.TYPE, OpenBrowserS2CPayload.CODEC);

    ClientPlayConnectionEvents.INIT.register((handler, client) -> {
      ClientPlayNetworking.registerReceiver(OpenBrowserS2CPayload.TYPE, (payload, context) -> openBrowser());

      String serverAddress = handler.getConnection().getRemoteAddress().toString();
      resourceManager = new ResourceManager(serverAddress);
    });

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
      ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(new HandshakeC2SPayload(0));
      sender.sendPacket(packet);

      browserLayers.add(new BrowserLayer(resourceManager));
    });

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      if (resourceManager != null) {
        resourceManager.dispose();
      }
    });

    HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> layeredDrawer.attachLayerBefore(IdentifiedLayer.BOSS_BAR, EXAMPLE_LAYER, SpectraClient::render));
  }

  private void openBrowser() {
    Minecraft.getInstance().setScreen(new BrowserScreen(resourceManager));
  }

  private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
    for (BrowserLayer browserLayer : browserLayers) {
        browserLayer.render(graphics, deltaTracker);
    }
  }
}