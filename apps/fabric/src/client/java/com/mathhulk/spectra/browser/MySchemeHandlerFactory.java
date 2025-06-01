package com.mathhulk.spectra.browser;

import com.mathhulk.spectra.ui.ResourceManager;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class MySchemeHandlerFactory implements CefSchemeHandlerFactory  {
    private final ResourceManager resourceManager;

    public MySchemeHandlerFactory(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        return new ScreenResourceHandler(resourceManager);
    }
}