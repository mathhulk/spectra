package com.mathhulk.spectra.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.mathhulk.spectra.Spectra;

public class ScreenManager implements PluginMessageListener {
  public static final String RESOURCES_CHANNEL = "spectra:resources";
  public static final String RESOURCE_CHANNEL = "spectra:resource";

  private final Spectra plugin;
  private final File resourcesFolder;

  public ScreenManager(Spectra plugin) {
    this.plugin = plugin;
    this.resourcesFolder = new File(plugin.getDataFolder(), "resources");
  }

  @Override
  public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    if (!channel.equals(RESOURCES_CHANNEL)) {
      return;
    }

    plugin.getLogger().info("Received resources from player " + player.getName());

    Bukkit.getScheduler().runTask(plugin, () -> {
      HashMap<Integer, String> resources = new HashMap<>();

      try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(message))) {
        int size = readVarInt(dis);
        plugin.getLogger().info("Received " + size + " resources from player " + player.getName());
        for (int i = 0; i < size; i++) {
          int key = dis.readInt();
          plugin.getLogger().info("Resource key length: " + key);

          int valueLength = readVarInt(dis);
          byte[] valueBytes = new byte[valueLength];
          dis.readFully(valueBytes);
          String value = new String(valueBytes, StandardCharsets.UTF_8);

          resources.put(key, value);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      for (Integer id : resources.keySet()) {
        String filePath = resources.get(id);

        if (isPathInvalid(filePath)) {
          continue;
        }

        File file = new File(resourcesFolder, filePath);
        if (!file.exists()) {
          continue;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
          byte[] buffer = new byte[16384];
          int bytesRead;
          int chunkIndex = 0;
          int chunkCount = (int) Math.ceil((double) file.length() / buffer.length);

          while ((bytesRead = fis.read(buffer)) != -1) {
            byte[] chunk = new byte[bytesRead];
            System.arraycopy(buffer, 0, chunk, 0, bytesRead);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(id);
            dos.writeInt(chunkCount);
            dos.writeInt(chunkIndex++);
            writeVarInt(dos, chunk.length);
            dos.write(chunk);
            dos.flush();

            plugin.getLogger().info(
                "Sending resource chunk " + chunkIndex + " for file " + filePath + " to player " + player.getName());
            player.sendPluginMessage(plugin, RESOURCE_CHANNEL, baos.toByteArray());
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private boolean isPathInvalid(String filePath) {
    Path targetPath = resourcesFolder.toPath().resolve(filePath).normalize();
    return !targetPath.toString().startsWith(resourcesFolder.getPath());
  }

  private void fillResources(HashMap<String, String> resources, File file) {
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        fillResources(resources, child);
      }

      return;
    }

    try {
      String fileId = getFileId(file);
      String fileHash = getFileHash(file);
      resources.put(fileId, fileHash);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String getFileId(File file) {
    return file.getPath().substring(resourcesFolder.getPath().length() + 1)
        .replace(File.separator, "/");
  }

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

  private HashMap<String, String> getResources() {
    if (!resourcesFolder.exists()) {
      resourcesFolder.mkdirs();
    }

    HashMap<String, String> resources = new HashMap<>();
    fillResources(resources, resourcesFolder);

    return resources;
  }

  public void enable() {
    plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, RESOURCES_CHANNEL, this);
    plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, RESOURCES_CHANNEL);
    plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, RESOURCE_CHANNEL);
  }

  public void disable() {
    plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, RESOURCES_CHANNEL, this);
    plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, RESOURCE_CHANNEL);
    plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, RESOURCE_CHANNEL);
  }

  private static int readVarInt(DataInputStream in) throws IOException {
    int value = 0;
    int position = 0;

    while (true) {
      byte currentByte = in.readByte();
      value |= (currentByte & 0x7F) << position;

      if ((currentByte & 0x80) == 0) {
        break;
      }

      position += 7;
      if (position >= 32) {
        throw new IOException("VarInt too big");
      }
    }

    return value;
  }

  private static void writeVarInt(DataOutputStream out, int value) throws IOException {
    while ((value & -128) != 0) {
      out.writeByte((value & 127) | 128);
      value >>>= 7;
    }
    out.writeByte(value);
  }

  public void load(Player player) {
    HashMap<String, String> resources = getResources();

    plugin.getLogger().info("Loading resources for player " + player.getName() + ": " + resources.size() + " files: " +
        resources.entrySet().toString());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);

    try {
      writeVarInt(dos, resources.size());

      for (Map.Entry<String, String> entry : resources.entrySet()) {
        byte[] keyBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = entry.getValue().getBytes(StandardCharsets.UTF_8);

        writeVarInt(dos, keyBytes.length);
        dos.write(keyBytes);

        writeVarInt(dos, valueBytes.length);
        dos.write(valueBytes);
      }

      dos.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }

    byte[] message = baos.toByteArray();
    player.sendPluginMessage(plugin, RESOURCES_CHANNEL, message);
  }
}
