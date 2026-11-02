# Node.js compatibility

Spectra resolves Node builtins to its own implementations, both with and without the `node:` prefix (`node:path` and `path`), so code written for Node, Bun or Deno imports them unchanged.

| Module | Coverage |
|---|---|
| `node:path` (`path/posix`) | Full POSIX API: `join`, `resolve`, `relative`, `dirname`, `basename`, `extname`, `parse`, `format`, ... |
| `node:fs`, `node:fs/promises` | `readFile`, `writeFile`, `appendFile`, `mkdir`, `readdir` (with `withFileTypes`), `stat`, `lstat`, `rm`, `unlink`, `rename`, `copyFile`, `realpath`, `exists`/`access`. Sync, callback and promise forms |
| `node:events` | `EventEmitter` (`on`, `once`, `off`, `prependListener`, ...) and `once()` |
| `node:util` | `format`, `inspect`, `promisify`, `callbackify`, `deprecate`, `inherits`, `isDeepStrictEqual`, `types` |
| `node:os` | `platform`, `arch`, `cpus`, `totalmem`, `freemem`, `homedir`, `tmpdir`, `hostname`, `EOL`, ... |
| `node:buffer` | `Buffer` (a `Uint8Array` subclass): `from` (utf8/hex/base64/base64url/latin1), `alloc`, `concat`, `toString`, common reads and writes |
| `node:crypto` | `createHash`, `createHmac`, `randomUUID`, `randomBytes`, `randomInt`, `webcrypto` |
| `node:timers`, `node:timers/promises` | Including promise-based `setTimeout` and `setInterval` |
| `node:url` | `URL`, `URLSearchParams`, `fileURLToPath`, `pathToFileURL` |
| `node:assert` (`assert/strict`) | `ok`, `equal`, `strictEqual`, `deepStrictEqual`, `throws`, `rejects`, ... |
| `node:process` and the `process` global | `env`, `platform`, `arch`, `cwd()`, `nextTick`, `hrtime`, `uptime`, `memoryUsage`, `on`/`emit` |

```ts
import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const file = path.join("plugins", "MyData", "scores.json");
const scores = JSON.parse(await readFile(file, "utf8"));
```

## Differences from Node

- **Working directory.** `process.cwd()`, and the base for relative `fs` paths, is the server directory.
- **Async `fs` isn't truly asynchronous.** It keeps Node's callback and promise contracts, but does the work on the server thread after the current task. Avoid huge files in hot paths.
- **`process.exit()` throws.** Scripts can't stop the server.
- **Missing modules:** `http`, `https`, `net`, `tls`, `dgram`, `child_process`, `worker_threads`, `stream`, `zlib` and `vm`. Use `fetch` and `WebSocket` for networking.
- **npm packages** work when they only need the modules above. The SDK bundles your dependencies with esbuild.
