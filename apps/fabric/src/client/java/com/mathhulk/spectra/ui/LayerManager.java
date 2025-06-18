package com.mathhulk.spectra.ui;

import com.mathhulk.spectra.browser.BrowserLayer;
import com.mathhulk.spectra.ui.payloads.DisableLayerS2CPayload;
import com.mathhulk.spectra.ui.payloads.EnableLayerS2CPayload;
import com.mathhulk.spectra.ui.payloads.RegisterLayerS2CPayload;
import com.mathhulk.spectra.ui.payloads.UnregisterLayerS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Map;

public class LayerManager {
    private static final ResourceLocation BROWSER_LAYER = ResourceLocation.fromNamespaceAndPath("spectra", "browser-layer");

    public static final Map<ResourceLocation, ArrayList<BrowserLayer>> layers = Map.ofEntries(
            Map.entry(IdentifiedLayer.BOSS_BAR, new ArrayList<>()),
            Map.entry(IdentifiedLayer.CHAT, new ArrayList<>()),
            Map.entry(IdentifiedLayer.CROSSHAIR, new ArrayList<>()),
            Map.entry(IdentifiedLayer.DEBUG, new ArrayList<>()),
            Map.entry(IdentifiedLayer.DEMO_TIMER, new ArrayList<>()),
            Map.entry(IdentifiedLayer.EXPERIENCE_LEVEL, new ArrayList<>()),
            Map.entry(IdentifiedLayer.HOTBAR_AND_BARS, new ArrayList<>()),
            Map.entry(IdentifiedLayer.TITLE_AND_SUBTITLE, new ArrayList<>()),
            Map.entry(IdentifiedLayer.SUBTITLES, new ArrayList<>()),
            Map.entry(IdentifiedLayer.STATUS_EFFECTS, new ArrayList<>()),
            Map.entry(IdentifiedLayer.SLEEP, new ArrayList<>()),
            Map.entry(IdentifiedLayer.SCOREBOARD, new ArrayList<>()),
            Map.entry(IdentifiedLayer.PLAYER_LIST, new ArrayList<>()),
            Map.entry(IdentifiedLayer.OVERLAY_MESSAGE, new ArrayList<>()),
            Map.entry(IdentifiedLayer.MISC_OVERLAYS, new ArrayList<>())
    );

    private static boolean initialized = false;

    public static ResourceLocation getResourceLocationFromString(String layer) {
        return switch (layer) {
            case "BOSS_BAR" -> IdentifiedLayer.BOSS_BAR;
            case "CHAT" -> IdentifiedLayer.CHAT;
            case "CROSSHAIR" -> IdentifiedLayer.CROSSHAIR;
            case "DEBUG" -> IdentifiedLayer.DEBUG;
            case "DEMO_TIMER" -> IdentifiedLayer.DEMO_TIMER;
            case "EXPERIENCE_LEVEL" -> IdentifiedLayer.EXPERIENCE_LEVEL;
            case "HOTBAR_AND_BARS" -> IdentifiedLayer.HOTBAR_AND_BARS;
            case "TITLE_AND_SUBTITLE" -> IdentifiedLayer.TITLE_AND_SUBTITLE;
            case "SUBTITLES" -> IdentifiedLayer.SUBTITLES;
            case "STATUS_EFFECTS" -> IdentifiedLayer.STATUS_EFFECTS;
            case "SLEEP" -> IdentifiedLayer.SLEEP;
            case "SCOREBOARD" -> IdentifiedLayer.SCOREBOARD;
            case "PLAYER_LIST" -> IdentifiedLayer.PLAYER_LIST;
            case "OVERLAY_MESSAGE" -> IdentifiedLayer.OVERLAY_MESSAGE;
            case "MISC_OVERLAYS" -> IdentifiedLayer.MISC_OVERLAYS;
            default -> null;
        };
    }

    private final ServerManager serverManager;

    public LayerManager(ServerManager serverManager) {
        if (!initialized) {
            throw new IllegalStateException("LayerManager constructor called before LayerManager.initialize()!");
        }

        this.serverManager = serverManager;

        ClientPlayNetworking.registerReceiver(RegisterLayerS2CPayload.TYPE, (payload, context) -> {
            ResourceLocation layer = getResourceLocationFromString(payload.layer());
            if (layer == null) return;

            registerLayer(layer, payload.resource(), payload.x(), payload.y(), payload.width(), payload.height(), payload.enabled());
        });

        ClientPlayNetworking.registerReceiver(UnregisterLayerS2CPayload.TYPE, (payload, context) -> {
            ResourceLocation layer = getResourceLocationFromString(payload.layer());
            if (layer == null) return;

            unregisterLayer(layer, payload.resource());
        });

        ClientPlayNetworking.registerReceiver(EnableLayerS2CPayload.TYPE, (payload, context) -> {
            ResourceLocation layer = getResourceLocationFromString(payload.layer());
            if (layer == null) return;

            enableLayer(layer, payload.resource());
        });

        ClientPlayNetworking.registerReceiver(DisableLayerS2CPayload.TYPE, (payload, context) -> {
            ResourceLocation layer = getResourceLocationFromString(payload.layer());
            if (layer == null) return;

            disableLayer(layer, payload.resource());
        });

        // TODO: Set width/height/position of browser layer
        // TODO: Allow duplicate layers
    }

    public void dispose() {
        ClientPlayNetworking.unregisterReceiver(RegisterLayerS2CPayload.REGISTER_LAYER_PAYLOAD_TYPE);
        ClientPlayNetworking.unregisterReceiver(UnregisterLayerS2CPayload.UNREGISTER_LAYER_PAYLOAD_TYPE);
        ClientPlayNetworking.unregisterReceiver(EnableLayerS2CPayload.ENABLE_LAYER_PAYLOAD_TYPE);
        ClientPlayNetworking.unregisterReceiver(DisableLayerS2CPayload.DISABLE_LAYER_PAYLOAD_TYPE);

        for (ArrayList<BrowserLayer> browserLayers : layers.values()) {
            for (BrowserLayer browserLayer : browserLayers) {
                browserLayer.close();
            }

            browserLayers.clear();
        }
    }

    public void disableLayer(ResourceLocation layer, String resource) {
        ArrayList<BrowserLayer> browserLayers = layers.get(layer);
        if (browserLayers == null) return;

        for (BrowserLayer browserLayer : browserLayers) {
            if (!browserLayer.getResource().equals(resource)) continue;
            browserLayer.disable();
        }
    }

    public void enableLayer(ResourceLocation layer, String resource) {
        ArrayList<BrowserLayer> browserLayers = layers.get(layer);
        if (browserLayers == null) return;

        for (BrowserLayer browserLayer : browserLayers) {
            if (!browserLayer.getResource().equals(resource)) continue;
            browserLayer.enable();
        }
    }

    public void unregisterLayer(ResourceLocation layer, String resource) {
        ArrayList<BrowserLayer> browserLayers = layers.get(layer);
        if (browserLayers == null) return;

        browserLayers.removeIf(browserLayer -> browserLayer.getResource().equals(resource));
    }

    public void registerLayer(ResourceLocation layer, String resource, int x, int y, int width, int height, boolean enabled) {
        ArrayList<BrowserLayer> browserLayers = layers.get(layer);
        if (browserLayers == null) return;

        // Prevent duplicate layers
        if (browserLayers.stream().anyMatch(browserLayer -> browserLayer.getResource().equals(resource))) {
            return;
        }

        browserLayers.add(new BrowserLayer(serverManager, resource, x, y, width, height, enabled));
    }

    public static void initialize() {
        if (initialized) return;

        PayloadTypeRegistry.playS2C().register(RegisterLayerS2CPayload.TYPE, RegisterLayerS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UnregisterLayerS2CPayload.TYPE, UnregisterLayerS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnableLayerS2CPayload.TYPE, EnableLayerS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DisableLayerS2CPayload.TYPE, DisableLayerS2CPayload.CODEC);

        HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> {
            for (ResourceLocation layer : layers.keySet()) {
                layeredDrawer.attachLayerAfter(layer, BROWSER_LAYER, (GuiGraphics graphics, DeltaTracker deltaTracker) -> {
                    LayerManager.render(layer, graphics, deltaTracker);
                });
            }
        });

        initialized = true;
    }

    private static void render(ResourceLocation layer, GuiGraphics graphics, DeltaTracker deltaTracker) {
        ArrayList<BrowserLayer> browserLayers = layers.get(layer);
        if (browserLayers == null || browserLayers.isEmpty()) return;

        // Render all enabled browser layers for the specified layer
        for (BrowserLayer browserLayer : browserLayers) {
            if (!browserLayer.isEnabled()) continue;
            browserLayer.render(graphics, deltaTracker);
        }
    }

    public void postMessage(String message) {
        for (ResourceLocation layer : layers.keySet()) {
            ArrayList<BrowserLayer> browserLayers = layers.get(layer);
            if (browserLayers == null || browserLayers.isEmpty()) continue;

            for (BrowserLayer browserLayer : browserLayers) {
                if (!browserLayer.isEnabled()) continue;
                browserLayer.postMessage(message);
            }
        }
    }
}
