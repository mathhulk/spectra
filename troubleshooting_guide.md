# Troubleshooting Guide

This guide will help you diagnose and resolve common issues you might encounter when using Spectra. We'll cover script loading problems, configuration errors, and performance issues.

## Script Loading Problems

### Scripts Not Being Detected

If your scripts are not being detected by Spectra:

1. Ensure your scripts are in the correct directory: `<server_root>/scripts/`
2. Check that your script files have the correct extension: `.js` or `.mjs`
3. Verify that the `ScriptManager` is enabled in the Spectra configuration

If the issue persists:

```java
// Check if ScriptManager is null
if (scriptManager == null) {
    getLogger().severe("ScriptManager is not initialized");
    return;
}

// Log the number of detected scripts
getLogger().info("Detected scripts: " + scriptManager.getScripts().size());
```

### Script Syntax Errors

If a script fails to load due to syntax errors:

1. Check the server console for error messages
2. Review the script for any syntax issues
3. Test the script in a JavaScript runtime outside of Spectra to isolate the problem

## Configuration Errors

### Plugin Not Loading

If the Spectra plugin isn't loading:

1. Ensure the plugin JAR file is in the server's `plugins` folder
2. Check the server startup logs for any Spectra-related error messages
3. Verify that the plugin is compatible with your server version

### ScriptPluginLoader Issues

If you're experiencing problems with `ScriptPluginLoader`:

1. Make sure Spectra is properly registered in the server's `plugin.yml`
2. Check if the `ScriptPluginLoader` is being registered correctly:

```java
PluginManager pluginManager = getServer().getPluginManager();
pluginManager.registerInterface(ScriptPluginLoader.class);
```

3. Verify that Spectra plugins are being detected:

```java
int totalPlugins = pluginLoader == null ? 0 : pluginLoader.getPlugins().size();
getLogger().info("Loaded " + totalPlugins + " Spectra plugins");
```

## Performance Issues

### High CPU Usage

If you're experiencing high CPU usage:

1. Check which scripts are currently running using the `/scripts list` command
2. Review your scripts for infinite loops or heavy computations
3. Consider using async operations for time-consuming tasks:

```javascript
// Example of using async operations
Bukkit.getScheduler().runTaskAsynchronously(plugin, () => {
    // Perform heavy computation here
    // Use Bukkit.getScheduler().runTask() to run code on the main thread when needed
});
```

### Memory Leaks

To identify and resolve memory leaks:

1. Use a profiling tool like VisualVM to monitor memory usage
2. Check for scripts that create large data structures without proper cleanup
3. Ensure that event listeners are properly unregistered when scripts are disabled:

```javascript
// Example of proper event listener cleanup
const listener = (event) => {
    // Event handling code
};

plugin.registerEvent("PlayerJoinEvent", listener);

// Cleanup when the script is disabled
plugin.on("disable", () => {
    HandlerList.unregisterAll(listener);
});
```

## Advanced Troubleshooting

For more complex issues:

1. Enable debug logging in Spectra's configuration
2. Review the `plugins/Spectra/logs` directory for detailed log files
3. Use the `/scripts reload` command to reload scripts without restarting the server

If you continue to experience issues after trying these troubleshooting steps, please report the problem on our GitHub issue tracker, providing as much detail as possible about your setup and the steps to reproduce the issue.