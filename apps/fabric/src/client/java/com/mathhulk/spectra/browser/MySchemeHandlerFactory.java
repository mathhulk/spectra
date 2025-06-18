package com.mathhulk.spectra.browser;

import com.mathhulk.spectra.ui.ResourceManager;
import com.mathhulk.spectra.ui.ServerManager;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class MySchemeHandlerFactory implements CefSchemeHandlerFactory  {
    private final ServerManager serverManager;

    public MySchemeHandlerFactory(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        return new ScreenResourceHandler(serverManager);
    }
}