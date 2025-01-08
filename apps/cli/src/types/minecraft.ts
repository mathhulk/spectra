import "./bukkit.js";

declare global {
  function addCommand(
    name: string,
    callback: (
      sender: org.bukkit.command.CommandSender,
      command: org.bukkit.command.Command,
      label: string,
      args: string[]
    ) => boolean
  ): org.bukkit.command.Command;

  function addEventListener<
    T extends typeof org.bukkit.event.player.PlayerJoinEvent
  >(
    event: T,
    callback: (event: InstanceType<T>) => void
  ): org.bukkit.event.Listener;

  function removeEventListener(listener: org.bukkit.event.Listener): void;

  function removeCommand(command: org.bukkit.command.Command): void;

  // @ts-expect-error
  const console: {
    log(...args: unknown[]): void;
    info(...args: unknown[]): void;
    debug(...args: unknown[]): void;
    error(...args: unknown[]): void;
    warn(...args: unknown[]): void;
    assert(check: unknown, message: unknown): void;
    clear(): void;
    count(): void;
    countReset(): void;
    group(): void;
    groupEnd(): void;
    time(): void;
    timeLog(): void;
    timeEnd(): void;
  };
}
