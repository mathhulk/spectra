package com.mathhulk.spectra.browser;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ExternalLinkHandler extends CefRequestHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ExternalLinkHandler.class);
    public String host;

    public ExternalLinkHandler(String host) {
        this.host = host;
    }

    public boolean isExternalLink(String url) {
        // Parse the URL into components
        try {
            URI uri = new URI(url);

            return (uri.getScheme().equals("http") || uri.getScheme().equals("https")) && !uri.getHost().equals(host);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
        if (isExternalLink(request.getURL())) {
            try {
                String os = System.getProperty("os.name").toLowerCase();

                if (os.contains("mac")) {
                    // macOS specific handling
                    String[] cmd = {"open", request.getURL()};
                    Runtime.getRuntime().exec(cmd);
                } else if (os.contains("win")) {
                    // Windows specific handling
                    String[] cmd = {"cmd.exe", "/c", "start", request.getURL()};
                    Runtime.getRuntime().exec(cmd);
                } else {
                    // Linux or other OS
                    String[] cmd = {"xdg-open", request.getURL()};
                    Runtime.getRuntime().exec(cmd);
                }

//                Desktop.getDesktop().browse(new URI(request.getURL()));
            } catch (Exception e) {
                log.error(e.toString());
            }

            return true;
        }

        return super.onBeforeBrowse(browser, frame, request, user_gesture, is_redirect);
    }
}
