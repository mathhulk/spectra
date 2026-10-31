package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.core.clients.ClientInfo;
import com.mathhulk.spectra.core.scripts.ScriptManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/** {@code /spectra}: script, resource, and client status management for operators. */
final class SpectraCommand implements BasicCommand {
  private static final List<String> SUBCOMMANDS = List.of("scripts", "reload", "watch", "resources", "client",
      "info");

  private final SpectraPlugin plugin;

  SpectraCommand(SpectraPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String permission() {
    return "spectra.admin";
  }

  @Override
  public void execute(@NonNull CommandSourceStack source, String @NonNull [] args) {
    CommandSender sender = source.getSender();
    ScriptManager scripts = plugin.scripts();
    String sub = args.length == 0 ? "info" : args[0];
    switch (sub) {
      case "info" -> {
        sender.sendMessage("Spectra " + plugin.getPluginMeta().getVersion() + " on " + plugin.engineDescription());
        sender.sendMessage("Scripts: " + scripts.list().size() + " loaded from " + scripts.directory()
            + (scripts.isWatching() ? " (watching)" : ""));
      }
      case "scripts" -> {
        if (scripts.list().isEmpty()) {
          sender.sendMessage("No scripts loaded.");
        }
        scripts.list().forEach(script -> sender.sendMessage(" - " + script.name() + " [" + script.language() + "] "
            + (script.enabled() ? "enabled" : "disabled")));
      }
      case "reload" -> {
        if (args.length > 1) {
          sender.sendMessage(scripts.reload(args[1]) ? "Reloaded " + args[1] : "Could not load " + args[1]);
        } else {
          scripts.reloadAll();
          sender.sendMessage("Reloaded " + scripts.list().size() + " scripts.");
        }
      }
      case "watch" -> {
        boolean on = args.length < 2 || args[1].equalsIgnoreCase("on");
        if (on) {
          try {
            scripts.watch();
            sender.sendMessage("Watching scripts for changes.");
          } catch (IOException e) {
            sender.sendMessage("Could not watch scripts: " + e.getMessage());
          }
        } else {
          scripts.stopWatching();
          sender.sendMessage("Stopped watching scripts.");
        }
      }
      case "resources" -> {
        plugin.server().rescanResources();
        var snapshot = plugin.server().resources().snapshot();
        sender.sendMessage("Resources: generation " + snapshot.generation() + ", " + snapshot.entries().size()
            + " files (rescan requested).");
      }
      case "client" -> {
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1])
            : sender instanceof Player player ? player : null;
        if (target == null) {
          sender.sendMessage("Usage: /spectra client <player>");
          return;
        }
        ClientInfo info = plugin.server().clients().info(target.getUniqueId());
        sender.sendMessage(target.getName() + ": " + info.status() + info.modVersion().map(v -> " (mod " + v + ")")
            .orElse("") + " " + info.capabilities());
      }
      default -> sender.sendMessage("Usage: /spectra [info|scripts|reload [name]|watch [on|off]|resources|client]");
    }
  }

  @Override
  public @NonNull Collection<String> suggest(@NonNull CommandSourceStack source, String @NonNull [] args) {
    if (args.length <= 1) {
      String prefix = args.length == 0 ? "" : args[0];
      return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
    }
    if (args.length == 2 && args[0].equals("reload")) {
      return plugin.scripts().list().stream().map(ScriptManager.ScriptStatus::name)
          .filter(name -> name.startsWith(args[1])).toList();
    }
    if (args.length == 2 && args[0].equals("watch")) {
      return Stream.of("on", "off").filter(s -> s.startsWith(args[1])).toList();
    }
    if (args.length == 2 && args[0].equals("client")) {
      return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.startsWith(args[1])).toList();
    }
    return List.of();
  }
}
