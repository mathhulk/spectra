# Commands

```ts
addCommand("hello", (sender, command, label, args) => {
  sender.sendMessage(`Hello ${args.join(" ")}`);
});

addCommand(
  {
    name: "heal",
    description: "Heal yourself",
    usage: "/heal",
    aliases: ["h"],
    permission: "myserver.heal",
  },
  (sender) => {
    if (sender instanceof org.bukkit.entity.Player) sender.setHealth(20);
  },
  // Optional tab completion: return the suggestions.
  (sender, command, alias, args) => ["now", "later"],
);
```

- **Names** match `[a-z0-9_-]` (up to 64 characters).
- **Namespaced form.** Each command is also available as `<script>:<name>`, so `main.js` registers `/main:hello` too.
- **Return value.** Returning `false` from the handler shows the usage message.
- **Permissions.** The `permission` option is checked before the handler runs.
- **Cleanup.** Commands and their aliases are removed when the script unloads, and players' command lists update immediately.
- **Removing early.** `removeCommand(command)` takes the value `addCommand` returned.
