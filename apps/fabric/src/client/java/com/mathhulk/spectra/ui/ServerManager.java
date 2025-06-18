package com.mathhulk.spectra.ui;

import com.mathhulk.spectra.browser.BrowserScreen;
import com.mathhulk.spectra.ui.payloads.HandshakeC2SPayload;
import com.mathhulk.spectra.ui.payloads.MessageS2CPayload;
import com.mathhulk.spectra.ui.payloads.OpenBrowserS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

public class ServerManager {
    private static ServerManager instance;

    private final LayerManager layerManager = new LayerManager(this);
    private final ResourceManager resourceManager = new ResourceManager(this);
    private final ScreenManager screenManager = new ScreenManager(this);
    private final String server;

    public ServerManager(String server) {
        this.server = server;

        ClientPlayNetworking.registerReceiver(MessageS2CPayload.TYPE, (payload, context) -> {
            layerManager.postMessage(payload.message());
            screenManager.postMessage(payload.message());
        });

        // TODO: Remove
        ClientPlayNetworking.registerReceiver(OpenBrowserS2CPayload.TYPE, (payload, context) -> openBrowser());
    }

    public String getServer() {
        return server;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public void dispose() {
        layerManager.dispose();
        resourceManager.dispose();
        screenManager.dispose();
    }

    public static ServerManager getInstance() {
        return instance;
    }

    public static void initialize() {
        LayerManager.initialize();
        ResourceManager.initialize();
        ScreenManager.initialize();

        PayloadTypeRegistry.playC2S().register(HandshakeC2SPayload.TYPE, HandshakeC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MessageS2CPayload.TYPE, MessageS2CPayload.CODEC);

        // TODO: Remove
        PayloadTypeRegistry.playS2C().register(OpenBrowserS2CPayload.TYPE, OpenBrowserS2CPayload.CODEC);

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            if (instance != null) {
                instance.dispose();
            }

            String server = handler.getConnection().getRemoteAddress().toString();
            instance = new ServerManager(server);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(new HandshakeC2SPayload(0));
            sender.sendPacket(packet);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (instance != null) {
                instance.dispose();
                instance = null;
            }
        });
    }

    private void openBrowser() {
        Minecraft.getInstance().setScreen(new BrowserScreen(this, "index.html"));
    }
}
