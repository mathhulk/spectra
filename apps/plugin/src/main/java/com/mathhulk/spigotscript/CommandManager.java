package com.mathhulk.spigotscript;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommandManager {
    private final Script script;

    private final ArrayList<Command> commands = new ArrayList<>();

    CommandManager(Script script) {
        this.script = script;
    }

    /**
     * Syncs commands on the main thread using reflection
     */
    private void syncCommands() {
        JavaPlugin plugin = getScript().getPlugin();

        Runnable runnable = () -> {
            try {
                Server server = plugin.getServer();
                Method syncCommandsMethod = server.getClass().getDeclaredMethod("syncCommands");
                syncCommandsMethod.setAccessible(true);
                syncCommandsMethod.invoke(server);
                syncCommandsMethod.setAccessible(false);
            } catch (Exception e) {
                throw new RuntimeException("Failed to sync commands", e);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            runnable.run();

            return;
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    /**
     * Gets the commandMap field using reflection
     */
    private CommandMap getCommandMap() {
        try {
            SimplePluginManager pluginManager = (SimplePluginManager) script.getPlugin().getServer().getPluginManager();
            Field commandMapField = pluginManager.getClass().getDeclaredField("commandMap");

            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(pluginManager);
            commandMapField.setAccessible(false);

            return commandMap;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get commandMap", e);
        }
    }

    /**
     * Gets the knownCommands field using reflection
     */
    private  HashMap<String, Command> getKnownCommands() {
        try {
            CommandMap commandMap = getCommandMap();
            Field knownCommandsField = commandMap.getClass().getSuperclass().getDeclaredField("knownCommands");

            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            HashMap<String, Command> knownCommands = (HashMap<String, Command>) knownCommandsField.get(commandMap);
            knownCommandsField.setAccessible(false);

            return knownCommands;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get knownCommands", e);
        }
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
            getKnownCommands().remove(command.getName());
            // command.unregister(getCommandMap());
            syncCommands();

            script.getPlugin().getLogger().info("Removed command: " + command.getName());
        } catch (Exception e) {
            script.getPlugin().getLogger().severe("Failed to remove command: " + command.getName());

            e.printStackTrace();
        }
    }

    public Command addCommand(String name, ExecutorFunction executor, TabCompleterFunction... optionalParameters) {
        if (optionalParameters.length > 1) {
            throw new IllegalArgumentException("Expected at most 3 arguments but found " + (optionalParameters.length + 2));
        }

        if (optionalParameters.length == 0) {
            return registerCommand(name, executor, null);
        }

        return registerCommand(name, executor, optionalParameters[0]);
    }

    private Command registerCommand(String name, ExecutorFunction executor, TabCompleterFunction tabCompleter) {
        try {
            Command command = new Command(name) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    return executor.apply(sender, this, label, args);
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    if (tabCompleter == null) {
                        return new ArrayList<>();
                    }

                    return tabCompleter.apply(sender, this, alias, args);
                }
            };

            getKnownCommands().put(name, command);
            // getCommandMap().register(script.getName(), command);
            syncCommands();

            commands.add(command);

            script.getPlugin().getLogger().info("Added command: " + name);

            return command;
        } catch (Exception e) {
            script.getPlugin().getLogger().severe("Failed to add command: " + name);

            e.printStackTrace();

            return null;
        }
    }

    public Script getScript() {
        return script;
    }

    @FunctionalInterface
    public interface ExecutorFunction {
        boolean apply(CommandSender sender, Command command, String label, String[] args);
    }

    @FunctionalInterface
    public interface TabCompleterFunction {
        List<String> apply(CommandSender sender, Command command, String alias, String[] args);
    }

    @FunctionalInterface
    public interface RemoveCommandFunction {
        void apply(Command command);
    }

    @FunctionalInterface
    public interface AddCommandFunction {
        Command apply(String name, ExecutorFunction executor, TabCompleterFunction... tabCompleter);
    }
}

