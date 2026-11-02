# Timers

Timers behave like they do on the web, but run on the server's main thread.

```ts
const id = setInterval((name) => console.log(`tick for ${name}`), 1000, "world");
setTimeout(() => clearInterval(id), 10_000);
```

- **Delays are milliseconds**, rounded to the nearest server tick (50 ms) and never shorter than one tick. `setTimeout(fn, 0)` runs on the next tick.
- **Extra arguments** are passed to the callback.
- **`setImmediate`, `queueMicrotask` and Promises** all work. Promise callbacks run right after the current task.
- **Cleanup.** Timers are cancelled when the script unloads.
- **Errors** in a callback are logged and don't stop other timers.
