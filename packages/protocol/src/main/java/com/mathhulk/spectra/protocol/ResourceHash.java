package com.mathhulk.spectra.protocol;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/** An immutable SHA-256 digest identifying a resource's content. */
public final class ResourceHash {
  public static final int LENGTH = 32;

  private final byte[] bytes;

  private ResourceHash(byte[] bytes) {
    if (bytes.length != LENGTH) {
      throw new IllegalArgumentException("SHA-256 hashes are 32 bytes, got " + bytes.length);
    }
    this.bytes = bytes.clone();
  }

  public static ResourceHash of(byte[] digest) {
    return new ResourceHash(digest);
  }

  public static ResourceHash fromHex(String hex) {
    return new ResourceHash(HexFormat.of().parseHex(hex));
  }

  public static ResourceHash compute(byte[] content) {
    return new ResourceHash(newDigest().digest(content));
  }

  public static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by every Java platform", e);
    }
  }

  public static ResourceHash read(FrameReader reader) {
    return new ResourceHash(reader.readBytesRaw(LENGTH));
  }

  public void write(FrameWriter writer) {
    writer.writeBytesRaw(bytes);
  }

  public byte[] toByteArray() {
    return bytes.clone();
  }

  public String toHex() {
    return HexFormat.of().formatHex(bytes);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ResourceHash hash && Arrays.equals(bytes, hash.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public String toString() {
    return toHex();
  }
}
