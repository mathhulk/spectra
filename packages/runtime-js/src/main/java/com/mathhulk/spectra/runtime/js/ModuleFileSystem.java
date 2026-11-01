package com.mathhulk.spectra.runtime.js;

import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;

/**
 * Read-only file system for script module loading that maps Node-style builtin specifiers ({@code node:path},
 * {@code path}, {@code fs/promises}, ...) to Spectra's shim modules, so code written for Node, Bun, or Deno can
 * import them unchanged.
 */
final class ModuleFileSystem implements FileSystem {
  private final FileSystem delegate = FileSystem.newReadOnlyFileSystem(FileSystem.newDefaultFileSystem());
  private final Map<String, Path> builtins;

  ModuleFileSystem(Map<String, Path> builtins) {
    this.builtins = builtins;
  }

  private Path builtin(String specifier) {
    String name = specifier.startsWith("node:") ? specifier.substring(5) : specifier;
    return builtins.get(name);
  }

  @Override
  public Path parsePath(URI uri) {
    Path builtin = "node".equals(uri.getScheme()) ? builtin("node:" + uri.getSchemeSpecificPart()) : null;
    return builtin != null ? builtin : delegate.parsePath(uri);
  }

  @Override
  public Path parsePath(String path) {
    Path builtin = builtin(path);
    return builtin != null ? builtin : delegate.parsePath(path);
  }

  @Override
  public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
    delegate.checkAccess(path, modes, linkOptions);
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    delegate.createDirectory(dir, attrs);
  }

  @Override
  public void delete(Path path) throws IOException {
    delegate.delete(path);
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException {
    return delegate.newByteChannel(path, options, attrs);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter)
      throws IOException {
    return delegate.newDirectoryStream(dir, filter);
  }

  @Override
  public Path toAbsolutePath(Path path) {
    return delegate.toAbsolutePath(path);
  }

  @Override
  public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
    return delegate.toRealPath(path, linkOptions);
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    return delegate.readAttributes(path, attributes, options);
  }

  @Override
  public void setCurrentWorkingDirectory(Path currentWorkingDirectory) {
    delegate.setCurrentWorkingDirectory(currentWorkingDirectory);
  }

  @Override
  public String getMimeType(Path path) {
    return delegate.getMimeType(path);
  }

  @Override
  public Charset getEncoding(Path path) {
    return delegate.getEncoding(path);
  }
}
