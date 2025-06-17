# Spectra Scripting API Reference

This document provides a comprehensive reference for the Spectra scripting API, including methods for event listening, command creation, and task scheduling.

## Table of Contents

1. [Event Listening](#event-listening)
2. [Command Creation](#command-creation)
3. [Task Scheduling](#task-scheduling)

## Event Listening

The Spectra scripting API allows you to listen for and respond to Bukkit events in your scripts.

### addEventListener

Adds an event listener for a specific Bukkit event.

```javascript
addEventListener(eventClass, callback, [priority])
```

- `eventClass`: The Bukkit event class to listen for (e.g., `org.bukkit.event.player.PlayerJoinEvent`)
- `callback`: A function that will be called when the event occurs
- `priority` (optional): The event priority (default: `NORMAL`)

Example:

```javascript
addEventListener(org.bukkit.event.player.PlayerJoinEvent, (event) => {
  const player = event.getPlayer();
  player.sendMessage("Welcome to the server!");
});
```

### removeEventListener

Removes a previously added event listener.

```javascript
removeEventListener(listener)
```

- `listener`: The listener object returned by `addEventListener`

Example:

```javascript
const listener = addEventListener(org.bukkit.event.player.PlayerQuitEvent, (event) => {
  console.log(`${event.getPlayer().getName()} left the server`);
});

// Later, when you want to remove the listener
removeEventListener(listener);
```

## Command Creation

Spectra allows you to create custom commands that players can use in-game.

### addCommand

Adds a new command to the server.

```javascript
addCommand(options, executor, [tabCompleter])
```

- `options`: An object or string containing command properties
  - If a string, it represents the command name
  - If an object, it can include:
    - `name`: The command name (required)
    - `permission`: The permission required to use the command
    - `aliases`: An array of alternative command names
    - `description`: A description of the command
    - `usage`: Usage instructions for the command
- `executor`: A function that executes when the command is run
- `tabCompleter` (optional): A function that provides tab completion suggestions

Example:

```javascript
addCommand({
  name: "greet",
  permission: "myserver.greet",
  description: "Greet a player",
  usage: "/greet <player>"
}, (sender, command, label, args) => {
  if (args.length < 1) {
    sender.sendMessage("Please specify a player name.");
    return false;
  }
  const targetPlayer = Bukkit.getPlayer(args[0]);
  if (targetPlayer) {
    targetPlayer.sendMessage(`${sender.getName()} says hello!`);
    sender.sendMessage(`You greeted ${targetPlayer.getName()}.`);
  } else {
    sender.sendMessage("Player not found.");
  }
  return true;
}, (sender, command, alias, args) => {
  if (args.length == 1) {
    return Bukkit.getOnlinePlayers().map(player => player.getName());
  }
  return [];
});
```

### removeCommand

Removes a previously added command.

```javascript
removeCommand(command)
```

- `command`: The command object returned by `addCommand`

Example:

```javascript
const greetCommand = addCommand("greet", (sender, command, label, args) => {
  // Command logic here
});

// Later, when you want to remove the command
removeCommand(greetCommand);
```

## Task Scheduling

Spectra provides methods for scheduling tasks to run at specified intervals or after a delay.

### setInterval

Schedules a task to run repeatedly at fixed time intervals.

```javascript
setInterval(callback, delay)
```

- `callback`: The function to be executed at each interval
- `delay`: The time in milliseconds between each execution

Example:

```javascript
const intervalId = setInterval(() => {
  Bukkit.broadcastMessage("This message is sent every 5 minutes.");
}, 5 * 60 * 1000);
```

### clearInterval

Stops a previously scheduled interval task.

```javascript
clearInterval(intervalId)
```

- `intervalId`: The identifier returned by `setInterval`

Example:

```javascript
clearInterval(intervalId);
```

### setTimeout

Schedules a task to run once after a specified delay.

```javascript
setTimeout(callback, delay)
```

- `callback`: The function to be executed after the delay
- `delay`: The time in milliseconds to wait before executing the callback

Example:

```javascript
setTimeout(() => {
  Bukkit.broadcastMessage("This message is sent 10 seconds after the script starts.");
}, 10 * 1000);
```

### clearTimeout

Cancels a previously scheduled timeout task.

```javascript
clearTimeout(timeoutId)
```

- `timeoutId`: The identifier returned by `setTimeout`

Example:

```javascript
const timeoutId = setTimeout(() => {
  // This will never run
}, 5000);

clearTimeout(timeoutId);
```

By using these API methods, you can create powerful and interactive scripts for your Bukkit server using Spectra. Remember to handle errors appropriately and consider performance implications when using these features in your scripts.