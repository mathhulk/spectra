---
outline: deep
---

# What is Spectra?

Spectra is a plugin for Minecraft: Java Edition servers and a [software development kit](#) (SDK) for building experiences in Minecraft using JavaScript or [TypeScript](https://www.typescriptlang.org/). Spectra utilizes [GraalJS](https://www.graalvm.org/latest/reference-manual/js/) to parse and run scripts written in JavaScript and enables them to build on top of Minecraft server software APIs written in Java like [Bukkit](https://dev.bukkit.org/) or [BungeeCord](https://www.spigotmc.org/wiki/bungeecord/).

::: code-group

```js [JavaScript]
addEventListener(org.bukkit.event.player.PlayerJoinEvent, (event) => {
  const player = event.getPlayer();

  const name = player.getName();
  console.log(name + " left the server");
});
```

```java [Java]
...
```

:::

::: tip Already familiar with JavaScript and Minecraft servers?
Jump to our [SDK](#) and have a script running on a Minecraft server in less than 5 minutes.
:::

## Why?

Because of community-driven projects like [Spigot](https://www.spigotmc.org/wiki/about-spigot/) and [Paper](https://papermc.io/), Minecraft: Java Edition servers can be be extended by writing plugins in Java using the Bukkit API. The Bukkit API exposes underlying game logic to developers, enabling them to manipulate the environment and players. The Bukkit API even powers servers like [Hypixel](https://hypixel.net/play/).

Developing plugins in Java will yield the best performance for Minecraft: Java Edition servers and the best experience for players at scale. However, Java can sometimes be hard to learn and tedious to work with. After each modification, plugins must be compiled and the server must be restarted for modifications to be reflected in-game.

Context matters. Not every experience needs to be built from the ground up to support a server under the constant load of hundreds of players or withstand modifying thousands of blocks per second. Spectra trades a mostly negligent [loss in performance](#) for a gradual learning curve and a far better developer experience.

## Use cases

Whether you have little to no experience with JavaScript or plugin development hoping to learn, or you are an experienced developer looking to speed up everyday workflows, Spectra might be right for you.

Spectra aims to bring familiar concepts from [Node.js](https://nodejs.org/en) and the web to Minecraft, enabling developers with prior JavaScript experience to dive right in or new developers to apply what they learn outside of Spectra and Minecraft.

::: danger Spectra does replace Java plugins
If you are trying to build experiences that support thousands of players or integrate closely with server software, or are looking to build a plugin you can distribute for others to use, Spectra might not be for you. Spectra does not and will not cover every use case.
:::

Each JavaScript code snippet in the Spectra documentation will be accompanied by a TypeScript code snippet and a Java code snippet. If you are interested in understanding how plugins are built in Java, you can always reference the Java equivalent. Should you ever decide to build a plugin in Java, Spectra hopefully gives you a head start.

### Learn to code with Minecraft

Spectra uses Minecraft to introduce JavaScript, a programming language built for the web and everything else, and TypeScript, a strongly typed programming language built on JavaScript.

With Spectra, scripts are easy to write and easy to understand. Spectra scripts do not have to be built or compiled and modifications will reflect instantly without restarting the Minecraft server.

::: code-group

```js [JavaScript]
console.log("Loaded!");

export const onEnable = () => {
  console.log("Enabled!");
};

export const onDisable = () => {
  console.log("Disabled!");
};
```

```java [Java]
package com.example.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {
  @Override
  public void onLoad() {
    getLogger().info("Loaded!");
  }

  @Override
  public void onEnable() {
    getLogger().info("Enabled!");
  }

  @Override
  public void onDisable() {
    getLogger().info("Disabled!");
  }
}
```

:::

### Scripts for common tasks

...

### Games and experiences

...
