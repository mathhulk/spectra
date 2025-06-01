package com.mathhulk.spectra.browser;

import com.cinemamod.mcef.MCEFBrowser;
import com.cinemamod.mcef.MCEFClient;
import net.minecraft.client.Minecraft;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefScreenInfo;
import org.lwjgl.glfw.GLFW;

public class ScaledBrowser extends MCEFBrowser {

    public ScaledBrowser(MCEFClient client, String url, boolean isTransparent) {
        super(client, url, isTransparent);
        this.setCloseAllowed();
        this.createImmediately();
    }

    public static double getDeviceScaleFactor() {
        long window = Minecraft.getInstance().getWindow().getWindow();

        int[] fbWidth = new int[1];
        int[] fbHeight = new int[1];
        GLFW.glfwGetFramebufferSize(window, fbWidth, fbHeight);

        int[] winWidth = new int[1];
        int[] winHeight = new int[1];
        GLFW.glfwGetWindowSize(window, winWidth, winHeight);

        // Ensure the scale factor is at least 1
        return Math.max(1, Math.min(fbWidth[0] / winWidth[0], fbHeight[0] / winHeight[0]));
    }

    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        screenInfo.device_scale_factor = getDeviceScaleFactor();
//        screenInfo.Set(getDeviceScaleFactor(), 32, 8, false, this.browser_rect_.getBounds(), this.browser_rect_.getBounds());
        return true;
    }
}
