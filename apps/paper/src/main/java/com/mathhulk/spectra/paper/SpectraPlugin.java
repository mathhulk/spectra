package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.api.Spectra;
import com.mathhulk.spectra.core.SpectraConfig;
import com.mathhulk.spectra.core.SpectraServer;
import com.mathhulk.spectra.core.scripts.ScriptManager;
import com.mathhulk.spectra.paper.world.ScriptChunkGenerator;
import com.mathhulk.spectra.protocol.Protocol;
import com.mathhulk.spectra.runtime.js.JsLanguage;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.ServicePriority;
import org.graalvm.polyglot.Value;
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

    ScriptChunkGenerator.serverThread(serverThread(), () -> Bukkit.getWorlds().stream()
        .map(world -> chunkTasks(world.getUID())).filter(java.util.Objects::nonNull).toList(), serverTasks());
    Spectra.Holder.set(server);
    getServer().getServicesManager().register(Spectra.class, server, this, ServicePriority.Normal);

    Path serverDirectory = getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
    javascript = new JsLanguage(platform.mainThread(), getLogger(), getClassLoader(), serverDirectory, server,
        Bukkit::getPlayer);
    if (javascript.compilerAvailable()) {
      getLogger().info("Scripts run on " + javascript.engineDescription() + " with the Graal compiler.");
    } else {
      getLogger().info("Scripts run on " + javascript.engineDescription() + " in interpreter mode. "
          + "For compiled performance, run the server on GraalVM (see the Spectra docs).");
    }

    Path scriptDirectory = serverDirectory.resolve("scripts");
    scripts = new ScriptManager(platform, scriptDirectory, List.of(javascript), scope -> Map.of(
        "addEventListener", ScriptEvents.add(this, scope),
        "removeEventListener", ScriptEvents.remove(),
        "addCommand", ScriptCommands.add(scope, getLogger()),
        "removeCommand", ScriptCommands.remove(scope, getLogger()),
        "spectra", new SpectraApi(server, scope, javascript.engineDescription()),
        "plugin", this,
        "server", getServer()));

    server.start();
    if (getConfig().getBoolean("types.generate", true)) {
      // After every plugin has enabled, so their APIs are included.
      Bukkit.getScheduler().runTask(this, () -> Bukkit.getScheduler().runTaskAsynchronously(this,
          () -> TypeGeneration.run(this, serverDirectory, getLogger())));
    }
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
    if (server != null) {
      getServer().getServicesManager().unregister(Spectra.class, server);
      Spectra.Holder.clear(server);
    }
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
        config.getInt("resources.bytes-per-tick", defaults.resourceBytesPerTick()),
        config.getStringList("client.network-allow"));
  }

  /**
   * World generators come from scripts exporting {@code getDefaultWorldGenerator(worldName, id)} (returning a
   * generator object), selected in bukkit.yml with {@code generator: Spectra} or {@code generator: Spectra:<id>}.
   * The first script that returns a generator wins.
   */
  @Override
  public ChunkGenerator getDefaultWorldGenerator(@NonNull String worldName, String id) {
    for (ScriptManager.Export export : scripts == null ? List.<ScriptManager.Export>of()
        : scripts.exports("getDefaultWorldGenerator")) {
      Value generator = export.scope().invoke("getDefaultWorldGenerator", () -> {
        Value factory = (Value) export.value();
        Value result = factory.canExecute() ? factory.execute(worldName, id) : factory;
        return result == null || result.isNull() ? null : result;
      }, null);
      if (generator != null) {
        getLogger().info("World " + worldName + " uses the generator from " + export.script());
        return new ScriptChunkGenerator(export.scope(), generator, getLogger());
      }
    }
    return super.getDefaultWorldGenerator(worldName, id);
  }

  /** Like {@link #getDefaultWorldGenerator}, for scripts exporting {@code getDefaultBiomeProvider(worldName, id)}. */
  @Override
  public BiomeProvider getDefaultBiomeProvider(@NonNull String worldName, String id) {
    for (ScriptManager.Export export : scripts == null ? List.<ScriptManager.Export>of()
        : scripts.exports("getDefaultBiomeProvider")) {
      Value provider = export.scope().invoke("getDefaultBiomeProvider", () -> {
        Value factory = (Value) export.value();
        Value result = factory.canExecute() ? factory.execute(worldName, id) : factory;
        return result == null || result.isNull() ? null : result;
      }, null);
      if (provider != null) {
        return ScriptChunkGenerator.biomeProvider(export.scope(), provider, getLogger());
      }
    }
    return super.getDefaultBiomeProvider(worldName, id);
  }

  /**
   * The Minecraft server's own task queue, which it drains while blocking on chunk loads (unlike the Bukkit
   * scheduler, which only runs between ticks). Falls back to the Bukkit scheduler if unavailable.
   */
  private java.util.concurrent.Executor serverTasks() {
    try {
      Object minecraft = getServer().getClass().getMethod("getServer").invoke(getServer());
      if (minecraft instanceof java.util.concurrent.Executor executor) {
        return executor;
      }
    } catch (ReflectiveOperationException e) {
      getLogger().log(Level.FINE, "Minecraft server executor unavailable", e);
    }
    return task -> Bukkit.getScheduler().runTask(this, task);
  }

  /** Minecraft's server thread; plugins loading at STARTUP are not necessarily enabled on it. */
  private Thread serverThread() {
    try {
      Object minecraft = getServer().getClass().getMethod("getServer").invoke(getServer());
      return (Thread) minecraft.getClass().getMethod("getRunningThread").invoke(minecraft);
    } catch (ReflectiveOperationException | ClassCastException e) {
      getLogger().log(Level.WARNING, "Could not find the server thread; world generation hooks may block", e);
      return Thread.currentThread();
    }
  }

  private final java.util.Map<java.util.UUID, java.util.concurrent.Executor> chunkExecutors =
      new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * The executor a world's chunk cache drains on the server thread while it waits for a chunk
   * ({@code ServerChunkCache.mainThreadProcessor}); null if the world is not loaded yet.
   */
  private java.util.concurrent.Executor chunkTasks(java.util.UUID worldId) {
    java.util.concurrent.Executor cached = chunkExecutors.get(worldId);
    if (cached != null) {
      return cached;
    }
    org.bukkit.World world = Bukkit.getWorld(worldId);
    if (world == null) {
      return null;
    }
    try {
      Object level = world.getClass().getMethod("getHandle").invoke(world);
      Object chunkSource = level.getClass().getMethod("getChunkSource").invoke(level);
      Object processor = chunkSource.getClass().getField("mainThreadProcessor").get(chunkSource);
      if (processor instanceof java.util.concurrent.Executor executor) {
        chunkExecutors.put(worldId, executor);
        return executor;
      }
    } catch (ReflectiveOperationException e) {
      getLogger().log(Level.FINE, "Chunk executor unavailable for " + world.getName(), e);
    }
    return null;
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
