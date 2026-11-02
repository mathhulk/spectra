// node:events
export class EventEmitter {
  static defaultMaxListeners = 10;
  #events = new Map();
  #max = EventEmitter.defaultMaxListeners;
  on(name, listener) { const list = this.#events.get(name) ?? []; list.push(listener); this.#events.set(name, list); return this; }
  addListener(name, listener) { return this.on(name, listener); }
  prependListener(name, listener) { this.#events.set(name, [listener, ...(this.#events.get(name) ?? [])]); return this; }
  once(name, listener) {
    const wrapper = (...args) => { this.off(name, wrapper); listener.apply(this, args); };
    wrapper.listener = listener;
    return this.on(name, wrapper);
  }
  off(name, listener) {
    const list = this.#events.get(name);
    if (list) this.#events.set(name, list.filter((l) => l !== listener && l.listener !== listener));
    return this;
  }
  removeListener(name, listener) { return this.off(name, listener); }
  removeAllListeners(name) { name === undefined ? this.#events.clear() : this.#events.delete(name); return this; }
  emit(name, ...args) {
    const list = this.#events.get(name);
    if (!list || list.length === 0) {
      if (name === 'error') throw args[0] instanceof Error ? args[0] : new Error(`Unhandled error: ${args[0]}`);
      return false;
    }
    for (const listener of [...list]) listener.apply(this, args);
    return true;
  }
  listeners(name) { return (this.#events.get(name) ?? []).map((l) => l.listener ?? l); }
  rawListeners(name) { return [...(this.#events.get(name) ?? [])]; }
  listenerCount(name) { return (this.#events.get(name) ?? []).length; }
  eventNames() { return [...this.#events.keys()]; }
  setMaxListeners(n) { this.#max = n; return this; }
  getMaxListeners() { return this.#max; }
}
export function once(emitter, name) {
  return new Promise((resolve, reject) => {
    if (typeof emitter.addEventListener === 'function' && typeof emitter.once !== 'function') {
      emitter.addEventListener(name, (event) => resolve([event]), { once: true });
      return;
    }
    const onError = (error) => { emitter.off(name, onEvent); reject(error); };
    const onEvent = (...args) => { emitter.off('error', onError); resolve(args); };
    emitter.once(name, onEvent);
    if (name !== 'error') emitter.once('error', onError);
  });
}
EventEmitter.EventEmitter = EventEmitter;
EventEmitter.once = once;
export default EventEmitter;
