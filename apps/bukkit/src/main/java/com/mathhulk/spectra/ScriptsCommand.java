package com.mathhulk.spectra;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.mathhulk.spectra.scripts.Script;
import com.mathhulk.spectra.scripts.ScriptManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptsCommand implements CommandExecutor, TabCompleter {
  private final Spectra plugin;

  private final String OPEN_BROWSER_CHANNEL = "spectra:open-browser";

  public ScriptsCommand(Spectra plugin) {
    this.plugin = plugin;

    plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, OPEN_BROWSER_CHANNEL);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
    if (arguments.length == 0) {
      return false;
    }

    String firstArgument = arguments[0];

    if (firstArgument.equals("stop-watching")) {
      ScriptManager scriptManager = plugin.getScriptManager();

      if (!scriptManager.isWatching()) {
        sender.sendMessage("Not watching for scripts.");
        return true;
      }

      scriptManager.stopWatching();
      sender.sendMessage("Stopped watching for scripts.");
    }

    if (firstArgument.equals("start-watching")) {
      ScriptManager scriptManager = plugin.getScriptManager();

      if (scriptManager.isWatching()) {
        sender.sendMessage("Already watching for scripts.");
        return true;
      }

      scriptManager.startWatching();
      sender.sendMessage("Started watching for scripts.");
    }

    if (firstArgument.equals("list")) {
      ArrayList<Script> scripts = plugin.getScriptManager().getScripts();

      ArrayList<String> scriptNames = new ArrayList<>();
      scripts.forEach(script -> scriptNames.add(script.getName()));

      sender.sendMessage("Scripts: " + String.join(", ", scriptNames));
    }

    if (firstArgument.equals("enable")) {
      if (arguments.length < 2) {
        sender.sendMessage("Usage: /scripts enable <script>");
        return true;
      }

      String scriptName = arguments[1];

      Script script = plugin.getScriptManager().getScript(scriptName);

      if (script == null) {
        sender.sendMessage("Script not found: " + scriptName);
        return true;
      }

      script.enable();
      sender.sendMessage("Enabled script: " + scriptName);
    }

    if (firstArgument.equals("disable")) {
      if (arguments.length < 2) {
        sender.sendMessage("Usage: /scripts disable <script>");
        return true;
      }

      String scriptName = arguments[1];

      Script script = plugin.getScriptManager().getScript(scriptName);

      if (script == null) {
        sender.sendMessage("Script not found: " + scriptName);
        return true;
      }

      script.disable();
      sender.sendMessage("Disabled script: " + scriptName);
    }

    if (firstArgument.equals("remove")) {
      if (arguments.length < 2) {
        sender.sendMessage("Usage: /scripts remove <script>");
        return true;
      }

      String scriptName = arguments[1];

      Script script = plugin.getScriptManager().getScript(scriptName);

      if (script == null) {
        sender.sendMessage("Script not found: " + scriptName);
        return true;
      }

      plugin.getScriptManager().removeScript(scriptName);
      sender.sendMessage("Removed script: " + scriptName);
    }

    if (firstArgument.equals("add")) {
      if (arguments.length < 2) {
        sender.sendMessage("Usage: /scripts add <script>");
        return true;
      }

      String scriptName = arguments[1];

      Script existingScript = plugin.getScriptManager().getScript(scriptName);

      if (existingScript != null) {
        sender.sendMessage("Script already exists: " + scriptName);
        return true;
      }

      Script script = plugin.getScriptManager().addScript(scriptName, true);

      if (script == null) {
        sender.sendMessage("Failed to add script: " + scriptName);
        return true;
      }

      sender.sendMessage("Added script: " + scriptName);
    }

    if (firstArgument.equals("sync")) {
      ScriptManager scriptManager = plugin.getScriptManager();
      scriptManager.disable();

      // TODO: Add a flag to disable watching by default
      scriptManager.load(true);

      scriptManager.enable();
    }

    if (firstArgument.equals("open")) {
      if (!(sender instanceof Player)) {
        sender.sendMessage("This command can only be used by players.");

        return true;
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);

      try {
        dos.writeInt(0);
        dos.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }

      byte[] message = baos.toByteArray();

      ((Player) sender).sendPluginMessage(plugin, OPEN_BROWSER_CHANNEL, message);
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      return List.of("start-watching", "stop-watching", "list", "enable", "disable", "remove", "add", "open");
    }

    if (args.length == 2 && List.of("enable", "disable", "remove", "add").contains(args[0])) {
      ArrayList<Script> scripts = plugin.getScriptManager().getScripts();

      ArrayList<String> scriptNames = new ArrayList<>();
      scripts.forEach(script -> scriptNames.add(script.getName()));

      return scriptNames;
    }

    return new ArrayList<>();
  }
}
