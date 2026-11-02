package com.mathhulk.spectra.protocol;

/** Size limits shared by encoders and decoders. Decoders enforce them on untrusted input. */
public final class Limits {
  /** Vanilla limit for a clientbound custom payload. */
  public static final int MAX_CLIENTBOUND_FRAME = 1024 * 1024;
  /** Vanilla limit for a serverbound custom payload. */
  public static final int MAX_SERVERBOUND_FRAME = 32767;

  public static final int MAX_ID = 128;
  public static final int MAX_SHORT_TEXT = 256;
  public static final int MAX_CAPABILITIES = 64;
  public static final int MAX_CAPABILITY = 64;

  public static final int MAX_MANIFEST_ENTRIES = 8192;
  public static final int MAX_CHUNK_SIZE = 256 * 1024;
  public static final int MAX_CHUNKS = 64 * 1024;
  /** Hashes per request; 32 bytes each keeps a request well under the serverbound frame limit. */
  public static final int MAX_REQUEST_HASHES = 960;

  public static final int MAX_CLIENTBOUND_JSON = 512 * 1024;
  public static final int MAX_SERVERBOUND_JSON = 30 * 1024;

  public static final int MAX_HTTP_PATH = 2048;
  public static final int MAX_HTTP_METHOD = 16;
  public static final int MAX_HEADERS = 64;
  public static final int MAX_HEADER_NAME = 128;
  public static final int MAX_HEADER_VALUE = 8192;
  /** Body bytes per serverbound packet; larger request bodies continue in HttpRequestBody packets. */
  public static final int MAX_SERVERBOUND_BODY = 16 * 1024;
  /** Body bytes per clientbound packet; larger response bodies continue in HttpResponseBody packets. */
  public static final int MAX_CLIENTBOUND_BODY = 512 * 1024;
  /** Socket message bytes per serverbound packet; larger messages are fragmented. */
  public static final int MAX_SERVERBOUND_SOCKET_DATA = 24 * 1024;
  public static final int MAX_CLIENTBOUND_SOCKET_DATA = 512 * 1024;
  public static final int MAX_SOCKET_PROTOCOLS = 8;
  public static final int MAX_NETWORK_ORIGINS = 64;

  private Limits() {
  }
}
