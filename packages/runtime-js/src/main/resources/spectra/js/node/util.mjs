// node:util
const inspectValue = (value, depth = 2, seen = new Set()) => {
  if (typeof value === 'string') return depth < 2 ? `'${value}'` : value;
  if (typeof value !== 'object' || value === null) return typeof value === 'bigint' ? `${value}n` : String(value);
  if (seen.has(value)) return '[Circular]';
  if (value instanceof Error) return value.stack ?? String(value);
  if (depth < 0) return Array.isArray(value) ? '[Array]' : '[Object]';
  seen.add(value);
  const next = (v) => inspectValue(v, depth - 1, seen);
  let out;
  if (Array.isArray(value)) out = `[ ${value.map(next).join(', ')} ]`;
  else if (value instanceof Map) out = `Map(${value.size}) { ${[...value].map(([k, v]) => `${next(k)} => ${next(v)}`).join(', ')} }`;
  else if (value instanceof Set) out = `Set(${value.size}) { ${[...value].map(next).join(', ')} }`;
  else {
    const entries = Object.keys(value).map((k) => `${k}: ${next(value[k])}`);
    out = entries.length ? `{ ${entries.join(', ')} }` : '{}';
  }
  seen.delete(value);
  return out;
};
export function inspect(value, options = {}) { return inspectValue(value, options.depth ?? 2); }
export function format(template, ...args) {
  if (typeof template !== 'string') return [template, ...args].map((v) => inspect(v)).join(' ');
  let i = 0;
  let out = template.replace(/%[sdifjoO%]/g, (token) => {
    if (token === '%%') return '%';
    if (i >= args.length) return token;
    const arg = args[i++];
    switch (token) {
      case '%s': return typeof arg === 'string' ? arg : inspect(arg);
      case '%d': case '%i': return String(token === '%i' ? parseInt(arg) : Number(arg));
      case '%f': return String(parseFloat(arg));
      case '%j': return JSON.stringify(arg);
      default: return inspect(arg);
    }
  });
  for (; i < args.length; i++) out += ` ${typeof args[i] === 'string' ? args[i] : inspect(args[i])}`;
  return out;
}
export function promisify(fn) {
  return (...args) => new Promise((resolve, reject) => fn(...args, (error, value) => (error ? reject(error) : resolve(value))));
}
export function callbackify(fn) {
  return (...args) => { const callback = args.pop(); fn(...args).then((v) => callback(null, v), (e) => callback(e)); };
}
export function deprecate(fn, message) {
  let warned = false;
  return function (...args) { if (!warned) { warned = true; console.warn(`DeprecationWarning: ${message}`); } return fn.apply(this, args); };
}
export function inherits(ctor, superCtor) { Object.setPrototypeOf(ctor.prototype, superCtor.prototype); Object.setPrototypeOf(ctor, superCtor); }
export function isDeepStrictEqual(a, b) { return JSON.stringify(a) === JSON.stringify(b); }
export const types = {
  isPromise: (v) => v instanceof Promise,
  isDate: (v) => v instanceof Date,
  isRegExp: (v) => v instanceof RegExp,
  isUint8Array: (v) => v instanceof Uint8Array,
};
export const TextEncoder = globalThis.TextEncoder;
export const TextDecoder = globalThis.TextDecoder;
export default { inspect, format, promisify, callbackify, deprecate, inherits, isDeepStrictEqual, types, TextEncoder, TextDecoder };
