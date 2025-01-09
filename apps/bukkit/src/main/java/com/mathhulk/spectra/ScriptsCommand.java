package com.mathhulk.spectra;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ScriptsCommand implements CommandExecutor, TabCompleter {
  private final Spectra spectra;

  public ScriptsCommand(Spectra spectra) {
    this.spectra = spectra;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
    if (arguments.length == 0) {
      return false;
    }

    String firstArgument = arguments[0];

    if (firstArgument.equals("stop-watching")) {
      ScriptManager scriptManager = spectra.getScriptManager();

      if (!scriptManager.isWatching()) {
        sender.sendMessage("Not watching for scripts.");
        return true;
      }

      scriptManager.stopWatching();
      sender.sendMessage("Stopped watching for scripts.");
    }

    if (firstArgument.equals("start-watching")) {
      ScriptManager scriptManager = spectra.getScriptManager();

      if (scriptManager.isWatching()) {
        sender.sendMessage("Already watching for scripts.");
        return true;
      }

      scriptManager.startWatching();
      sender.sendMessage("Started watching for scripts.");
    }

    if (firstArgument.equals("list")) {
      ArrayList<Script> scripts = spectra.getScriptManager().getScripts();

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

      Script script = spectra.getScriptManager().getScript(scriptName);

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

      Script script = spectra.getScriptManager().getScript(scriptName);

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

      Script script = spectra.getScriptManager().getScript(scriptName);

      if (script == null) {
        sender.sendMessage("Script not found: " + scriptName);
        return true;
      }

      spectra.getScriptManager().removeScript(scriptName);
      sender.sendMessage("Removed script: " + scriptName);
    }

    if (firstArgument.equals("add")) {
      if (arguments.length < 2) {
        sender.sendMessage("Usage: /scripts add <script>");
        return true;
      }

      String scriptName = arguments[1];

      Script existingScript = spectra.getScriptManager().getScript(scriptName);

      if (existingScript != null) {
        sender.sendMessage("Script already exists: " + scriptName);
        return true;
      }

      Script script = spectra.getScriptManager().addScript(scriptName, true);

      if (script == null) {
        sender.sendMessage("Failed to add script: " + scriptName);
        return true;
      }

      sender.sendMessage("Added script: " + scriptName);
    }

    if (firstArgument.equals("sync")) {
      ScriptManager scriptManager = spectra.getScriptManager();
      scriptManager.disable();
      scriptManager.enable(true);
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      return List.of("start-watching", "stop-watching", "list", "enable", "disable", "remove", "add");
    }

    if (args.length == 2 && List.of("enable", "disable", "remove", "add").contains(args[0])) {
      ArrayList<Script> scripts = spectra.getScriptManager().getScripts();

      ArrayList<String> scriptNames = new ArrayList<>();
      scripts.forEach(script -> scriptNames.add(script.getName()));

      return scriptNames;
    }

    return new ArrayList<>();
  }
}
