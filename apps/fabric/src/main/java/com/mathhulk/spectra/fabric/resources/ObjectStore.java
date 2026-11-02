package com.mathhulk.spectra.fabric.resources;

import com.mathhulk.spectra.protocol.ResourceHash;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Content-addressed resource cache shared by every server: {@code objects/ab/abcdef...}. Files are named by their
 * SHA-256, so identical files are stored once, server-supplied paths never touch the file system, and a file only
 * exists here after its content was verified.
 */
public final class ObjectStore {
  private final Path root;

  public ObjectStore(Path root) {
    this.root = root;
  }

  public Path path(ResourceHash hash) {
    String hex = hash.toHex();
    return root.resolve(hex.substring(0, 2)).resolve(hex);
  }

  public boolean contains(ResourceHash hash) {
    return Files.isRegularFile(path(hash));
  }

  /** Verifies {@code content} against {@code hash} and stores it atomically. */
  public void put(ResourceHash hash, byte[] content) throws IOException {
    if (!ResourceHash.compute(content).equals(hash)) {
      throw new IOException("Content does not match hash " + hash);
    }
    Path target = path(hash);
    Files.createDirectories(target.getParent());
    Path temporary = Files.createTempFile(target.getParent(), "download-", ".tmp");
    try {
      Files.write(temporary, content);
      try {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
