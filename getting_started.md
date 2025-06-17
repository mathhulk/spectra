---
title: Getting Started with Spectra
description: A comprehensive guide to installing, configuring, and using Spectra for Minecraft server scripting
---

# Getting Started with Spectra

Welcome to Spectra, a powerful scripting framework for Minecraft servers. This guide will walk you through the process of setting up Spectra, configuring your environment, and creating your first script.

## Table of Contents
1. [Installation](#installation)
2. [Configuration](#configuration)
3. [Creating Your First Script](#creating-your-first-script)
4. [Common Use Cases](#common-use-cases)

## Installation

To get started with Spectra, follow these steps:

1. Ensure you have a compatible Minecraft server (Spigot, Paper, or Purpur) installed.
2. Download the latest version of Spectra from the official repository.
3. Place the Spectra JAR file in your server's `plugins` directory.
4. Restart your Minecraft server.

## Configuration

Spectra uses a configuration file to customize its behavior. Create a file named `spectra.config.js` or `spectra.config.mjs` in your server's root directory.

Here's a basic configuration example:

```javascript
import { defineConfig } from 'spectra-sdk';

export default defineConfig({
  server: {
    eula: true,
    dir: '.minecraft',
    type: 'paper',
    version: '1.19.4',
    java: {
      path: 'java',
      args: ['-Xmx4G', '-Xms2G']
    }
  }
});
```

Adjust the configuration options to match your server setup. For more details on available options, refer to the `Config` interface in the `apps/spectra-sdk/src/lib/config.ts` file.

## Creating Your First Script

Spectra allows you to write scripts using JavaScript. Here's how to create your first script:

1. Create a new file with a `.js` extension in your server's `plugins/Spectra/scripts` directory.
2. Open the file in your preferred text editor.
3. Add the following code to create a basic "Hello, World!" script:

```javascript
export function onEnable() {
  console.log("Hello, World! My first Spectra script is running!");
}

export function onDisable() {
  console.log("Goodbye! My first Spectra script is shutting down.");
}

addEventListener("PlayerJoinEvent", (event) => {
  const player = event.getPlayer();
  player.sendMessage("Welcome to the server!");
});
```

4. Save the file and restart your Minecraft server or use the `/scripts reload` command in-game.

This script demonstrates the basic structure of a Spectra script, including `onEnable` and `onDisable` functions, and an event listener for when a player joins the server.

## Common Use Cases

Here are some common use cases and examples to help you get started with Spectra:

### Adding a Custom Command

```javascript
addCommand("greet", (sender, args) => {
  const playerName = args[0] || sender.getName();
  sender.sendMessage(`Hello, ${playerName}!`);
});
```

### Scheduling Tasks

```javascript
// Run a task every 5 minutes (6000 ticks)
const intervalId = setInterval(() => {
  console.log("This message appears every 5 minutes");
}, 6000);

// Run a task after a 10-second delay
setTimeout(() => {
  console.log("This message appears after 10 seconds");
}, 200);
```

### Listening for Multiple Events

```javascript
const eventHandler = (event) => {
  console.log(`${event.getEventName()} occurred!`);
};

addEventListener("BlockBreakEvent", eventHandler);
addEventListener("BlockPlaceEvent", eventHandler);
```

Remember to check the Spectra API documentation for a full list of available functions and events you can use in your scripts.

With this guide, you should now be ready to start developing powerful scripts for your Minecraft server using Spectra. Happy scripting!