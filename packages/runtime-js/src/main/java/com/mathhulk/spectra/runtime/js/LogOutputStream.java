package com.mathhulk.spectra.runtime.js;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Turns a script's stdout/stderr (console.log and friends) into log records prefixed with the script name. */
final class LogOutputStream extends OutputStream {
  private final Logger logger;
  private final Level level;
  private final String prefix;
  private final ByteArrayOutputStream line = new ByteArrayOutputStream();

  LogOutputStream(Logger logger, Level level, String prefix) {
    this.logger = logger;
    this.level = level;
    this.prefix = prefix;
  }

  @Override
  public synchronized void write(int b) {
    if (b == '\n') {
      flushLine();
    } else if (b != '\r') {
      line.write(b);
    }
  }

  private void flushLine() {
    logger.log(level, "[" + prefix + "] " + line.toString(StandardCharsets.UTF_8));
    line.reset();
  }

  @Override
  public synchronized void flush() {
    if (line.size() > 0) {
      flushLine();
    }
  }
}
