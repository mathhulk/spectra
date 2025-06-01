package com.mathhulk.spectra.browser;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;

public class BrowserMessageRouterHandler implements CefMessageRouterHandler {
    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
                           boolean persistent, CefQueryCallback callback) {
        System.out.println("JS called Java with: " + request);
        callback.success("Response from Java");
        return true; // handled
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long query_id) {
    }

    @Override
    public void setNativeRef(String s, long l) {

    }

    @Override
    public long getNativeRef(String s) {
        return 0;
    }
}