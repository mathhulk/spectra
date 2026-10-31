package com.mathhulk.spectra.core.platform;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/** Everything core needs from a server platform (Paper, Velocity, Minestom, ...). */
public interface Platform {
  /** Short platform name, e.g. "paper". */
  String name();

  Logger logger();

  /** Directory for Spectra's own data (config, server id, resources). */
  Path dataDirectory();

  MainThread mainThread();

  /** Executor for blocking work such as file IO and hashing. Never touches game state. */
  Executor asyncExecutor();

  /** Sends one protocol frame on the Spectra channel. Must be called on the main thread. */
  void sendFrame(UUID player, byte[] frame);

  /** Disconnects a player with a plain-text reason. Must be called on the main thread. */
  void kick(UUID player, String reason);

  boolean isOnline(UUID player);
}
