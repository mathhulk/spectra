package com.mathhulk.spectra.ui;

import com.mathhulk.spectra.ui.payloads.ResourceS2CPayload;
import com.mathhulk.spectra.ui.payloads.ResourcesC2SPayload;
import com.mathhulk.spectra.ui.payloads.ResourcesS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class ResourceManager {
    private final HashMap<Integer, ArrayList<ResourceS2CPayload>> payloads = new HashMap<>();
    private HashMap<Integer, String> resources;

    private float progress = 0F;
    private int resourceCount = 0;
    private String task = "Initializing";

    private final ServerManager serverManager;

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(ResourcesS2CPayload.TYPE, ResourcesS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ResourcesC2SPayload.TYPE, ResourcesC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ResourceS2CPayload.TYPE, ResourceS2CPayload.CODEC);
    }

    public ResourceManager(ServerManager serverManager) {
        this.serverManager = serverManager;

        ClientPlayNetworking.registerGlobalReceiver(ResourcesS2CPayload.TYPE, this::handleResources);
        ClientPlayNetworking.registerGlobalReceiver(ResourceS2CPayload.TYPE, this::handleResource);
    }

    public Path getResourcesPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(
                Path.of("spectra_data", "resources", serverManager.getServer())
        );
    }

    public String getTask() {
        return task;
    }

    public float getProgress() {
        return progress;
    }

    /**
     * Handles the resources payload from the server.
     * It checks if the resources already exist and if they match the expected hash.
     * If not, it adds them to the resources map for further processing.
     *
     * @param payload The resources payload containing file paths and their hashes.
     * @param context The networking context.
     */
    private void handleResources(ResourcesS2CPayload payload, ClientPlayNetworking.Context context) {
        Minecraft.getInstance().setScreen(new LoadingScreen(this, Minecraft.getInstance().screen));

        this.progress = 10;
        this.task = "Syncing resources";

        HashMap<String, String> existingResources = getResources();
        HashMap<String, String> payloadResources = payload.resources();

        HashMap<Integer, String> requestedResources = new HashMap<>();

        // Request resources that are not present or have changed
        for (String filePath : payloadResources.keySet()) {
            if (isPathInvalid(filePath)) {
                continue;
            }

            String currentFileHash = payloadResources.get(filePath);
            String existingFileHash = existingResources.get(filePath);

            existingResources.remove(filePath);

            if (existingFileHash != null && existingFileHash.equals(currentFileHash)) {
                continue;
            }

            int id = requestedResources.size();
            requestedResources.put(id, filePath);
        }

        // Remove existing resources that are no longer referenced
        for (String filePath : existingResources.keySet()) {
            try {
                Path targetPath = getResourcesPath().resolve(filePath).normalize();
                Files.delete(targetPath);
            } catch (IOException e) {
                // TODO: Error handling
            }
        }

        // If there are no resources to request, we can close the loading screen
        if (requestedResources.isEmpty()) {
            this.progress = 100;

            Minecraft.getInstance().setScreen(null);

            return;
        }

        // Request additional resources
        ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(new ResourcesC2SPayload(requestedResources));
        context.responseSender().sendPacket(packet);

        this.progress = 20;
        this.task = "Requesting additional resources";

        resources = requestedResources;
        resourceCount = requestedResources.size();
    }

    /**
     * Handles a single resource payload.
     * If the payload is complete, it writes the resource to the file system.
     * If the payload is a chunk, it accumulates chunks until the full resource is received.
     *
     * @param payload The resource payload containing the resource data.
     * @param context The networking context.
     */
    private void handleResource(ResourceS2CPayload payload, ClientPlayNetworking.Context context) {
        // Check if the resource ID is already known
        ArrayList<ResourceS2CPayload> existingPayloads = payloads.computeIfAbsent(payload.id(), k -> new ArrayList<>());
        existingPayloads.add(payload);

        this.task = "Downloading resource: " + resources.get(payload.id()) + " (" + existingPayloads.size() + "/" + payload.length() + ")";
        this.progress = this.progress + ((float) 1 / payload.length()) * ((float) 80 / resourceCount);

        // If the resource is not complete, we wait for more chunks
        if (existingPayloads.size() != payload.length()) {
            return;
        }

        // If we have all chunks, we write the resource to the file system
        String filePath = resources.get(payload.id());

        int contentLength = 0;
        for (ResourceS2CPayload existingPayload : existingPayloads) {
            contentLength += existingPayload.chunk().length;
        }

        byte[] content = new byte[contentLength];
        int destinationPosition = 0;
        for (ResourceS2CPayload existingPayload : existingPayloads) {
            byte[] chunk = existingPayload.chunk();
            System.arraycopy(chunk, 0, content, destinationPosition, chunk.length);
            destinationPosition += chunk.length;
        }

        // Remove the resource from the known resources and payloads
        payloads.remove(payload.id());
        resources.remove(payload.id());

        // If there are no more resources to process, close the loading screen
        if (resources.isEmpty()) {
            Minecraft.getInstance().setScreen(null);
        }

        boolean success = writePath(filePath, content);
        if (success) return;

        // TODO: Error handling
    }

    public boolean isPathInvalid(String filePath) {
        Path resourcesPath = getResourcesPath();
        Path targetPath = resourcesPath.resolve(filePath).normalize();
        return !targetPath.startsWith(resourcesPath);
    }

    private boolean writePath(String filePath, byte[] content) {
        if (isPathInvalid(filePath)) {
            return false;
        }

        Path targetPath = getResourcesPath().resolve(filePath).normalize();

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content);
            return true;
        } catch (Exception e) {
            // TODO: Error handling
            return false;
        }
    }

    /**
     * Computes the MD5 hash of a file.
     *
     * @param file The file to compute the hash for.
     * @return The MD5 hash of the file as a hex string.
     * @throws IOException              If an I/O error occurs while reading the file.
     * @throws NoSuchAlgorithmException If the MD5 algorithm is not available.
     */
    private String getFileHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = messageDigest.digest();

        // Convert the byte array to hex string
        StringBuilder hexString = new StringBuilder();

        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    /**
     * Recursively fills a map with file paths and their hashes.
     *
     * @param resources The map to fill with file paths and hashes.
     * @param file      The file or directory to process.
     */
    private void fillResources(HashMap<String, String> resources, File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();

            if (files == null) {
                return;
            }

            for (File child : files) {
                fillResources(resources, child);
            }

            return;
        }

        try {
            String fileId = getFileId(file);
            String fileHash = getFileHash(file);
            resources.put(fileId, fileHash);
        } catch (Exception e) {
            // TODO: Error handling
        }
    }

    /**
     * Gets the file ID for a given file.
     * The file ID is the relative path from the resource directory.
     *
     * @param file The file to get the ID for.
     * @return The file ID as a string.
     */
    private String getFileId(File file) {
        return file.getPath().substring(getResourcesPath().toString().length() + 1)
                .replace(File.separator, "/");
    }

    /**
     * Gets a map containing file paths and their hashes.
     * This method is used to retrieve the resources for sending to the server.
     *
     * @return A map of file paths and their hashes.
     */
    private HashMap<String, String> getResources() {
        HashMap<String, String> resources = new HashMap<>();
        fillResources(resources, getResourcesPath().toFile());
        return resources;
    }

    public void dispose() {
        ClientPlayNetworking.unregisterReceiver(ResourceS2CPayload.RESOURCE_PAYLOAD_TYPE);
        ClientPlayNetworking.unregisterReceiver(ResourcesS2CPayload.RESOURCES_PAYLOAD_TYPE);

        resources.clear();
        payloads.clear();
    }
}
