package com.mathhulk.spectra;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class ScriptsCommand extends Command implements TabExecutor {
  private final SpectraBungeeCordPlugin plugin;

  public ScriptsCommand(SpectraBungeeCordPlugin plugin) {
    super("scripts", "spectra.command.scripts");

    this.plugin = plugin;
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    if (arguments.length == 0) {
      return;
    }

    String firstArgument = arguments[0];

    if (firstArgument.equals("stop-watching")) {
      ScriptManager scriptManager = plugin.getScriptManager();

      if (!scriptManager.isWatching()) {
        sender.sendMessage(new TextComponent("Not watching for scripts."));

        return;
      }

      scriptManager.stopWatching();
      sender.sendMessage(new TextComponent("Stopped watching for scripts."));
    }

    if (firstArgument.equals("start-watching")) {
      ScriptManager scriptManager = plugin.getScriptManager();

      if (scriptManager.isWatching()) {
        sender.sendMessage(new TextComponent("Already watching for scripts."));
        return;
      }

      scriptManager.startWatching();
      sender.sendMessage(new TextComponent("Started watching for scripts."));
    }

    if (firstArgument.equals("list")) {
      ArrayList<Script> scripts = plugin.getScriptManager().getScripts();

      ArrayList<String> scriptNames = new ArrayList<>();
      scripts.forEach(script -> scriptNames.add(script.getName()));

      sender.sendMessage(new TextComponent("Scripts: " + String.join(", ", scriptNames)));
    }

    if (firstArgument.equals("enable")) {
      if (arguments.length < 2) {
        sender.sendMessage(new TextComponent("Usage: /scripts enable <script>"));
        return;
      }

      String scriptName = arguments[1];

      Script script = plugin.getScriptManager().getScript(scriptName);

      if (script == null) {
        sender.sendMessage(new TextComponent("Script not found: " + scriptName));
        return;
      }

      script.enable();
      sender.sendMessage(new TextComponent("Enabled script: " + scriptName));
    }

    if (firstArgument.equals("disable")) {
      if (arguments.length < 2) {
        sender.sendMessage(new TextComponent("Usage: /scripts disable <script>"));
        return;
      }

      String scriptName = arguments[1];

      Script script = plugin.getScriptManager().getScript(scriptName);

      if (script == null) {
        sender.sendMessage(new TextComponent("Script not found: " + scriptName));
        return;
      }

      script.disable();
      sender.sendMessage(new TextComponent("Disabled script: " + scriptName));
    }

    if (firstArgument.equals("remove")) {
      if (arguments.length < 2) {
        sender.sendMessage(new TextComponent("Usage: /scripts remove <script>"));
        return;
      }

      String scriptName = arguments[1];

      Script script = plugin.getScriptManager().getScript(scriptName);

      if (script == null) {
        sender.sendMessage(new TextComponent("Script not found: " + scriptName));
        return;
      }

      plugin.getScriptManager().removeScript(scriptName);
      sender.sendMessage(new TextComponent("Removed script: " + scriptName));
    }

    if (firstArgument.equals("add")) {
      if (arguments.length < 2) {
        sender.sendMessage(new TextComponent("Usage: /scripts add <script>"));
        return;
      }

      String scriptName = arguments[1];

      Script existingScript = plugin.getScriptManager().getScript(scriptName);

      if (existingScript != null) {
        sender.sendMessage(new TextComponent("Script already exists: " + scriptName));
        return;
      }

      Script script = plugin.getScriptManager().addScript(scriptName, true);

      if (script == null) {
        sender.sendMessage(new TextComponent("Failed to add script: " + scriptName));
        return;
      }

      sender.sendMessage(new TextComponent("Added script: " + scriptName));
    }

    if (firstArgument.equals("sync")) {
      ScriptManager scriptManager = plugin.getScriptManager();
      scriptManager.disable();
      scriptManager.enable(true);
    }

    return;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      return List.of("start-watching", "stop-watching", "list", "enable", "disable", "remove", "add");
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
