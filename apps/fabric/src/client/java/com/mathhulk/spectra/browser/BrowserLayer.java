package com.mathhulk.spectra.browser;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mathhulk.spectra.ui.ServerManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CoreShaders;
import org.cef.CefApp;

public class BrowserLayer {
    private MCEFBrowser browser;

    private final ServerManager serverManager;

    private final int width;
    private final int height;
    private final int x;
    private final int y;
    private final String resource;

    private boolean enabled;

    public BrowserLayer(ServerManager serverManager, String resource, int x, int y, int width, int height) {
        this(serverManager, resource, x, y, width, height, false);
    }

    public BrowserLayer(ServerManager serverManager, String resource, int x, int y, int width, int height, boolean enabled) {
        this.serverManager = serverManager;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.resource = resource;
        this.enabled = enabled;

        if (enabled) {
            initialize();
        }
    }

    public void postMessage(String message) {
        if (browser == null) return;
        browser.getMainFrame().executeJavaScript("window.postMessage('" + message + "', '*');", browser.getURL(), 0);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getResource() {
        return resource;
    }

    public void enable() {
        if (enabled) return;
        initialize();
        enabled = true;
    }

    public void disable() {
        if (!enabled) return;
        close();
        enabled = false;
    }

    public void initialize() {
        if (browser != null) return;

        // TODO: Standardize the scheme handler factory registration
        CefApp appHandle = MCEF.getApp().getHandle();
        appHandle.registerSchemeHandlerFactory("ui", "server", new MySchemeHandlerFactory(serverManager));

        browser = MCEF.createBrowser("ui://server/" + resource, true);
        resize(width, height);
    }

    public void close() {
        if (browser == null) return;

        browser.close();
        browser = null;
    }

    private void resize(int width, int height) {
        if (browser == null) return;

        browser.resize((int) (width * Minecraft.getInstance().getWindow().getGuiScale()), (int) (height * Minecraft.getInstance().getWindow().getGuiScale()));
    }

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!enabled) {
            return;
        }

        resize(width, height);
        MCEF.getApp().getHandle().N_DoMessageLoopWork();

        if (browser.getRenderer().getTextureID() == 0) return;

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, browser.getRenderer().getTextureID());
        Tesselator t = Tesselator.getInstance();
        BufferBuilder buffer = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(x, height, 0).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(width, height, 0).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(width, y, 0).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(x, y, 0).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255);
        BufferUploader.drawWithShader(buffer.build());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableDepthTest();
    }
}
