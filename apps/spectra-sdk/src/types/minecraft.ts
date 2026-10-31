import "./bukkit.js";

declare global {
  /** A registration that can be undone early. Everything a script registers is undone when it unloads. */
  interface Cancellable {
    cancel(): void;
  }

  type EventPriority = "lowest" | "low" | "normal" | "high" | "highest" | "monitor";

  interface CommandOptions {
    /** Lower-case name, `[a-z0-9_-]`. The command is also available as `<script>:<name>`. */
    name: string;
    aliases?: string[];
    description?: string;
    usage?: string;
    permission?: string;
  }

  /** Return `false` to show the command's usage. */
  type CommandHandler = (
    sender: org.bukkit.command.CommandSender,
    command: org.bukkit.command.Command,
    label: string,
    args: string[]
  ) => boolean | void;

  type TabCompleter = (
    sender: org.bukkit.command.CommandSender,
    command: org.bukkit.command.Command,
    alias: string,
    args: string[]
  ) => string[] | void;

  function addCommand(
    nameOrOptions: string | CommandOptions,
    handler: CommandHandler,
    tabCompleter?: TabCompleter
  ): org.bukkit.command.Command;

  function removeCommand(command: org.bukkit.command.Command): void;

  /**
   * Listens for a Bukkit/Paper event. Handlers for asynchronous events (such as chat) run on the event's own
   * thread, like in Java plugins.
   */
  function addEventListener<T extends abstract new (...args: any) => any>(
    event: T,
    handler: (event: InstanceType<T>) => void,
    priority?: EventPriority,
    ignoreCancelled?: boolean
  ): Cancellable;

  function removeEventListener(listener: Cancellable): void;

  /** Delays are in milliseconds and run on the server thread, rounded to the nearest tick (50 ms). */
  function setTimeout<A extends unknown[]>(callback: (...args: A) => void, delay?: number, ...args: A): number;
  function setInterval<A extends unknown[]>(callback: (...args: A) => void, delay?: number, ...args: A): number;
  function clearTimeout(id: number): void;
  function clearInterval(id: number): void;

  type ClientStatus = "PENDING" | "SUPPORTED" | "UNSUPPORTED" | "INCOMPATIBLE";

  /** What Spectra knows about a player's client. */
  interface ClientInfo {
    status(): { name(): ClientStatus };
    isResolved(): boolean;
    /** True when the player has a compatible Spectra client mod. */
    isSupported(): boolean;
    /** True when the client supports a capability such as `"ui.screens"` or `"ui.layers"`. */
    has(capability: string): boolean;
    protocolVersion(): number;
  }

  /** Result of opening a view. */
  type OpenResult = "SENT" | "QUEUED" | "UNSUPPORTED";

  type Anchor =
    | "top_left"
    | "top"
    | "top_right"
    | "left"
    | "center"
    | "right"
    | "bottom_left"
    | "bottom"
    | "bottom_right";

  interface LayerOptions {
    anchor?: Anchor;
    /** Offset from the anchor, in GUI pixels. */
    x?: number;
    y?: number;
    /** Size in GUI pixels; 0 fills that axis. */
    width?: number;
    height?: number;
    zIndex?: number;
    visible?: boolean;
    /** Vanilla HUD element to render after, e.g. "chat" or "hotbar". */
    after?: string;
  }

  /** Spectra's own API: client capabilities and server-driven UIs. */
  const spectra: {
    /** The script engine, e.g. "GraalJS 25.4 (Interpreted)". */
    engine(): string;

    /** Current knowledge about a player's client; status is PENDING until the handshake resolves. */
    client(player: org.bukkit.entity.Player): ClientInfo;

    /**
     * Calls back once the player's client status is known. Use it to require the client mod for a feature, e.g.
     * `if (!info.isSupported()) player.kick(...)`, or to fall back to vanilla UI.
     */
    whenClientResolved(player: org.bukkit.entity.Player, callback: (info: ClientInfo) => void): void;
    onClientResolved(callback: (info: ClientInfo) => void): Cancellable;

    /** Opens a full-screen view of a resource from `plugins/Spectra/resources`. */
    openScreen(player: org.bukkit.entity.Player, viewId: string, resource: string): OpenResult;
    /** Opens a HUD layer. */
    openLayer(
      player: org.bukkit.entity.Player,
      viewId: string,
      resource: string,
      options?: LayerOptions
    ): OpenResult;
    setViewVisible(player: org.bukkit.entity.Player, viewId: string, visible: boolean): boolean;
    closeView(player: org.bukkit.entity.Player, viewId: string): boolean;
    /** Sends a JSON string to one view, or to all of the player's views when `viewId` is "". */
    sendView(player: org.bukkit.entity.Player, viewId: string, json: string): boolean;

    /** Messages from `window.spectra.send(...)` in a view. The JSON is untrusted player input. */
    onViewMessage(handler: (player: org.bukkit.entity.Player, viewId: string, json: string) => void): Cancellable;
    /** View lifecycle: OPENED, CLOSED (including Escape), FAILED. */
    onViewEvent(
      handler: (
        player: org.bukkit.entity.Player,
        viewId: string,
        type: "OPENED" | "CLOSED" | "FAILED",
        detail: string
      ) => void
    ): Cancellable;
  };

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
