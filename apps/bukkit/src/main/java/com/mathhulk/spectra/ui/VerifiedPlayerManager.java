package com.mathhulk.spectra.ui;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.mathhulk.spectra.Spectra;

public class VerifiedPlayerManager implements PluginMessageListener, Listener {
  private final String HANDSHAKE_CHANNEL = "spectra:handshake";

  private final Spectra plugin;
  private final ScreenManager screenManager;

  private HashSet<Player> verifiedPlayers = new HashSet<>();
  private boolean enabled = false;

  public VerifiedPlayerManager(Spectra plugin) {
    this.plugin = plugin;
    this.screenManager = new ScreenManager(plugin);
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    if (!channel.equals(HANDSHAKE_CHANNEL)) {
      return;
    }

    verifiedPlayers.add(player);

    Bukkit.getScheduler().runTask(plugin, () -> {
      // TODO: Queue content downloads
      screenManager.load(player);
    });
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      boolean isVerified = isVerified(player);

      plugin.getLogger().info("Player " + player.getName() + " is verified: " + isVerified);

      if (isVerified) {
        player.sendMessage("Thank you for using Spectra!");
      } else {
        player.kickPlayer("You must have Spectra installed to play on this server.");
      }
    }, 20);
  }

  public boolean isVerified(Player player) {
    return verifiedPlayers.contains(player);
  }

  public void enable() {
    plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, HANDSHAKE_CHANNEL, this);
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    screenManager.enable();
    this.enabled = true;
  }

  public void disable() {
    screenManager.disable();
    plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, HANDSHAKE_CHANNEL, this);
    HandlerList.unregisterAll(this);
    verifiedPlayers.clear();
    this.enabled = false;
  }
}
