// Spectra's web-platform and Node.js compatibility layer. Runs before every script, in its own context.
(() => {
  'use strict';
  const N = globalThis.__spectraNative;
  delete globalThis.__spectraNative;
  Object.defineProperty(globalThis, Symbol.for('spectra.native'), { value: N });
  const define = (name, value) =>
    Object.defineProperty(globalThis, name, { value, writable: true, configurable: true, enumerable: false });

  // ---- bytes ----
  const fromHost = (buffer) => {
    if (buffer == null) return new Uint8Array(0);
    try {
      return new Uint8Array(buffer).slice();
    } catch {
      const length = buffer.remaining ? buffer.remaining() : buffer.length;
      const out = new Uint8Array(length);
      for (let i = 0; i < length; i++) out[i] = (buffer.get ? buffer.get(i) : buffer[i]) & 255;
      return out;
    }
  };
  const toHost = (data) => {
    if (data == null) return null;
    if (typeof data === 'string') return data;
    if (data instanceof ArrayBuffer) return data;
    if (ArrayBuffer.isView(data)) return data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength);
    return String(data);
  };
  Object.defineProperty(globalThis, Symbol.for('spectra.bytes'), { value: { fromHost, toHost } });

  // ---- scheduling ----
  define('queueMicrotask', (callback) => { Promise.resolve().then(callback); });
  define('setImmediate', (callback, ...args) => setTimeout(callback, 0, ...args));
  define('clearImmediate', (id) => clearTimeout(id));

  // ---- text ----
  class TextEncoder {
    get encoding() { return 'utf-8'; }
    encode(input = '') {
      const s = String(input);
      const out = [];
      for (let i = 0; i < s.length; i++) {
        let c = s.charCodeAt(i);
        if (c >= 0xd800 && c <= 0xdbff && i + 1 < s.length) {
          const d = s.charCodeAt(i + 1);
          if (d >= 0xdc00 && d <= 0xdfff) { c = 0x10000 + ((c - 0xd800) << 10) + (d - 0xdc00); i++; } else c = 0xfffd;
        } else if (c >= 0xd800 && c <= 0xdfff) c = 0xfffd;
        if (c < 0x80) out.push(c);
        else if (c < 0x800) out.push(0xc0 | (c >> 6), 0x80 | (c & 63));
        else if (c < 0x10000) out.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 63), 0x80 | (c & 63));
        else out.push(0xf0 | (c >> 18), 0x80 | ((c >> 12) & 63), 0x80 | ((c >> 6) & 63), 0x80 | (c & 63));
      }
      return new Uint8Array(out);
    }
  }
  class TextDecoder {
    constructor(label = 'utf-8', options = {}) {
      const normalized = String(label).toLowerCase();
      if (normalized !== 'utf-8' && normalized !== 'utf8') throw new RangeError(`Unsupported encoding: ${label}`);
      this.fatal = !!options.fatal;
      this.ignoreBOM = !!options.ignoreBOM;
    }
    get encoding() { return 'utf-8'; }
    decode(input) {
      if (input == null) return '';
      const bytes = input instanceof Uint8Array ? input
        : ArrayBuffer.isView(input) ? new Uint8Array(input.buffer, input.byteOffset, input.byteLength) : new Uint8Array(input);
      let out = '';
      let i = !this.ignoreBOM && bytes[0] === 0xef && bytes[1] === 0xbb && bytes[2] === 0xbf ? 3 : 0;
      const bad = () => { if (this.fatal) throw new TypeError('Invalid UTF-8'); return 0xfffd; };
      while (i < bytes.length) {
        const b = bytes[i++];
        let c;
        if (b < 0x80) c = b;
        else if (b >= 0xc2 && b < 0xe0 && i < bytes.length) c = ((b & 31) << 6) | (bytes[i++] & 63);
        else if (b >= 0xe0 && b < 0xf0 && i + 1 < bytes.length) { c = ((b & 15) << 12) | ((bytes[i++] & 63) << 6) | (bytes[i++] & 63); }
        else if (b >= 0xf0 && b < 0xf5 && i + 2 < bytes.length) {
          c = ((b & 7) << 18) | ((bytes[i++] & 63) << 12) | ((bytes[i++] & 63) << 6) | (bytes[i++] & 63);
        } else c = bad();
        out += String.fromCodePoint(c);
      }
      return out;
    }
  }
  define('TextEncoder', TextEncoder);
  define('TextDecoder', TextDecoder);
  define('btoa', (text) => {
    const s = String(text);
    const bytes = new Uint8Array(s.length);
    for (let i = 0; i < s.length; i++) {
      const c = s.charCodeAt(i);
      if (c > 255) throw new DOMException('Invalid character', 'InvalidCharacterError');
      bytes[i] = c;
    }
    return N.base64Encode(toHost(bytes));
  });
  define('atob', (text) => {
    const bytes = fromHost(N.buffer(N.base64Decode(String(text))));
    let out = '';
    for (const b of bytes) out += String.fromCharCode(b);
    return out;
  });

  class DOMException extends Error {
    constructor(message = '', name = 'Error') { super(message); this.name = name; }
  }
  if (!globalThis.DOMException) define('DOMException', DOMException);

  // ---- URL ----
  class URLSearchParams {
    #list = [];
    #url = null;
    constructor(init = '') {
      if (typeof init === 'string') this.#parse(init);
      else if (init instanceof URLSearchParams) this.#list = [...init];
      else if (init && typeof init[Symbol.iterator] === 'function') for (const [k, v] of init) this.#list.push([String(k), String(v)]);
      else if (init && typeof init === 'object') for (const k of Object.keys(init)) this.#list.push([k, String(init[k])]);
    }
    static _bind(params, url) { params.#url = url; return params; }
    #parse(query) {
      this.#list = [];
      for (const part of query.replace(/^\?/, '').split('&')) {
        if (!part) continue;
        const eq = part.indexOf('=');
        const decode = (s) => decodeURIComponent(s.replace(/\+/g, ' '));
        this.#list.push(eq < 0 ? [decode(part), ''] : [decode(part.slice(0, eq)), decode(part.slice(eq + 1))]);
      }
    }
    _reset(query) { this.#parse(query); }
    #update() { if (this.#url) this.#url._setSearch(this.toString()); }
    append(k, v) { this.#list.push([String(k), String(v)]); this.#update(); }
    delete(k) { this.#list = this.#list.filter(([key]) => key !== String(k)); this.#update(); }
    get(k) { const e = this.#list.find(([key]) => key === String(k)); return e ? e[1] : null; }
    getAll(k) { return this.#list.filter(([key]) => key === String(k)).map(([, v]) => v); }
    has(k) { return this.#list.some(([key]) => key === String(k)); }
    set(k, v) {
      const key = String(k);
      const i = this.#list.findIndex(([x]) => x === key);
      if (i < 0) this.#list.push([key, String(v)]);
      else { this.#list[i] = [key, String(v)]; this.#list = this.#list.filter(([x], j) => x !== key || j === i); }
      this.#update();
    }
    sort() { this.#list.sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0)); this.#update(); }
    get size() { return this.#list.length; }
    forEach(callback, thisArg) { for (const [k, v] of this.#list) callback.call(thisArg, v, k, this); }
    keys() { return this.#list.map(([k]) => k)[Symbol.iterator](); }
    values() { return this.#list.map(([, v]) => v)[Symbol.iterator](); }
    entries() { return this.#list.map((e) => [...e])[Symbol.iterator](); }
    [Symbol.iterator]() { return this.entries(); }
    toString() {
      const encode = (s) => encodeURIComponent(s).replace(/%20/g, '+');
      return this.#list.map(([k, v]) => `${encode(k)}=${encode(v)}`).join('&');
    }
  }
  class URL {
    #parts;
    #params;
    constructor(input, base) {
      const parts = N.parseUrl(String(input), base === undefined ? null : String(base));
      if (!parts) throw new TypeError(`Invalid URL: ${input}`);
      this.#parts = [...parts];
      this.#params = URLSearchParams._bind(new URLSearchParams(this.#parts[6]), this);
    }
    static canParse(input, base) { try { new URL(input, base); return true; } catch { return false; } }
    static parse(input, base) { try { return new URL(input, base); } catch { return null; } }
    _setSearch(query) { this.#parts[6] = query ? `?${query}` : ''; }
    get protocol() { return this.#parts[0]; }
    get username() { return this.#parts[1]; }
    get password() { return this.#parts[2]; }
    get hostname() { return this.#parts[3]; }
    get port() { return this.#parts[4]; }
    get host() { return this.#parts[4] ? `${this.#parts[3]}:${this.#parts[4]}` : this.#parts[3]; }
    get pathname() { return this.#parts[5]; }
    set pathname(value) { this.#parts[5] = String(value).startsWith('/') ? String(value) : `/${value}`; }
    get search() { return this.#parts[6].length > 1 ? this.#parts[6] : ''; }
    set search(value) { const v = String(value); this.#parts[6] = v && v !== '?' ? (v.startsWith('?') ? v : `?${v}`) : ''; this.#params._reset(this.#parts[6]); }
    get searchParams() { return this.#params; }
    get hash() { return this.#parts[7].length > 1 ? this.#parts[7] : ''; }
    set hash(value) { const v = String(value); this.#parts[7] = v ? (v.startsWith('#') ? v : `#${v}`) : ''; }
    get origin() { return /^(https?|wss?|ftp):$/.test(this.protocol) ? `${this.protocol}//${this.host}` : 'null'; }
    get href() {
      const auth = this.username ? `${this.username}${this.password ? `:${this.password}` : ''}@` : '';
      const authority = this.hostname || /^(https?|wss?|file):$/.test(this.protocol) ? `//${auth}${this.host}` : '';
      return `${this.protocol}${authority}${this.pathname}${this.search}${this.hash}`;
    }
    set href(value) { const u = new URL(value); this.#parts = [u.protocol, u.username, u.password, u.hostname, u.port, u.pathname, u.search, u.hash]; this.#params._reset(this.#parts[6]); }
    toString() { return this.href; }
    toJSON() { return this.href; }
  }
  define('URL', URL);
  define('URLSearchParams', URLSearchParams);

  // ---- events ----
  class Event {
    #stop = false;
    constructor(type, init = {}) {
      this.type = String(type);
      this.bubbles = !!init.bubbles;
      this.cancelable = !!init.cancelable;
      this.defaultPrevented = false;
      this.timeStamp = N.now();
      this.target = null;
      this.currentTarget = null;
    }
    preventDefault() { if (this.cancelable) this.defaultPrevented = true; }
    stopImmediatePropagation() { this.#stop = true; }
    stopPropagation() {}
    get _stopped() { return this.#stop; }
  }
  class CustomEvent extends Event {
    constructor(type, init = {}) { super(type, init); this.detail = init.detail ?? null; }
  }
  class MessageEvent extends Event {
    constructor(type, init = {}) { super(type, init); this.data = init.data; this.origin = init.origin ?? ''; }
  }
  class CloseEvent extends Event {
    constructor(type, init = {}) { super(type, init); this.code = init.code ?? 0; this.reason = init.reason ?? ''; this.wasClean = !!init.wasClean; }
  }
  class ErrorEvent extends Event {
    constructor(type, init = {}) { super(type, init); this.message = init.message ?? ''; this.error = init.error; }
  }
  class EventTarget {
    #listeners = new Map();
    addEventListener(type, listener, options = {}) {
      if (!listener) return;
      const once = typeof options === 'object' && !!options.once;
      const list = this.#listeners.get(type) ?? [];
      if (list.some((l) => l.listener === listener)) return;
      list.push({ listener, once });
      this.#listeners.set(type, list);
      options?.signal?.addEventListener('abort', () => this.removeEventListener(type, listener));
    }
    removeEventListener(type, listener) {
      const list = this.#listeners.get(type);
      if (list) this.#listeners.set(type, list.filter((l) => l.listener !== listener));
    }
    dispatchEvent(event) {
      Object.defineProperty(event, 'target', { value: this, configurable: true });
      Object.defineProperty(event, 'currentTarget', { value: this, configurable: true });
      const handler = this[`on${event.type}`];
      const list = [...(this.#listeners.get(event.type) ?? [])];
      if (typeof handler === 'function') list.unshift({ listener: handler, once: false });
      for (const entry of list) {
        if (entry.once) this.removeEventListener(event.type, entry.listener);
        try {
          typeof entry.listener === 'function' ? entry.listener.call(this, event) : entry.listener.handleEvent(event);
        } catch (error) {
          console.error(error);
        }
        if (event._stopped) break;
      }
      return !event.defaultPrevented;
    }
  }
  define('Event', Event);
  define('CustomEvent', CustomEvent);
  define('MessageEvent', MessageEvent);
  define('CloseEvent', CloseEvent);
  define('ErrorEvent', ErrorEvent);
  define('EventTarget', EventTarget);

  class AbortSignal extends EventTarget {
    aborted = false;
    reason = undefined;
    static abort(reason) { const c = new AbortController(); c.abort(reason); return c.signal; }
    static timeout(ms) { const c = new AbortController(); setTimeout(() => c.abort(new DOMException('Timed out', 'TimeoutError')), ms); return c.signal; }
    static any(signals) {
      const c = new AbortController();
      for (const s of signals) { if (s.aborted) { c.abort(s.reason); break; } s.addEventListener('abort', () => c.abort(s.reason), { once: true }); }
      return c.signal;
    }
    throwIfAborted() { if (this.aborted) throw this.reason; }
  }
  class AbortController {
    signal = new AbortSignal();
    abort(reason = new DOMException('This operation was aborted', 'AbortError')) {
      if (this.signal.aborted) return;
      this.signal.aborted = true;
      this.signal.reason = reason;
      this.signal.dispatchEvent(new Event('abort'));
    }
  }
  define('AbortSignal', AbortSignal);
  define('AbortController', AbortController);

  // ---- fetch API ----
  class Headers {
    #map = new Map();
    constructor(init) {
      if (init instanceof Headers) for (const [k, v] of init) this.append(k, v);
      else if (Array.isArray(init)) for (const [k, v] of init) this.append(k, v);
      else if (init && typeof init === 'object') for (const k of Object.keys(init)) this.append(k, init[k]);
    }
    append(k, v) { const key = String(k).toLowerCase(); const list = this.#map.get(key) ?? []; list.push(String(v)); this.#map.set(key, list); }
    set(k, v) { this.#map.set(String(k).toLowerCase(), [String(v)]); }
    get(k) { const list = this.#map.get(String(k).toLowerCase()); return list ? list.join(', ') : null; }
    getSetCookie() { return [...(this.#map.get('set-cookie') ?? [])]; }
    has(k) { return this.#map.has(String(k).toLowerCase()); }
    delete(k) { this.#map.delete(String(k).toLowerCase()); }
    forEach(callback, thisArg) { for (const [k, v] of this) callback.call(thisArg, v, k, this); }
    keys() { return [...this.#map.keys()].sort()[Symbol.iterator](); }
    *entries() { for (const k of [...this.#map.keys()].sort()) yield [k, this.get(k)]; }
    values() { return [...this.entries()].map(([, v]) => v)[Symbol.iterator](); }
    [Symbol.iterator]() { return this.entries(); }
    _flat() { const flat = []; for (const [k, list] of this.#map) for (const v of list) flat.push(k, v); return flat; }
  }
  const encoder = new TextEncoder();
  const decoder = new TextDecoder();
  const bodyBytes = (body, headers) => {
    if (body == null) return null;
    if (typeof body === 'string') { if (!headers.has('content-type')) headers.set('content-type', 'text/plain;charset=UTF-8'); return encoder.encode(body); }
    if (body instanceof URLSearchParams) { if (!headers.has('content-type')) headers.set('content-type', 'application/x-www-form-urlencoded;charset=UTF-8'); return encoder.encode(body.toString()); }
    if (body instanceof ArrayBuffer) return new Uint8Array(body.slice(0));
    if (ArrayBuffer.isView(body)) return new Uint8Array(body.buffer.slice(body.byteOffset, body.byteOffset + body.byteLength));
    return encoder.encode(String(body));
  };
  class Body {
    #bytes;
    #used = false;
    _init(bytes) { this.#bytes = bytes; }
    get bodyUsed() { return this.#used; }
    get body() { return this.#bytes; }
    #consume() { if (this.#used) return Promise.reject(new TypeError('Body already used')); this.#used = true; return Promise.resolve(this.#bytes ?? new Uint8Array(0)); }
    arrayBuffer() { return this.#consume().then((b) => b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength)); }
    bytes() { return this.#consume().then((b) => new Uint8Array(b)); }
    text() { return this.#consume().then((b) => decoder.decode(b)); }
    json() { return this.text().then((t) => JSON.parse(t)); }
  }
  class Request extends Body {
    constructor(input, init = {}) {
      super();
      const from = input instanceof Request ? input : null;
      this.url = from ? from.url : new URL(String(input)).href;
      this.method = String(init.method ?? from?.method ?? 'GET').toUpperCase();
      this.headers = new Headers(init.headers ?? from?.headers);
      this.signal = init.signal ?? from?.signal ?? new AbortController().signal;
      this.redirect = init.redirect ?? 'follow';
      this._init(bodyBytes(init.body ?? null, this.headers) ?? from?.body ?? null);
    }
    clone() { return new Request(this, { body: this.body }); }
  }
  class Response extends Body {
    constructor(body = null, init = {}) {
      super();
      this.status = init.status ?? 200;
      this.statusText = init.statusText ?? '';
      this.headers = new Headers(init.headers);
      this.url = init.url ?? '';
      this.redirected = false;
      this.type = 'default';
      this._init(bodyBytes(body, this.headers));
    }
    get ok() { return this.status >= 200 && this.status < 300; }
    clone() { return new Response(this.body, this); }
    static json(data, init = {}) {
      const headers = new Headers(init.headers);
      if (!headers.has('content-type')) headers.set('content-type', 'application/json');
      return new Response(JSON.stringify(data), { ...init, headers });
    }
    static error() { const r = new Response(null, { status: 0 }); r.type = 'error'; return r; }
    static redirect(url, status = 302) { return new Response(null, { status, headers: { location: String(url) } }); }
  }
  define('Headers', Headers);
  define('Request', Request);
  define('Response', Response);

  define('fetch', (input, init = {}) => new Promise((resolve, reject) => {
    const request = new Request(input, init);
    if (request.signal.aborted) return reject(request.signal.reason);
    if (!/^https?:$/.test(new URL(request.url).protocol)) return reject(new TypeError(`fetch: unsupported URL ${request.url}`));
    const handle = N.fetch(request.method, request.url, request.headers._flat(), toHost(request.body), init.timeout ?? 0,
      (status, headers, body, url, error) => {
        if (error != null) return reject(new TypeError(`fetch failed: ${error}`));
        const h = new Headers();
        for (let i = 0; i + 1 < headers.length; i += 2) h.append(headers[i], headers[i + 1]);
        const response = new Response(fromHost(body), { status, headers: h, url });
        resolve(response);
      });
    request.signal.addEventListener('abort', () => { handle.abort(); reject(request.signal.reason); }, { once: true });
  }));

  // ---- WebSocket (outgoing) ----
  class WebSocket extends EventTarget {
    static CONNECTING = 0; static OPEN = 1; static CLOSING = 2; static CLOSED = 3;
    #socket;
    readyState = 0;
    protocol = '';
    binaryType = 'arraybuffer';
    bufferedAmount = 0;
    extensions = '';
    constructor(url, protocols = []) {
      super();
      this.url = new URL(String(url)).href;
      if (!/^wss?:$/.test(new URL(this.url).protocol)) throw new SyntaxError(`WebSocket URL must use ws or wss: ${url}`);
      const list = typeof protocols === 'string' ? [protocols] : [...protocols];
      this.#socket = N.connect(this.url, list, {
        open: (protocol) => { this.readyState = 1; this.protocol = protocol ?? ''; this.dispatchEvent(new Event('open')); },
        message: (data, binary) => {
          const payload = binary ? fromHost(data).buffer : data;
          this.dispatchEvent(new MessageEvent('message', { data: payload, origin: new URL(this.url).origin }));
        },
        close: (code, reason) => {
          if (this.readyState === 3) return;
          this.readyState = 3;
          this.dispatchEvent(new CloseEvent('close', { code, reason, wasClean: code === 1000 }));
        },
        error: (message) => this.dispatchEvent(new ErrorEvent('error', { message })),
      });
    }
    send(data) {
      if (this.readyState !== 1) throw new DOMException('WebSocket is not open', 'InvalidStateError');
      typeof data === 'string' ? this.#socket.sendText(data) : this.#socket.sendBinary(toHost(data));
    }
    close(code = 1000, reason = '') {
      if (this.readyState >= 2) return;
      this.readyState = 2;
      this.#socket.close(code, reason);
    }
  }
  define('WebSocket', WebSocket);

  // ---- crypto, performance, navigator ----
  const subtle = {
    digest: (algorithm, data) => Promise.resolve().then(() =>
      fromHost(N.buffer(N.digest(typeof algorithm === 'string' ? algorithm : algorithm.name, toHost(data)))).buffer),
  };
  define('crypto', {
    randomUUID: () => N.randomUUID(),
    getRandomValues: (array) => {
      const bytes = fromHost(N.buffer(N.randomBytes(array.byteLength)));
      new Uint8Array(array.buffer, array.byteOffset, array.byteLength).set(bytes);
      return array;
    },
    subtle,
  });
  const timeOrigin = N.timeOrigin();
  const start = N.now();
  define('performance', { timeOrigin, now: () => N.now() - start, mark() {}, measure() {} });
  define('navigator', { userAgent: 'Spectra', language: 'en-US', hardwareConcurrency: N.os().get('cpus') });

  define('structuredClone', (value) => {
    const seen = new Map();
    const clone = (v) => {
      if (v === null || typeof v !== 'object') {
        if (typeof v === 'function' || typeof v === 'symbol') throw new DOMException('Could not clone', 'DataCloneError');
        return v;
      }
      if (seen.has(v)) return seen.get(v);
      let out;
      if (v instanceof Date) out = new Date(v.getTime());
      else if (v instanceof RegExp) out = new RegExp(v.source, v.flags);
      else if (v instanceof ArrayBuffer) out = v.slice(0);
      else if (ArrayBuffer.isView(v)) out = new v.constructor(v);
      else if (v instanceof Map) { out = new Map(); seen.set(v, out); for (const [k, x] of v) out.set(clone(k), clone(x)); return out; }
      else if (v instanceof Set) { out = new Set(); seen.set(v, out); for (const x of v) out.add(clone(x)); return out; }
      else if (Array.isArray(v)) { out = []; seen.set(v, out); for (const x of v) out.push(clone(x)); return out; }
      else { out = {}; seen.set(v, out); for (const k of Object.keys(v)) out[k] = clone(v[k]); return out; }
      seen.set(v, out);
      return out;
    };
    return clone(value);
  });

  // ---- process (Node) ----
  const os = N.os();
  const listeners = new Map();
  const process = {
    env: Object.fromEntries([...N.env().entrySet()].map((e) => [e.getKey(), e.getValue()])),
    argv: ['spectra'],
    execArgv: [],
    platform: os.get('platform'),
    arch: os.get('arch'),
    pid: Number(os.get('pid')),
    version: 'v22.0.0',
    versions: { node: '22.0.0', spectra: '0.1.0' },
    release: { name: 'node' },
    exitCode: undefined,
    cwd: () => N.cwd(),
    exit: (code) => { throw new Error(`process.exit(${code ?? 0}) is not supported in Spectra scripts`); },
    nextTick: (callback, ...args) => queueMicrotask(() => callback(...args)),
    hrtime: Object.assign((previous) => {
      const t = BigInt(N.nanoTime());
      const s = Number(t / 1000000000n);
      const n = Number(t % 1000000000n);
      return previous ? [s - previous[0], n - previous[1]] : [s, n];
    }, { bigint: () => BigInt(N.nanoTime()) }),
    uptime: () => Number(N.os().get('uptime')),
    memoryUsage: () => ({ rss: 0, heapTotal: Number(os.get('totalmem')), heapUsed: 0, external: 0, arrayBuffers: 0 }),
    on(event, listener) { (listeners.get(event) ?? listeners.set(event, []).get(event)).push(listener); return this; },
    off(event, listener) { listeners.set(event, (listeners.get(event) ?? []).filter((l) => l !== listener)); return this; },
    once(event, listener) { return this.on(event, listener); },
    emit(event, ...args) { for (const l of listeners.get(event) ?? []) l(...args); return (listeners.get(event) ?? []).length > 0; },
    stdout: { write: (s) => { console.log(String(s).replace(/\n$/, '')); return true; } },
    stderr: { write: (s) => { console.error(String(s).replace(/\n$/, '')); return true; } },
  };
  define('process', process);
  define('global', globalThis);

  // ---- server-side 'fetch' and 'websocket' events: requests from views (see spectra.d.ts) ----
  const webListeners = { fetch: [], websocket: [] };
  let servingHttp = false;
  let servingSockets = false;
  const VIEW_ORIGIN = 'https://view.spectra.invalid';

  class ViewSocket extends EventTarget {
    readyState = 1;
    constructor(native, url, player, viewId) {
      super();
      this.url = url;
      this.player = player;
      this.viewId = viewId;
      this.binaryType = 'arraybuffer';
      native.listen({
        message: (data, binary) => this.dispatchEvent(new MessageEvent('message', { data: binary ? fromHost(data).buffer : data })),
        close: (code, reason) => { this.readyState = 3; this.dispatchEvent(new CloseEvent('close', { code, reason, wasClean: true })); },
      });
      this._native = native;
    }
    send(data) { typeof data === 'string' ? this._native.sendText(data) : this._native.sendBinary(toHost(data)); }
    close(code = 1000, reason = '') { if (this.readyState < 2) { this.readyState = 2; this._native.close(code, reason); } }
  }

  const dispatchFetch = (playerId, viewId, method, path, headers, body, respond) => {
    const h = new Headers();
    for (let i = 0; i + 1 < headers.length; i += 2) h.append(headers[i], headers[i + 1]);
    const bytes = fromHost(body);
    const request = new Request(VIEW_ORIGIN + path, { method, headers: h, body: method === 'GET' || method === 'HEAD' ? null : bytes });
    let responded = false;
    const finish = (response) => {
      if (!(response instanceof Response)) response = new Response(String(response ?? ''), { status: 500 });
      respond(response.status, response.headers._flat(), toHost(response.body ?? new Uint8Array(0)));
    };
    const event = new Event('fetch');
    Object.assign(event, {
      request, viewId, player: N.player(playerId),
      respondWith(value) {
        if (responded) throw new DOMException('respondWith already called', 'InvalidStateError');
        responded = true;
        Promise.resolve(value).then(finish, (error) => { console.error(error); finish(new Response('Internal error', { status: 500 })); });
      },
    });
    for (const listener of webListeners.fetch) {
      listener(event);
      if (responded) return;
    }
    respond(null);
  };

  const dispatchSocket = (playerId, viewId, path, accept) => {
    let socket = null;
    const event = new Event('websocket');
    Object.assign(event, {
      request: new Request(VIEW_ORIGIN + path), viewId, player: N.player(playerId),
      accept() { socket ??= new ViewSocket(accept(), VIEW_ORIGIN + path, this.player, viewId); return socket; },
    });
    for (const listener of webListeners.websocket) {
      listener(event);
      if (socket) return;
    }
  };

  const hostAddEventListener = globalThis.addEventListener;
  const hostRemoveEventListener = globalThis.removeEventListener;
  define('addEventListener', (type, listener, ...options) => {
    if (typeof type !== 'string') return hostAddEventListener(type, listener, ...options);
    if (!(type in webListeners)) throw new TypeError(`Unknown event type '${type}'; use 'fetch', 'websocket', or an event class`);
    webListeners[type].push(listener);
    if (type === 'fetch' && !servingHttp) { servingHttp = true; N.serveHttp(dispatchFetch); }
    if (type === 'websocket' && !servingSockets) { servingSockets = true; N.serveSockets(dispatchSocket); }
    return { cancel: () => { webListeners[type] = webListeners[type].filter((l) => l !== listener); } };
  });
  define('removeEventListener', (type, listener) => {
    if (typeof type !== 'string') return hostRemoveEventListener(type);
    if (type in webListeners) webListeners[type] = webListeners[type].filter((l) => l !== listener);
  });
})();
