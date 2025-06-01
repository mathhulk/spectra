package com.mathhulk.spectra.browser;

import com.mathhulk.spectra.ui.ResourceManager;
import net.minecraft.client.Minecraft;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScreenResourceHandler implements CefResourceHandler {
    private static final Logger log = LoggerFactory.getLogger(ScreenResourceHandler.class);
    private ByteArrayInputStream inputStream;
    private String mimeType;
    private int responseLength;

    private final ResourceManager resourceManager;

    public ScreenResourceHandler(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
//
//    @Override
//    public boolean processRequest(CefRequest request, CefCallback callback) {
//        String url = request.getURL();
//        String requestPath = url.substring("ui://menu/".length());
//
//        if (resourceManager.isPathInvalid(requestPath)) {
//            // Invalid path, continue without processing
//            callback.Continue();
//            return false;
//        }
//
//        // Resolve the file path based on the request
//        Path filePath = resourceManager.getResourcesPath().resolve(requestPath).normalize();
//
//        log.info(filePath.toString());
//
//        // Read the file content
//        String content;
//        try {
//            content = Files.readString(filePath, StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            // Handle file not found or read error
//            callback.Continue();
//            return false;
//        }
//
//        String fileName = filePath.getFileName().toString();
//
//        if (fileName.endsWith(".html")) {
//            mimeType = "text/html";
//        } else if (fileName.endsWith(".css")) {
//            mimeType = "text/css";
//        } else if (fileName.endsWith(".js")) {
//            mimeType = "application/javascript";
//        } else if (fileName.endsWith(".json")) {
//            mimeType = "application/json";
//        } else if (fileName.endsWith(".png")) {
//            mimeType = "image/png";
//        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
//            mimeType = "image/jpeg";
//        } else if (fileName.endsWith(".gif")) {
//            mimeType = "image/gif";
//        } else if (fileName.endsWith(".svg")) {
//            mimeType = "image/svg+xml";
//        } else if (fileName.endsWith(".xml")) {
//            mimeType = "application/xml";
//        } else if (fileName.endsWith(".woff") || fileName.endsWith(".woff2")) {
//            mimeType = "font/woff";
//        } else if (fileName.endsWith(".ttf")) {
//            mimeType = "font/ttf";
//        } else if (fileName.endsWith(".otf")) {
//            mimeType = "font/otf";
//        } else if (fileName.endsWith(".mp4")) {
//            mimeType = "video/mp4";
//        } else if (fileName.endsWith(".webm")) {
//            mimeType = "video/webm";
//        } else if (fileName.endsWith(".ogg")) {
//            mimeType = "audio/ogg";
//        } else if (fileName.endsWith(".mp3")) {
//            mimeType = "audio/mpeg";
//        } else if (fileName.endsWith(".wav")) {
//            mimeType = "audio/wav";
//        } else if (fileName.endsWith(".ico")) {
//            mimeType = "image/x-icon";
//        } else if (fileName.endsWith(".pdf")) {
//            mimeType = "application/pdf";
//        } else if (fileName.endsWith(".zip")) {
//            mimeType = "application/zip";
//        } else if (fileName.endsWith(".csv")) {
//            mimeType = "text/csv";
//        } else if (fileName.endsWith(".md")) {
//            mimeType = "text/markdown";
//        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
//            mimeType = "application/x-yaml";
//        } else if (fileName.endsWith(".webp")) {
//            mimeType = "image/webp";
//        } else if (fileName.endsWith(".avif")) {
//            mimeType = "image/avif";
//        } else if (fileName.endsWith(".flac")) {
//            mimeType = "audio/flac";
//        } else if (fileName.endsWith(".aac")) {
//            mimeType = "audio/aac";
//        } else {
//            mimeType = "text/plain"; // Default fallback MIME type
//        }
//
//        data = content.getBytes(StandardCharsets.UTF_8);
//        offset = 0;
//        callback.Continue();
//        return true;
//    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL();
        String requestPath = url.substring("ui://menu/".length());

        if (resourceManager.isPathInvalid(requestPath)) {
            // Invalid path, continue without processing
            callback.Continue();
            return false;
        }

        // Resolve the file path based on the request
        Path filePath = resourceManager.getResourcesPath().resolve(requestPath).normalize();

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
