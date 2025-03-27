# Best Practices for Developing Scripts with Spectra

This guide outlines best practices for developing scripts using Spectra, a powerful scripting framework for Bukkit plugins. By following these guidelines, you can create efficient, maintainable, and robust scripts for your Minecraft server.

## Performance Optimization

1. **Minimize event listener usage:**
   Only register event listeners when necessary, and remove them when they're no longer needed. Excessive event listeners can impact server performance.

   ```javascript
   // Good practice: Remove event listener when it's no longer needed
   const listener = (event) => {
     // Handle event
     removeEventListener("playerJoin", listener);
   };
   addEventListener("playerJoin", listener);
   ```

2. **Use efficient data structures:**
   Choose appropriate data structures for your use case. For example, use Sets for unique collections and Maps for key-value pairs.

   ```javascript
   // Good practice: Using a Set for unique player names
   const uniquePlayers = new Set();
   ```

3. **Avoid blocking operations:**
   Perform long-running or blocking operations asynchronously to prevent server lag.

   ```javascript
   // Good practice: Using setTimeout for delayed execution
   setTimeout(() => {
     // Perform resource-intensive operation
   }, 0);
   ```

## Error Handling

1. **Use try-catch blocks:**
   Wrap potentially error-prone code in try-catch blocks to gracefully handle exceptions.

   ```javascript
   try {
     // Potentially error-prone code
   } catch (error) {
     console.error("An error occurred:", error.message);
   }
   ```

2. **Validate input:**
   Always validate user input and function parameters to prevent unexpected behavior.

   ```javascript
   function damagePlayer(player, amount) {
     if (typeof amount !== "number" || amount < 0) {
       throw new Error("Invalid damage amount");
     }
     // Apply damage to player
   }
   ```

3. **Provide meaningful error messages:**
   When throwing errors or logging issues, include descriptive messages to aid in debugging.

   ```javascript
   if (!player.hasPermission("admin")) {
     throw new Error("Player does not have admin permissions");
   }
   ```

## Code Organization

1. **Modularize your code:**
   Split your script into smaller, reusable functions and modules for better maintainability.

   ```javascript
   // economy.js
   export function getBalance(player) {
     // Implementation
   }

   export function addMoney(player, amount) {
     // Implementation
   }

   // main.js
   import { getBalance, addMoney } from "./economy.js";
   ```

2. **Use consistent naming conventions:**
   Adopt a consistent naming style for variables, functions, and classes throughout your scripts.

   ```javascript
   // Good practice: Consistent camelCase for functions and variables
   function calculateTotalDamage(baseDamage, multiplier) {
     const critChance = 0.1;
     // Implementation
   }
   ```

3. **Comment your code:**
   Add meaningful comments to explain complex logic or non-obvious behavior.

   ```javascript
   // Calculate damage based on player's equipment and buffs
   function calculateDamage(player) {
     let baseDamage = player.getWeaponDamage();
     
     // Apply strength buff if active
     if (player.hasEffect("strength")) {
       baseDamage *= 1.5;
     }
     
     // Reduce damage if player is weakened
     if (player.hasEffect("weakness")) {
       baseDamage *= 0.8;
     }
     
     return baseDamage;
   }
   ```

## Example of a Well-Structured Script

Here's an example of a well-structured script that incorporates the best practices mentioned above:

```javascript
// playerUtils.js
export function isPlayerAdmin(player) {
  return player.hasPermission("admin");
}

export function giveWelcomeKit(player) {
  // Implementation
}

// main.js
import { isPlayerAdmin, giveWelcomeKit } from "./playerUtils.js";

const WELCOME_MESSAGE = "Welcome to the server!";

function onPlayerJoin(event) {
  const player = event.getPlayer();
  
  try {
    console.log(`Player ${player.getName()} joined the server`);
    
    player.sendMessage(WELCOME_MESSAGE);
    
    if (isPlayerAdmin(player)) {
      player.sendMessage("You have admin privileges");
    }
    
    // Delay welcome kit to avoid overwhelming new players
    setTimeout(() => {
      giveWelcomeKit(player);
    }, 5000);
  } catch (error) {
    console.error(`Error handling player join: ${error.message}`);
  }
}

addEventListener("playerJoin", onPlayerJoin);

export function onEnable() {
  console.log("Script enabled successfully");
}

export function onDisable() {
  console.log("Script disabled");
}
```

By following these best practices and structuring your scripts well, you'll create more efficient, maintainable, and robust code for your Spectra-powered Bukkit plugins.