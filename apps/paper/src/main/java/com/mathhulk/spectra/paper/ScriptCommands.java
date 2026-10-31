package com.mathhulk.spectra.paper;

import com.mathhulk.spectra.core.platform.Cancellable;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code addCommand(nameOrOptions, handler, tabCompleter?)} for scripts. Commands are registered under a namespace
 * derived from the script name (for example {@code main:hello}) and removed, aliases included, when the script
 * unloads.
 */
final class ScriptCommands {
  @FunctionalInterface
  public interface Handler {
    Object run(CommandSender sender, Command command, String label, String[] args);
  }

  @FunctionalInterface
  public interface TabCompleter {
    Object run(CommandSender sender, Command command, String alias, String[] args);
  }

  @FunctionalInterface
  public interface AddCommand {
    Command apply(Object nameOrOptions, Handler handler, TabCompleter... tabCompleter);
  }

  @FunctionalInterface
  public interface RemoveCommand {
    void apply(Command command);
  }

  private ScriptCommands() {
  }

  /** Lower-case [a-z0-9_] namespace from a script file name such as {@code My-Script.js}. */
  static String namespace(String scriptName) {
    int dot = scriptName.lastIndexOf('.');
    String base = (dot > 0 ? scriptName.substring(0, dot) : scriptName).toLowerCase(Locale.ROOT);
    String cleaned = base.replaceAll("[^a-z0-9_]", "_");
    return cleaned.isEmpty() ? "script" : cleaned;
  }

  static AddCommand add(ScriptScope scope, Logger logger) {
    return (nameOrOptions, handler, tabCompleter) -> {
      if (tabCompleter.length > 1) {
        throw new IllegalArgumentException("addCommand takes at most 3 arguments");
      }
      Options options = Options.parse(nameOrOptions);
      TabCompleter completer = tabCompleter.length == 1 ? tabCompleter[0] : null;

      Command command = new Command(options.name, options.description, options.usage, options.aliases) {
        @Override
        public boolean execute(@NonNull CommandSender sender, @NonNull String label, String @NonNull [] args) {
          if (!testPermission(sender)) {
            return true;
          }
          Object result = scope.invoke("command /" + label, () -> handler.run(sender, this, label, args), true);
          return !Boolean.FALSE.equals(result);
        }

        @Override
        public @NonNull List<String> tabComplete(@NonNull CommandSender sender, @NonNull String alias,
            String @NonNull [] args) {
          if (completer == null) {
            return List.of();
          }
          Object result = scope.invoke("tab completion /" + alias, () -> completer.run(sender, this, alias, args),
              null);
          return toStrings(result);
        }
      };
      if (options.permission != null) {
        command.setPermission(options.permission);
      }

      CommandMap map = Bukkit.getCommandMap();
      map.register(namespace(scope.scriptName()), command);
      sync(logger);
      return trackRemoval(scope, command, logger);
    };
  }

  private static Command trackRemoval(ScriptScope scope, Command command, Logger logger) {
    scope.track(() -> {
      CommandMap map = Bukkit.getCommandMap();
      command.unregister(map);
      map.getKnownCommands().values().removeIf(registered -> registered == command);
      sync(logger);
    });
    return command;
  }

  static RemoveCommand remove(ScriptScope scope, Logger logger) {
    return command -> {
      if (command != null) {
        CommandMap map = Bukkit.getCommandMap();
        command.unregister(map);
        map.getKnownCommands().values().removeIf(registered -> registered == command);
        sync(logger);
      }
    };
  }

  /** Pushes Bukkit command changes into Brigadier and to every client's command tree. */
  private static void sync(Logger logger) {
    try {
      Method syncCommands = Bukkit.getServer().getClass().getMethod("syncCommands");
      syncCommands.invoke(Bukkit.getServer());
    } catch (ReflectiveOperationException e) {
      logger.log(Level.FINE, "Server has no syncCommands(); relying on updateCommands()", e);
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.updateCommands();
    }
  }

  private static List<String> toStrings(Object result) {
    if (result instanceof Collection<?> values) {
      List<String> strings = new ArrayList<>(values.size());
      values.forEach(value -> strings.add(String.valueOf(value)));
      return strings;
    }
    if (result instanceof Object[] values) {
      List<String> strings = new ArrayList<>(values.length);
      for (Object value : values) {
        strings.add(String.valueOf(value));
      }
      return strings;
    }
    return List.of();
  }

  private record Options(String name, String description, String usage, List<String> aliases, String permission) {
    static Options parse(Object value) {
      if (value instanceof String name) {
        return new Options(requireName(name), "", "/" + name, List.of(), null);
      }
      if (!(value instanceof Map<?, ?> map)) {
        throw new IllegalArgumentException("addCommand expects a name or an options object");
      }
      String name = requireName(string(map.get("name")));
      String description = map.get("description") == null ? "" : string(map.get("description"));
      String usage = map.get("usage") == null ? "/" + name : string(map.get("usage"));
      String permission = map.get("permission") == null ? null : string(map.get("permission"));
      return new Options(name, description, usage, toStrings(map.get("aliases")), permission);
    }

    private static String string(Object value) {
      return value == null ? null : String.valueOf(value);
    }

    private static String requireName(String name) {
      if (name == null || !name.matches("[a-z0-9_\\-]{1,64}")) {
        throw new IllegalArgumentException("Command names must match [a-z0-9_-]{1,64}: " + name);
      }
      return name;
    }
  }
}
