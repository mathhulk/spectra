package com.mathhulk.spectra.browser;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.cinemamod.mcef.MCEFClient;
import com.mathhulk.spectra.ui.ResourceManager;
import com.mathhulk.spectra.ui.ServerManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.network.chat.Component;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefMessageRouter;

import java.net.URI;
import java.util.function.Supplier;

public class BrowserScreen extends Screen {
    private final ServerManager serverManager;
    private final String resource;
    private final Screen parent;

    private MCEFBrowser browser;

    public BrowserScreen(ServerManager serverManager, String resource) {
        this(serverManager, resource, null);
    }

    public BrowserScreen(ServerManager serverManager, String resource, Screen parent) {
        super(Component.empty());

        this.serverManager = serverManager;
        this.resource = resource;
        this.parent = parent;
    }

    public void postMessage(String message) {
        if (browser == null) return;
        browser.getMainFrame().executeJavaScript("window.postMessage('" + message + "', '*');", browser.getURL(), 0);
    }

    @Override
    protected void init() {
        super.init();

        if (browser == null) {
            CefApp appHandle = MCEF.getApp().getHandle();
            CefClient clientHandle = MCEF.getClient().getHandle();

            // Communicate between Java and JavaScript
            CefMessageRouter router = CefMessageRouter.create();
            router.addHandler(new BrowserMessageRouterHandler(), true);
            clientHandle.addMessageRouter(router);

            // Open external links in the system browser
            clientHandle.addRequestHandler(new ExternalLinkHandler());

            // Load local resources
            appHandle.registerSchemeHandlerFactory("ui", "server", new MySchemeHandlerFactory(serverManager));

            browser = MCEF.createBrowser("ui://server/" + resource, true);

            resizeBrowser();
        }
    }

    private int mouseX(double x) {
        return (int) (x * Minecraft.getInstance().getWindow().getGuiScale());
    }

    private int mouseY(double y) {
        return (int) (y * Minecraft.getInstance().getWindow().getGuiScale());
    }

    private int scaleX(double x) {
        return (int) (x * Minecraft.getInstance().getWindow().getGuiScale());
    }

    private int scaleY(double y) {
        return (int) (y * Minecraft.getInstance().getWindow().getGuiScale());
    }

    private void resizeBrowser() {
        browser.resize(scaleX(width), scaleY(height));
    }

    @Override
    public void resize(Minecraft minecraft, int i, int j) {
        super.resize(minecraft, i, j);
        resizeBrowser();
    }

    @Override
    public void onClose() {
        browser.close();
        super.onClose();

        if (parent != null) {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        // super.render(guiGraphics, i, j, f);

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, browser.getRenderer().getTextureID());
        Tesselator t = Tesselator.getInstance();
        BufferBuilder buffer = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(0, height, 0).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(width, height, 0).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(width, 0, 0).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255);
        buffer.addVertex(0, 0, 0).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255);
        BufferUploader.drawWithShader(buffer.build());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        browser.sendMousePress(mouseX(mouseX), mouseY(mouseY), button);
        browser.setFocus(true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        browser.sendMouseRelease(mouseX(mouseX), mouseY(mouseY), button);
        browser.setFocus(true);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        browser.sendMouseMove(mouseX(mouseX), mouseY(mouseY));
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        browser.sendMouseWheel(mouseX(mouseX), mouseY(mouseY), scrollY, 0);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        browser.sendKeyPress(keyCode, scanCode, modifiers);
        browser.setFocus(true);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        browser.sendKeyRelease(keyCode, scanCode, modifiers);
        browser.setFocus(true);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint == (char) 0) return false;
        browser.sendKeyTyped(codePoint, modifiers);
        browser.setFocus(true);
        return super.charTyped(codePoint, modifiers);
    }
}