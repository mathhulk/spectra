package com.mathhulk.spectra;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class CommandManager {
  private final Script script;

  private final ArrayList<Command> commands = new ArrayList<>();

  CommandManager(Script script) {
    this.script = script;
  }

  public void removeCommands() {
    for (Command command : commands) {
      removeCommand(command);
    }
  }

  public void removeCommand(Command command) {
    if (!commands.contains(command)) {
      return;
    }

    try {
      ProxyServer.getInstance().getPluginManager().unregisterCommand(command);

      script.getPlugin().getLogger().info("Removed command: " + command.getName());
    } catch (Exception e) {
      script.getPlugin().getLogger().severe("Failed to remove command: " + command.getName());

      e.printStackTrace();
    }
  }

  public void addCommand(String name, ExecutorFunction executor, TabCompleterFunction... optionalParameters) {
    if (optionalParameters.length > 1) {
      throw new IllegalArgumentException("Expected at most 3 arguments but found " + (optionalParameters.length + 2));
    }

    if (optionalParameters.length == 0) {
      registerCommand(name, executor, null);

      return;
    }

    registerCommand(name, executor, optionalParameters[0]);
  }

  private void registerCommand(String name, ExecutorFunction executor, TabCompleterFunction tabCompleter) {
    try {
      class ScriptCommand extends Command implements TabExecutor {
        public ScriptCommand(String name) {
          super(name);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
          executor.apply(sender, args);
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String[] args) {
          if (tabCompleter == null) {
            return new ArrayList<>();
          }

          return tabCompleter.apply(sender, args);
        }
      }

      Command command = new ScriptCommand(name);

      ProxyServer.getInstance().getPluginManager().registerCommand(script.getPlugin(), command);

      script.getPlugin().getLogger().info("Added command: " + name);
    } catch (Exception e) {
      script.getPlugin().getLogger().severe("Failed to add command: " + name);

      e.printStackTrace();
    }
  }

  public Script getScript() {
    return script;
  }

  @FunctionalInterface
  public interface ExecutorFunction {
    boolean apply(CommandSender sender, String[] args);
  }

  @FunctionalInterface
  public interface TabCompleterFunction {
    List<String> apply(CommandSender sender, String[] args);
  }

  @FunctionalInterface
  public interface RemoveCommandFunction {
    void apply(Command command);
  }

  @FunctionalInterface
  public interface AddCommandFunction {
    void apply(String name, ExecutorFunction executor, TabCompleterFunction... tabCompleter);
  }
}
