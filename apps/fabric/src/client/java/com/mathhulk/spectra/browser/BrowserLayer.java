package com.mathhulk.spectra.browser;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFClient;
import com.mathhulk.spectra.ui.ResourceManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CoreShaders;
import org.cef.CefApp;

public class BrowserLayer {
    private ScaledBrowser browser;

    private final ResourceManager resourceManager;

    public BrowserLayer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void initialize() {
        if (browser != null) return;

        CefApp appHandle = MCEF.getApp().getHandle();
        MCEFClient client = MCEF.getClient();
        appHandle.registerSchemeHandlerFactory("ui", "menu", new MySchemeHandlerFactory(resourceManager));

        browser = new ScaledBrowser(client, "ui://menu/test-menu.html", true);

        resize(100, 100);
    }

    private void resize(int width, int height) {
        if (browser == null) return;

        double scaleFactor = Minecraft.getInstance().getWindow().getGuiScale() / ScaledBrowser.getDeviceScaleFactor();

        browser.resize((int) (width * scaleFactor), (int) (height * scaleFactor));
    }

    public void close() {
        if (browser == null) return;

        browser.close();
    }

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        initialize();

        resize(100, 100);

        MCEF.getApp().getHandle().N_DoMessageLoopWork();

        if (browser.getRenderer().getTextureID() == 0) {
            // If the browser texture is not ready, we can skip rendering
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, browser.getRenderer().getTextureID());
        Tesselator t = Tesselator.getInstance();
        BufferBuilder buffer = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(0, 100, 0).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(100, 100, 0).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(100, 0, 0).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(0, 0, 0).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255);
        BufferUploader.drawWithShader(buffer.build());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableDepthTest();
    }
}
