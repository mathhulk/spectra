---
title: Spectra Configuration Guide
description: Learn how to configure Spectra, including server settings, Java options, and plugin-specific configurations.
---

# Spectra Configuration Guide

This guide provides detailed information on how to configure Spectra, including server settings, Java options, and plugin-specific configurations. We'll explain the purpose of each configuration option and provide examples to help you set up your Spectra environment.

## Table of Contents

1. [Configuration File](#configuration-file)
2. [Configuration Options](#configuration-options)
   - [Build Options](#build-options)
   - [Server Options](#server-options)
   - [Java Options](#java-options)
   - [JAR Options](#jar-options)
3. [Examples](#examples)

## Configuration File

Spectra uses a configuration file to define various settings for your Minecraft server. By default, Spectra looks for a configuration file named `spectra.config.mjs` or `spectra.config.js` in the current working directory.

To create a configuration file, use the `defineConfig` function provided by Spectra:

```javascript
import { defineConfig } from 'spectra-sdk';

export default defineConfig({
  // Your configuration options here
});
```

## Configuration Options

The Spectra configuration object supports the following options:

### Build Options

- `build`: (Optional) An object containing esbuild options for building your Spectra plugins.

### Server Options

- `server`: (Optional) An object containing server-specific options:
  - `eula`: (Boolean, default: `false`) Set to `true` to accept the [Minecraft EULA](https://aka.ms/MinecraftEULA). You must accept the EULA before running a server.
  - `dir`: (String, default: `".minecraft"`) The directory where the server files will be stored.
  - `type`: (String, default: `"spigot"`) The type of Minecraft server to use. Available options are `"spigot"`, `"paper"`, or `"purpur"`.
  - `version`: (String, optional) The Minecraft server version to use. If not specified, the latest version will be used.

### Java Options

- `server.java`: (Optional) An object containing Java-specific options:
  - `path`: (String, default: `"java"`) The path to the Java executable.
  - `args`: (Array of strings, default: `["-Xmx2G", "-Xms2G"]`) Arguments to pass to the Java executable.

### JAR Options

- `server.jar`: (Optional) An object containing JAR-specific options:
  - `path`: (String, optional) The path to a custom server JAR file. If provided, this will be used instead of downloading a server JAR based on the `type` and `version` options.
  - `args`: (Array of strings, default: `["nogui"]`) Arguments to pass to the server JAR.

## Examples

Here are some example configurations to help you get started:

### Basic Configuration

```javascript
import { defineConfig } from 'spectra-sdk';

export default defineConfig({
  server: {
    eula: true,
    type: 'paper',
    version: '1.19.4',
  },
});
```

This configuration sets up a Paper server running version 1.19.4 with the EULA accepted.

### Advanced Configuration

```javascript
import { defineConfig } from 'spectra-sdk';

export default defineConfig({
  build: {
    entryPoints: ['src/index.ts'],
    outfile: 'dist/plugin.js',
  },
  server: {
    eula: true,
    dir: 'my-server',
    type: 'purpur',
    version: '1.20',
    java: {
      path: '/usr/bin/java',
      args: ['-Xmx4G', '-Xms2G'],
    },
    jar: {
      args: ['nogui', 'max-players=50'],
    },
  },
});
```

This advanced configuration:

1. Sets up build options for a Spectra plugin.
2. Creates a Purpur server running version 1.20.
3. Specifies a custom server directory.
4. Uses a specific Java executable with custom memory allocation.
5. Passes additional arguments to the server JAR.

Remember to adjust these configurations based on your specific needs and server requirements.

By using the Spectra configuration system, you can easily customize your Minecraft server environment and ensure consistent settings across your development and production setups.