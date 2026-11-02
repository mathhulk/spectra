package com.mathhulk.spectra.fabric.browser;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** The {@code window.spectra} bridge injected into every Spectra page, and safe JS string encoding. */
public final class Bridge {
  /**
   * Defined before any page script runs. {@code send} goes to the server through the CEF message router; the page
   * receives server messages through {@code onMessage} listeners and a {@code spectra:message} window event.
   */
  static final String SCRIPT = """
      <script>(() => {
        const query = (message, persistent = false, onSuccess = () => {}, onFailure = () => {}) => window.cefQuery({
          request: JSON.stringify(message), persistent, onSuccess, onFailure,
        });
        const listeners = new Set();
        const send = (data) => new Promise((resolve, reject) =>
          query({ t: 'post', d: data === undefined ? null : data }, false, () => resolve(),
            (code, message) => reject(new Error(message))));
        const game = {
          player: () => fetch('/_spectra/game/player').then((r) => r.json()),
          mapUrl: (options = {}) => `/_spectra/game/map.png?${new URLSearchParams(options)}&t=${Date.now()}`,
        };
        Object.defineProperty(window, 'spectra', { value: Object.freeze({
          send,
          onMessage(listener) { listeners.add(listener); return () => listeners.delete(listener); },
          game,
        }) });
        Object.defineProperty(window, '__spectraReceive', { value: (text) => {
          let data;
          try { data = JSON.parse(text); } catch { return; }
          for (const listener of listeners) { try { listener(data); } catch (e) { console.error(e); } }
          window.dispatchEvent(new MessageEvent('spectra:message', { data }));
        } });

        // WebSockets to this origin under /api/ are tunneled to the Minecraft server; others are native.
        const NativeWebSocket = window.WebSocket;
        let nextSocket = 1;
        const toBase64 = (bytes) => { let s = ''; for (const b of bytes) s += String.fromCharCode(b); return btoa(s); };
        const fromBase64 = (text) => Uint8Array.from(atob(text), (c) => c.charCodeAt(0));
        class TunneledWebSocket extends EventTarget {
          static CONNECTING = 0; static OPEN = 1; static CLOSING = 2; static CLOSED = 3;
          readyState = 0; protocol = ''; extensions = ''; bufferedAmount = 0; binaryType = 'blob';
          onopen = null; onmessage = null; onclose = null; onerror = null;
          constructor(url, protocols = []) {
            super();
            this.url = url.href;
            this.id = nextSocket++;
            const list = typeof protocols === 'string' ? [protocols] : [...protocols];
            query({ t: 'ws-open', id: this.id, path: url.pathname + url.search, protocols: list }, true,
              (text) => this.#event(JSON.parse(text)),
              (code, message) => this.#closed(1006, message));
          }
          #fire(event) { this.dispatchEvent(event); const h = this['on' + event.type]; if (h) h.call(this, event); }
          #closed(code, reason) {
            if (this.readyState === 3) return;
            this.readyState = 3;
            this.#fire(new CloseEvent('close', { code, reason, wasClean: code === 1000 }));
          }
          #event(e) {
            if (e.e === 'open') { this.readyState = 1; this.protocol = e.protocol || ''; this.#fire(new Event('open')); }
            else if (e.e === 'message') {
              let data = e.text;
              if (e.binary) { const bytes = fromBase64(e.b64); data = this.binaryType === 'arraybuffer' ? bytes.buffer : new Blob([bytes]); }
              this.#fire(new MessageEvent('message', { data, origin: location.origin }));
            } else if (e.e === 'close') this.#closed(e.code, e.reason);
          }
          send(data) {
            if (this.readyState !== 1) throw new DOMException('WebSocket is not open', 'InvalidStateError');
            if (typeof data === 'string') { query({ t: 'ws-send', id: this.id, text: data }); return; }
            const toBytes = (buffer) => query({ t: 'ws-send', id: this.id, b64: toBase64(new Uint8Array(buffer)) });
            if (data instanceof Blob) data.arrayBuffer().then(toBytes);
            else toBytes(ArrayBuffer.isView(data) ? data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength) : data);
          }
          close(code = 1000, reason = '') {
            if (this.readyState >= 2) return;
            this.readyState = 2;
            query({ t: 'ws-close', id: this.id, code, reason });
          }
        }
        window.WebSocket = function WebSocket(url, protocols) {
          const resolved = new URL(url, location.href);
          if (resolved.host === location.host && resolved.pathname.startsWith('/api/')) {
            return new TunneledWebSocket(resolved, protocols);
          }
          return new NativeWebSocket(url, protocols);
        };
        Object.assign(window.WebSocket, { CONNECTING: 0, OPEN: 1, CLOSING: 2, CLOSED: 3 });
      })();</script>
      """;

  private Bridge() {
  }

  /** Inserts the bridge after the doctype (if any) so documents stay in standards mode. */
  public static byte[] inject(byte[] html) {
    String text = new String(html, StandardCharsets.UTF_8);
    int insertAt = 0;
    String head = text.stripLeading();
    int leading = text.length() - head.length();
    if (head.toLowerCase(Locale.ROOT).startsWith("<!doctype")) {
      int end = text.indexOf('>', leading);
      insertAt = end < 0 ? 0 : end + 1;
    }
    return (text.substring(0, insertAt) + SCRIPT + text.substring(insertAt)).getBytes(StandardCharsets.UTF_8);
  }

  /** Encodes {@code value} as a JavaScript string literal that is safe to splice into code. */
  public static String jsString(String value) {
    StringBuilder out = new StringBuilder(value.length() + 16).append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '<' -> out.append("\\u003c");
        case ' ' -> out.append("\\u2028");
        case ' ' -> out.append("\\u2029");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.append('"').toString();
  }
}
