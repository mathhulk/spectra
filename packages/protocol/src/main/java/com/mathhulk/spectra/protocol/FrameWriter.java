package com.mathhulk.spectra.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;

/** Growable little helper for writing protocol primitives. Not thread-safe. */
public final class FrameWriter {
  private byte[] buffer;
  private int size;

  public FrameWriter() {
    this(64);
  }

  public FrameWriter(int initialCapacity) {
    buffer = new byte[Math.max(16, initialCapacity)];
  }

  private void ensure(int extra) {
    int needed = size + extra;
    if (needed > buffer.length) {
      buffer = Arrays.copyOf(buffer, Math.max(needed, buffer.length * 2));
    }
  }

  public int size() {
    return size;
  }

  public byte[] toByteArray() {
    return Arrays.copyOf(buffer, size);
  }

  public FrameWriter writeByte(int value) {
    ensure(1);
    buffer[size++] = (byte) value;
    return this;
  }

  public FrameWriter writeBytesRaw(byte[] bytes) {
    ensure(bytes.length);
    System.arraycopy(bytes, 0, buffer, size, bytes.length);
    size += bytes.length;
    return this;
  }

  public FrameWriter writeBool(boolean value) {
    return writeByte(value ? 1 : 0);
  }

  public FrameWriter writeVarInt(int value) {
    while ((value & ~0x7F) != 0) {
      writeByte((value & 0x7F) | 0x80);
      value >>>= 7;
    }
    return writeByte(value);
  }

  public FrameWriter writeVarLong(long value) {
    while ((value & ~0x7FL) != 0) {
      writeByte((int) (value & 0x7F) | 0x80);
      value >>>= 7;
    }
    return writeByte((int) value);
  }

  public FrameWriter writeBytes(byte[] bytes) {
    writeVarInt(bytes.length);
    return writeBytesRaw(bytes);
  }

  public FrameWriter writeString(String value, int maxBytes) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > maxBytes) {
      throw new ProtocolException("String is " + bytes.length + " bytes, limit is " + maxBytes);
    }
    return writeBytes(bytes);
  }

  public <E extends Enum<E>> FrameWriter writeEnum(E value) {
    return writeVarInt(value.ordinal());
  }

  public <T> FrameWriter writeList(Collection<T> values, int maxCount, BiConsumer<FrameWriter, T> element) {
    if (values.size() > maxCount) {
      throw new ProtocolException("List has " + values.size() + " elements, limit is " + maxCount);
    }
    writeVarInt(values.size());
    for (T value : values) {
      element.accept(this, value);
    }
    return this;
  }
}
