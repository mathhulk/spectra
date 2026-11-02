package com.mathhulk.spectra.api.clients;

/** Well-known capability strings a client may advertise. */
public final class Capability {
  /** Content-addressed resource sync. */
  public static final String RESOURCES = "resources";
  /** Full-screen views. */
  public static final String SCREENS = "ui.screens";
  /** HUD layer views. */
  public static final String LAYERS = "ui.layers";
  /** JSON messages between views and the server. */
  public static final String MESSAGES = "ui.messages";
  /** HTTP requests from views to the server, tunneled over the Spectra protocol. */
  public static final String HTTP = "http";
  /** WebSockets from views to the server, tunneled over the Spectra protocol. */
  public static final String SOCKETS = "sockets";
  /** The client honors the server's network allowlist for direct internet requests from views. */
  public static final String NETWORK = "network";

  private Capability() {
  }
}
