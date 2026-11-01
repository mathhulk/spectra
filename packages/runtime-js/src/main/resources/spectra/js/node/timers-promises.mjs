// node:timers/promises
export const setTimeout = (ms, value, options = {}) => new Promise((resolve, reject) => {
  if (options.signal?.aborted) return reject(options.signal.reason);
  const id = globalThis.setTimeout(() => resolve(value), ms);
  options.signal?.addEventListener('abort', () => { globalThis.clearTimeout(id); reject(options.signal.reason); }, { once: true });
});
export const setImmediate = (value) => new Promise((resolve) => globalThis.setImmediate(() => resolve(value)));
export async function* setInterval(ms, value) { while (true) { await setTimeout(ms); yield value; } }
export const scheduler = { wait: (ms) => setTimeout(ms), yield: () => setImmediate() };
export default { setTimeout, setImmediate, setInterval, scheduler };
