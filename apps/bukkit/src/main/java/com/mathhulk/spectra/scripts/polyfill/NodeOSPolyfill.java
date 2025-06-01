package com.mathhulk.spectra.scripts.polyfill;

import java.net.InetAddress;
import java.nio.ByteOrder;

public class NodeOSPolyfill {
  public static String EOL = System.lineSeparator();

  public static String devNull = System.getProperty("os.name").toLowerCase().contains("win") ? "\\\\.\\nil"
      : "/dev/null";

  public static int availableParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }

  /**
   * @return System.getProperty("os.arch")
   */
  public static String arch() {
    return System.getProperty("os.arch");
  }

  public static void cpus() {
    throw new UnsupportedOperationException("os.cpus() is not implemented");
  }

  public static String endianness() {
    ByteOrder byteOrder = ByteOrder.nativeOrder();

    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      return "BE";
    } else {
      return "LE";
    }
  }

  public static int freemem() {
    return (int) Runtime.getRuntime().freeMemory();
  }

  public static void getPriority(int pid) {
    throw new UnsupportedOperationException("os.getPriority() is not implemented");
  }

  public static String homedir() {
    return System.getProperty("user.home");
  }

  /**
   * @return InetAddress.getLocalHost().getHostName()
   */
  public static String hostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      return null;
    }
  }

  public static void loadavg() {
    throw new UnsupportedOperationException("os.loadavg() is not implemented");
  }

  /**
   * @return System.getProperty("os.arch")
   */
  public static String machine() {
    return System.getProperty("os.arch");
  }

  public static void networkInterfaces() {
    throw new UnsupportedOperationException("os.networkInterfaces() is not implemented");
  }

  /**
   * @return System.getProperty("os.name")
   */
  public static String platform() {
    return System.getProperty("os.name");
  }

  /**
   * @return System.getProperty("os.version")
   */
  public static String release() {
    return System.getProperty("os.version");
  }

  public static String setPriority(int priority) {
    throw new UnsupportedOperationException("os.setPriority() is not implemented");
  }

  public static String setPriority(int pid, int priority) {
    throw new UnsupportedOperationException("os.setPriority() is not implemented");
  }

  public static String tmpdir() {
    return System.getProperty("java.io.tmpdir");
  }

  public static int totalmem() {
    return (int) Runtime.getRuntime().totalMemory();
  }

  public static String type() {
    throw new UnsupportedOperationException("os.type() is not implemented");
  }

  public static int uptime() {
    throw new UnsupportedOperationException("os.uptime() is not implemented");
  }

  public static String userInfo() {
    throw new UnsupportedOperationException("os.userInfo() is not implemented");
  }

  public static String userInfo(Object options) {
    throw new UnsupportedOperationException("os.userInfo() is not implemented");
  }

  /**
   * @return System.getProperty("os.version")
   */
  public static String version() {
    return System.getProperty("os.version");
  }

}
