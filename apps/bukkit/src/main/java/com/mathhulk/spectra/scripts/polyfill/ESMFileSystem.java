package com.mathhulk.spectra.scripts.polyfill;

import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.FileAttribute;
import java.util.*;

public class ESMFileSystem implements FileSystem {
  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException {
    if (path.toString().startsWith("node:")) {

    }

    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
    return path.toRealPath(linkOptions);
  }

  @Override
  public Path toAbsolutePath(Path path) {
    return path.toAbsolutePath();
  }

  @Override
  public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
    throw new AssertionError();
  }

  @Override
  public Path parsePath(URI uri) {
    return Path.of(uri);
  }

  @Override
  public Path parsePath(String path) {
    return Paths.get(path, null);
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    throw new AssertionError();
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
    throw new AssertionError();
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    throw new AssertionError();
  }

  @Override
  public void delete(Path path) throws IOException {
    throw new AssertionError();
  }
}
