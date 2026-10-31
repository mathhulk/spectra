package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.core.SpectraConfig;
import com.mathhulk.spectra.core.SpectraServer;
import com.mathhulk.spectra.core.scripts.ScriptManager;
import com.mathhulk.spectra.protocol.Protocol;
import com.mathhulk.spectra.runtime.js.JsLanguage;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.logging.Level;

/** Paper entry point: wires the Paper platform into core, the GraalJS runtime, and the script globals. */
public final class SpectraPlugin extends JavaPlugin implements Listener, PluginMessageListener {
  private SpectraServer server;
  private JsLanguage javascript;
  private ScriptManager scripts;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    PaperPlatform platform = new PaperPlatform(this);
    try {
      server = new SpectraServer(platform, readConfig(getConfig()));
    } catch (IOException e) {
      getLogger().log(Level.SEVERE, "Could not initialize Spectra data directory", e);
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }

    getServer().getMessenger().registerOutgoingPluginChannel(this, Protocol.CHANNEL);
    getServer().getMessenger().registerIncomingPluginChannel(this, Protocol.CHANNEL, this);
    getServer().getPluginManager().registerEvents(this, this);
    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
        event -> event.registrar().register("spectra", "Manage Spectra scripts and UIs", List.of(),
            new SpectraCommand(this)));

    javascript = new JsLanguage(platform.mainThread(), getLogger(), getClassLoader());
    if (javascript.compilerAvailable()) {
      getLogger().info("Scripts run on " + javascript.engineDescription() + " with the Graal compiler.");
    } else {
      getLogger().info("Scripts run on " + javascript.engineDescription() + " in interpreter mode. "
          + "For compiled performance, run the server on GraalVM (see the Spectra docs).");
    }

    Path scriptDirectory = getServer().getWorldContainer().toPath().resolve("scripts");
    scripts = new ScriptManager(platform, scriptDirectory, List.of(javascript), scope -> Map.of(
        "addEventListener", ScriptEvents.add(this, scope),
        "removeEventListener", ScriptEvents.remove(),
        "addCommand", ScriptCommands.add(scope, getLogger()),
        "removeCommand", ScriptCommands.remove(scope, getLogger()),
        "spectra", new SpectraApi(server, scope, javascript.engineDescription())));

    server.start();
    for (Player player : Bukkit.getOnlinePlayers()) {
      server.join(player.getUniqueId());
    }
    scripts.loadAll();
    scripts.enableAll();
    if (server.config().watchScripts()) {
      try {
        scripts.watch();
      } catch (IOException e) {
        getLogger().log(Level.WARNING, "Could not watch " + scriptDirectory + " for changes", e);
      }
    }
  }

  @Override
  public void onDisable() {
    if (scripts != null) {
      scripts.close();
    }
    if (javascript != null) {
      javascript.close();
    }
    if (server != null) {
      server.close();
    }
  }

  private static SpectraConfig readConfig(FileConfiguration config) {
    SpectraConfig defaults = SpectraConfig.DEFAULTS;
    int minVersion = config.getInt("client.min-protocol-version", 0);
    return new SpectraConfig(
        config.getBoolean("client.require", defaults.requireClient()),
        minVersion > 0 ? OptionalInt.of(minVersion) : OptionalInt.empty(),
        config.getString("client.kick-message", defaults.kickMessage()),
        config.getLong("client.handshake-timeout-ticks", defaults.handshakeTimeoutTicks()),
        config.getBoolean("scripts.watch", defaults.watchScripts()),
        config.getInt("resources.bytes-per-tick", defaults.resourceBytesPerTick()));
  }

  SpectraServer server() {
    return server;
  }

  ScriptManager scripts() {
    return scripts;
  }

  String engineDescription() {
    return javascript.engineDescription();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoin(PlayerJoinEvent event) {
    server.join(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    server.quit(event.getPlayer().getUniqueId());
  }

  @Override
  public void onPluginMessageReceived(@NonNull String channel, @NonNull Player player, byte @NonNull [] message) {
    if (!Protocol.CHANNEL.equals(channel)) {
      return;
    }
    byte[] frame = message.clone();
    server.platform().mainThread().execute(() -> server.handleFrame(player.getUniqueId(), frame));
  }
}
