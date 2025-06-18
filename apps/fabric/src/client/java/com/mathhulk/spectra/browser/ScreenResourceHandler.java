package com.mathhulk.spectra.browser;

import com.mathhulk.spectra.ui.ServerManager;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScreenResourceHandler implements CefResourceHandler {
    private static final Logger log = LoggerFactory.getLogger(ScreenResourceHandler.class);
    private ByteArrayInputStream inputStream;
    private String mimeType;
    private int responseLength;

    private final ServerManager serverManager;

    public ScreenResourceHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL();
        String requestPath = url.substring("ui://menu/".length());

        if (serverManager.getResourceManager().isPathInvalid(requestPath)) {
            // Invalid path, continue without processing
            callback.Continue();
            return false;
        }

        // Resolve the file path based on the request
        Path filePath = serverManager.getResourceManager().getResourcesPath().resolve(requestPath).normalize();

        try {
            byte[] data = Files.readAllBytes(filePath);
            inputStream = new ByteArrayInputStream(data);
            mimeType = guessMimeType(filePath);
            responseLength = data.length;

            callback.Continue();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLengthOut, StringRef redirectUrlOut) {
        response.setMimeType(mimeType);
        response.setStatus(200);
        responseLengthOut.set(responseLength);
    }

    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesReadOut, CefCallback callback) {
        int actuallyRead = inputStream.read(dataOut, 0, bytesToRead);
        if (actuallyRead <= 0) {
            bytesReadOut.set(0);
            return false; // Done reading
        }
        bytesReadOut.set(actuallyRead);
        return true;
    }

    @Override
    public void cancel() {
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException ignored) {}
    }

    private String guessMimeType(Path filePath) {
        try {
            String mime = Files.probeContentType(filePath);
            return mime != null ? mime : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}
