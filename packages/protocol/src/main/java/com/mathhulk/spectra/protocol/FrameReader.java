package com.mathhulk.spectra.protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Bounds-checked reader for protocol primitives. Every read validates against the remaining input. */
public final class FrameReader {
  private final byte[] data;
  private int position;

  public FrameReader(byte[] data) {
    this.data = data;
  }

  public int remaining() {
    return data.length - position;
  }

  private void require(int count) {
    if (count < 0 || count > remaining()) {
      throw new ProtocolException("Unexpected end of frame (needed " + count + ", have " + remaining() + ")");
    }
  }

  public int readByte() {
    require(1);
    return data[position++] & 0xFF;
  }

  public byte[] readBytesRaw(int length) {
    require(length);
    byte[] out = Arrays.copyOfRange(data, position, position + length);
    position += length;
    return out;
  }

  public boolean readBool() {
    int value = readByte();
    if (value > 1) {
      throw new ProtocolException("Invalid boolean " + value);
    }
    return value == 1;
  }

  public int readVarInt() {
    int value = 0;
    for (int shift = 0; shift < 35; shift += 7) {
      int b = readByte();
      value |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return value;
      }
    }
    throw new ProtocolException("VarInt too long");
  }

  public long readVarLong() {
    long value = 0;
    for (int shift = 0; shift < 70; shift += 7) {
      int b = readByte();
      value |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return value;
      }
    }
    throw new ProtocolException("VarLong too long");
  }

  /** Reads a non-negative length and checks it against {@code max}. */
  public int readLength(int max) {
    int length = readVarInt();
    if (length < 0 || length > max) {
      throw new ProtocolException("Length " + length + " outside [0, " + max + "]");
    }
    return length;
  }

  public byte[] readBytes(int maxLength) {
    return readBytesRaw(readLength(maxLength));
  }

  public String readString(int maxBytes) {
    return new String(readBytes(maxBytes), StandardCharsets.UTF_8);
  }

  public <E extends Enum<E>> E readEnum(Class<E> type) {
    E[] values = type.getEnumConstants();
    int ordinal = readVarInt();
    if (ordinal < 0 || ordinal >= values.length) {
      throw new ProtocolException("Invalid " + type.getSimpleName() + " ordinal " + ordinal);
    }
    return values[ordinal];
  }

  public <T> List<T> readList(int maxCount, Function<FrameReader, T> element) {
    int count = readLength(maxCount);
    List<T> values = new ArrayList<>(Math.min(count, 1024));
    for (int i = 0; i < count; i++) {
      values.add(element.apply(this));
    }
    return List.copyOf(values);
  }

  public void expectEnd() {
    if (remaining() != 0) {
      throw new ProtocolException(remaining() + " trailing bytes in frame");
    }
  }
}
