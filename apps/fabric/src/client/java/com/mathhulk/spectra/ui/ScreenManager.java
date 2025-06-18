package com.mathhulk.spectra.ui;

import com.mathhulk.spectra.browser.BrowserScreen;
import com.mathhulk.spectra.ui.payloads.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenManager {
    private static boolean initialized = false;

    private final ServerManager serverManager;

    public static void initialize() {
        if (initialized) return;

        PayloadTypeRegistry.playS2C().register(OpenScreenS2CPayload.TYPE, OpenScreenS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CloseScreenS2CPayload.TYPE, CloseScreenS2CPayload.CODEC);

        initialized = true;
    }

    public void dispose() {
        ClientPlayNetworking.unregisterReceiver(OpenScreenS2CPayload.OPEN_SCREEN_PAYLOAD_TYPE);
        ClientPlayNetworking.unregisterReceiver(CloseScreenS2CPayload.CLOSE_SCREEN_PAYLOAD_TYPE);

        close();
    }

    public ScreenManager(ServerManager serverManager) {
        if (!initialized) {
            throw new IllegalStateException("ScreenManager constructor called before ScreenManager.initialize()!");
        }

        this.serverManager = serverManager;

        ClientPlayNetworking.registerReceiver(OpenScreenS2CPayload.TYPE, (payload, context) -> {
            open(payload.resource(), payload.replace());
        });

        ClientPlayNetworking.registerReceiver(CloseScreenS2CPayload.TYPE, (payload, context) -> {
            close();
        });
    }

    private void open(String resource, boolean replace) {
        if (replace) {
            Screen currentScreen = Minecraft.getInstance().screen;
            Minecraft.getInstance().setScreen(new BrowserScreen(serverManager, resource, currentScreen));
            return;
        }

        Minecraft.getInstance().setScreen(new BrowserScreen(serverManager, resource));
    }

    public void postMessage(String message) {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (!(currentScreen instanceof BrowserScreen)) return;

        ((BrowserScreen) currentScreen).postMessage(message);
    }

    private void close() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (!(currentScreen instanceof BrowserScreen)) return;

        Minecraft.getInstance().setScreen(null);
    }
}
