package com.mathhulk.spectra.protocol;

import java.util.regex.Pattern;

/** Validation for resource paths. Both sides must reject anything that could escape a resource root. */
public final class ResourcePath {
  public static final int MAX_LENGTH = 512;

  private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._@+\\- ]+");

  private ResourcePath() {
  }

  /** Returns true when {@code path} is a safe, normalized, relative resource path. */
  public static boolean isValid(String path) {
    if (path == null || path.isEmpty() || path.length() > MAX_LENGTH) {
      return false;
    }
    if (path.startsWith("/") || path.endsWith("/") || path.contains("\\") || path.contains("//")) {
      return false;
    }
    for (String segment : path.split("/")) {
      if (segment.equals(".") || segment.equals("..") || !SEGMENT.matcher(segment).matches()) {
        return false;
      }
      if (segment.endsWith(" ") || segment.startsWith(" ")) {
        return false;
      }
    }
    // Windows drive letters and reserved separators are already excluded by SEGMENT (no ':').
    return true;
  }

  public static String requireValid(String path) {
    if (!isValid(path)) {
      throw new ProtocolException("Invalid resource path: " + path);
    }
    return path;
  }
}
