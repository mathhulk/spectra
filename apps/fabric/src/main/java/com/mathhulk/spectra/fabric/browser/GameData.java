package com.mathhulk.spectra.fabric.browser;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Built-in endpoints that expose what the client already knows, so HUDs such as minimaps need no server round
 * trip:
 *
 * <ul>
 *   <li>{@code /_spectra/game/player}: position, rotation, dimension, health, and time as JSON.</li>
 *   <li>{@code /_spectra/game/map.png?radius=64}: a top-down map of loaded terrain around the player, colored like
 *       vanilla maps, north up, one pixel per block. Unloaded areas are transparent.</li>
 * </ul>
 *
 * <p>Both read the client world, so they must be produced on the client thread.
 */
public final class GameData {
  public static final int MAX_RADIUS = 128;

  private GameData() {
  }

  static Map<String, String> query(String url) {
    Map<String, String> query = new HashMap<>();
    String raw = URI.create(url).getRawQuery();
    if (raw != null) {
      for (String pair : raw.split("&")) {
        int eq = pair.indexOf('=');
        if (eq > 0) {
          query.put(pair.substring(0, eq), java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
      }
    }
    return query;
  }

  public static byte[] player(Minecraft minecraft) {
    LocalPlayer player = minecraft.player;
    ClientLevel level = minecraft.level;
    JsonObject json = new JsonObject();
    if (player != null && level != null) {
      json.addProperty("x", player.getX());
      json.addProperty("y", player.getY());
      json.addProperty("z", player.getZ());
      json.addProperty("yaw", player.getYRot());
      json.addProperty("pitch", player.getXRot());
      json.addProperty("dimension", level.dimension().identifier().toString());
      json.addProperty("health", player.getHealth());
      json.addProperty("maxHealth", player.getMaxHealth());
      json.addProperty("food", player.getFoodData().getFoodLevel());
      json.addProperty("time", level.getOverworldClockTime());
    }
    return json.toString().getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] map(Minecraft minecraft, Map<String, String> query) throws IOException {
    LocalPlayer player = minecraft.player;
    ClientLevel level = minecraft.level;
    int radius = Math.clamp(parse(query.get("radius"), 64), 1, MAX_RADIUS);
    int size = radius * 2 + 1;
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    if (player != null && level != null) {
      int centerX = player.getBlockX();
      int centerZ = player.getBlockZ();
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      for (int dz = -radius; dz <= radius; dz++) {
        int previousY = Integer.MIN_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
          int x = centerX + dx;
          int z = centerZ + dz;
          if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
            previousY = Integer.MIN_VALUE;
            continue;
          }
          int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
          pos.set(x, y, z);
          MapColor color = level.getBlockState(pos).getMapColor(level, pos);
          if (color == MapColor.NONE) {
            continue;
          }
          int northY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) - 1;
          MapColor.Brightness brightness = y > northY ? MapColor.Brightness.HIGH
              : y < northY ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
          image.setRGB(dx + radius, dz + radius, color.calculateARGBColor(brightness));
          previousY = y;
        }
      }
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  private static int parse(String value, int fallback) {
    try {
      return value == null ? fallback : Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
