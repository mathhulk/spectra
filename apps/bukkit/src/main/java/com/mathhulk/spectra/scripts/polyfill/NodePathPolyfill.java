package com.mathhulk.spectra.scripts.polyfill;

import java.io.File;

public class NodePathPolyfill {
  public static String delimeter = File.pathSeparator;

  public static String posix = null;

  public static String win32 = null;

  public static String sep = File.separator;

  public static String basename(String path, String suffix) {
    return new File(path).getName().replaceAll(suffix + "$", "");
  }

  public static String dirname(String path) {
    return new File(path).getParent();
  }

  public static String extname(String path) {
    String name = new File(path).getName();
    int lastDotIndex = name.lastIndexOf('.');

    if (lastDotIndex == -1 || lastDotIndex == 0) {
      return "";
    }

    return name.substring(lastDotIndex);
  }
}
