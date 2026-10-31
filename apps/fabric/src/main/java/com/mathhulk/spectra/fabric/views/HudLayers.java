package com.mathhulk.spectra.fabric.views;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registers one HUD element after each vanilla element (plus one on top) that draws the Spectra layers attached
 * there. Layers name their slot with {@code after}, e.g. "chat" or "hotbar".
 */
public final class HudLayers {
  private static final Map<String, Identifier> SLOTS = Map.ofEntries(
      Map.entry("misc_overlays", VanillaHudElements.MISC_OVERLAYS),
      Map.entry("crosshair", VanillaHudElements.CROSSHAIR),
      Map.entry("hotbar", VanillaHudElements.HOTBAR),
      Map.entry("health", VanillaHudElements.HEALTH_BAR),
      Map.entry("food", VanillaHudElements.FOOD_BAR),
      Map.entry("experience", VanillaHudElements.EXPERIENCE_LEVEL),
      Map.entry("effects", VanillaHudElements.MOB_EFFECTS),
      Map.entry("boss_bar", VanillaHudElements.BOSS_BAR),
      Map.entry("scoreboard", VanillaHudElements.SCOREBOARD),
      Map.entry("title", VanillaHudElements.TITLE_AND_SUBTITLE),
      Map.entry("chat", VanillaHudElements.CHAT),
      Map.entry("player_list", VanillaHudElements.PLAYER_LIST),
      Map.entry("subtitles", VanillaHudElements.SUBTITLES));

  private HudLayers() {
  }

  /** Normalized slot name for a layer's {@code after} value; null (on top) when absent or unknown. */
  static String slotFor(String after) {
    if (after == null) {
      return null;
    }
    String key = after.toLowerCase(Locale.ROOT);
    return SLOTS.containsKey(key) ? key : null;
  }

  public static void register(Supplier<ViewManager> views) {
    SLOTS.forEach((name, vanilla) -> HudElementRegistry.attachElementAfter(vanilla,
        Identifier.fromNamespaceAndPath("spectra", "after_" + name), (graphics, delta) -> {
          ViewManager manager = views.get();
          if (manager != null) {
            manager.drawLayers(graphics, name);
          }
        }));
    HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("spectra", "top"), (graphics, delta) -> {
      ViewManager manager = views.get();
      if (manager != null) {
        manager.drawLayers(graphics, null);
      }
    });
  }
}
