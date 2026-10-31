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
        const listeners = new Set();
        const send = (data) => new Promise((resolve, reject) => window.cefQuery({
          request: JSON.stringify(data === undefined ? null : data),
          persistent: false,
          onSuccess: () => resolve(),
          onFailure: (code, message) => reject(new Error(message)),
        }));
        Object.defineProperty(window, 'spectra', { value: Object.freeze({
          send,
          onMessage(listener) { listeners.add(listener); return () => listeners.delete(listener); },
        }) });
        Object.defineProperty(window, '__spectraReceive', { value: (text) => {
          let data;
          try { data = JSON.parse(text); } catch { return; }
          for (const listener of listeners) { try { listener(data); } catch (e) { console.error(e); } }
          window.dispatchEvent(new MessageEvent('spectra:message', { data }));
        } });
      })();</script>
      """;

  private Bridge() {
  }

  /** Inserts the bridge after the doctype (if any) so documents stay in standards mode. */
  static byte[] inject(byte[] html) {
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
