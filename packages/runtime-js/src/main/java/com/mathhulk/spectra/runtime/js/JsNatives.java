package com.mathhulk.spectra.runtime.js;

import com.mathhulk.spectra.api.Registration;
import com.mathhulk.spectra.api.Spectra;
import com.mathhulk.spectra.api.http.HttpRequest;
import com.mathhulk.spectra.api.http.HttpResponse;
import com.mathhulk.spectra.api.http.WebSocket;
import com.mathhulk.spectra.core.platform.MainThread;
import com.mathhulk.spectra.core.scripts.ScriptScope;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Host functions behind Spectra's web and Node compatibility layer ({@code prelude.js} and the {@code node:*}
 * shims). One instance per script. Asynchronous results are delivered on the main thread under the script's lock.
 */
public final class JsNatives {
  private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(30)).build();
  private static final SecureRandom RANDOM = new SecureRandom();
  /** Headers java.net.http manages itself. */
  private static final Set<String> RESTRICTED = Set.of("connection", "content-length", "expect", "host", "upgrade");

  private final MainThread mainThread;
  private final ScriptScope scope;
  private final Path workingDirectory;
  private final Spectra spectra;
  private final Function<UUID, Object> players;

  JsNatives(MainThread mainThread, ScriptScope scope, Path workingDirectory, Spectra spectra,
      Function<UUID, Object> players) {
    this.mainThread = mainThread;
    this.scope = scope;
    this.workingDirectory = workingDirectory.toAbsolutePath().normalize();
    this.spectra = spectra;
    this.players = players;
  }

  // ---- bytes ----

  /** Reads an ArrayBuffer, typed array, array, or string from JS into Java bytes. */
  static byte[] bytes(Value value) {
    if (value == null || value.isNull()) {
      return new byte[0];
    }
    if (value.isString()) {
      return value.asString().getBytes(StandardCharsets.UTF_8);
    }
    if (value.hasBufferElements()) {
      byte[] out = new byte[(int) value.getBufferSize()];
      value.readBuffer(0, out, 0, out.length);
      return out;
    }
    if (value.isHostObject() && value.asHostObject() instanceof byte[] array) {
      return array.clone();
    }
    if (value.hasArrayElements()) {
      byte[] out = new byte[(int) value.getArraySize()];
      for (int i = 0; i < out.length; i++) {
        out[i] = (byte) value.getArrayElement(i).asInt();
      }
      return out;
    }
    throw new IllegalArgumentException("Expected bytes, got " + value);
  }

  /** Wraps bytes as a host ByteBuffer; the prelude copies it into a Uint8Array. */
  public ByteBuffer buffer(byte[] bytes) {
    return ByteBuffer.wrap(bytes);
  }

  public String base64Encode(Value data) {
    return Base64.getEncoder().encodeToString(bytes(data));
  }

  public byte[] base64Decode(String text) {
    return Base64.getMimeDecoder().decode(text);
  }

  // ---- URL ----

  /** Parses {@code input} against {@code base}; returns [protocol, user, password, host, port, path, query, hash]. */
  public String[] parseUrl(String input, String base) {
    try {
      URI uri = base == null ? new URI(input.trim()) : new URI(base.trim()).resolve(new URI(input.trim()));
      if (uri.getScheme() == null) {
        return null;
      }
      String userInfo = uri.getRawUserInfo() == null ? "" : uri.getRawUserInfo();
      int colon = userInfo.indexOf(':');
      String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
      int port = uri.getPort();
      boolean defaultPort = port == -1 || (scheme.equals("http") || scheme.equals("ws")) && port == 80
          || (scheme.equals("https") || scheme.equals("wss")) && port == 443;
      String path = uri.isOpaque() ? uri.getRawSchemeSpecificPart() : uri.getRawPath();
      if (!uri.isOpaque() && uri.getRawAuthority() != null && (path == null || path.isEmpty())) {
        path = "/";
      }
      return new String[] {scheme + ":", colon < 0 ? userInfo : userInfo.substring(0, colon),
          colon < 0 ? "" : userInfo.substring(colon + 1),
          uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT), defaultPort ? "" : String.valueOf(port),
          path == null ? "" : path, uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery(),
          uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment()};
    } catch (URISyntaxException | IllegalArgumentException e) {
      return null;
    }
  }

  // ---- crypto ----

  public String randomUUID() {
    return UUID.randomUUID().toString();
  }

  public byte[] randomBytes(int count) {
    byte[] out = new byte[count];
    RANDOM.nextBytes(out);
    return out;
  }

  private static String javaAlgorithm(String algorithm) {
    return switch (algorithm.toLowerCase(Locale.ROOT).replace("-", "")) {
      case "sha1" -> "SHA-1";
      case "sha256" -> "SHA-256";
      case "sha384" -> "SHA-384";
      case "sha512" -> "SHA-512";
      case "md5" -> "MD5";
      default -> throw new IllegalArgumentException("Unsupported digest algorithm: " + algorithm);
    };
  }

  public byte[] digest(String algorithm, Value data) throws NoSuchAlgorithmException {
    return MessageDigest.getInstance(javaAlgorithm(algorithm)).digest(bytes(data));
  }

  public byte[] hmac(String algorithm, Value key, Value data) throws Exception {
    String name = "Hmac" + javaAlgorithm(algorithm).replace("-", "");
    Mac mac = Mac.getInstance(name);
    mac.init(new SecretKeySpec(bytes(key), name));
    return mac.doFinal(bytes(data));
  }

  // ---- time, process, os ----

  public double now() {
    return System.nanoTime() / 1_000_000.0;
  }

  public long nanoTime() {
    return System.nanoTime();
  }

  public double timeOrigin() {
    return System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getUptime();
  }

  public Map<String, String> env() {
    return Map.copyOf(System.getenv());
  }

  public String cwd() {
    return workingDirectory.toString();
  }

  public Map<String, Object> os() throws IOException {
    Runtime runtime = Runtime.getRuntime();
    Map<String, Object> os = new LinkedHashMap<>();
    String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    os.put("platform", name.contains("win") ? "win32" : name.contains("mac") ? "darwin" : name.contains("linux")
        ? "linux" : name);
    String arch = System.getProperty("os.arch", "");
    os.put("arch", arch.equals("amd64") || arch.equals("x86_64") ? "x64" : arch.equals("aarch64") ? "arm64" : arch);
    os.put("release", System.getProperty("os.version", ""));
    os.put("type", System.getProperty("os.name", ""));
    os.put("cpus", runtime.availableProcessors());
    os.put("totalmem", runtime.maxMemory());
    os.put("freemem", runtime.freeMemory());
    os.put("homedir", System.getProperty("user.home", ""));
    os.put("tmpdir", System.getProperty("java.io.tmpdir", ""));
    os.put("hostname", InetAddress.getLocalHost().getHostName());
    os.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0);
    os.put("pid", ProcessHandle.current().pid());
    os.put("username", System.getProperty("user.name", ""));
    return os;
  }

  // ---- fs ----

  private Path path(String path) {
    return workingDirectory.resolve(path).normalize();
  }

  public byte[] readFile(String path) throws IOException {
    return Files.readAllBytes(path(path));
  }

  public void writeFile(String path, Value data, boolean append) throws IOException {
    Path target = path(path);
    if (append) {
      Files.write(target, bytes(data), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } else {
      Files.write(target, bytes(data));
    }
  }

  public boolean exists(String path) {
    return Files.exists(path(path));
  }

  public void mkdir(String path, boolean recursive) throws IOException {
    if (recursive) {
      Files.createDirectories(path(path));
    } else {
      Files.createDirectory(path(path));
    }
  }

  public String[] readdir(String path) throws IOException {
    try (Stream<Path> entries = Files.list(path(path))) {
      return entries.map(entry -> entry.getFileName().toString()).sorted().toArray(String[]::new);
    }
  }

  public Map<String, Object> stat(String path, boolean followLinks) throws IOException {
    BasicFileAttributes attributes = followLinks
        ? Files.readAttributes(path(path), BasicFileAttributes.class)
        : Files.readAttributes(path(path), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    return Map.of("size", attributes.size(), "isFile", attributes.isRegularFile(), "isDirectory",
        attributes.isDirectory(), "isSymbolicLink", attributes.isSymbolicLink(), "mtimeMs",
        (double) attributes.lastModifiedTime().toMillis(), "birthtimeMs", (double) attributes.creationTime().toMillis(),
        "atimeMs", (double) attributes.lastAccessTime().toMillis());
  }

  public void rm(String path, boolean recursive, boolean force) throws IOException {
    Path target = path(path);
    if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
      if (force) {
        return;
      }
      throw new java.nio.file.NoSuchFileException(target.toString());
    }
    if (recursive && Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
      try (Stream<Path> walk = Files.walk(target)) {
        for (Path entry : walk.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(entry);
        }
      }
    } else {
      Files.delete(target);
    }
  }

  public void rename(String from, String to) throws IOException {
    Files.move(path(from), path(to), StandardCopyOption.REPLACE_EXISTING);
  }

  public void copyFile(String from, String to) throws IOException {
    Files.copy(path(from), path(to), StandardCopyOption.REPLACE_EXISTING);
  }

  public String realpath(String path) throws IOException {
    return path(path).toRealPath().toString();
  }

  // ---- fetch (outgoing HTTP) ----

  /** Handle returned to JS so a request can be aborted. */
  public static final class Abort {
    private final CompletableFuture<?> future;

    Abort(CompletableFuture<?> future) {
      this.future = future;
    }

    public void abort() {
      future.cancel(true);
    }
  }

  public Abort fetch(String method, String url, String[] headers, Value body, double timeoutMillis, Value callback) {
    java.net.http.HttpRequest.Builder request = java.net.http.HttpRequest.newBuilder(URI.create(url));
    for (int i = 0; i + 1 < headers.length; i += 2) {
      if (!RESTRICTED.contains(headers[i].toLowerCase(Locale.ROOT))) {
        request.header(headers[i], headers[i + 1]);
      }
    }
    byte[] payload = bytes(body);
    request.method(method, payload.length == 0 && (method.equals("GET") || method.equals("HEAD"))
        ? BodyPublishers.noBody() : BodyPublishers.ofByteArray(payload));
    if (timeoutMillis > 0) {
      request.timeout(Duration.ofMillis((long) timeoutMillis));
    }
    CompletableFuture<java.net.http.HttpResponse<byte[]>> future =
        HTTP.sendAsync(request.build(), BodyHandlers.ofByteArray());
    future.whenComplete((response, error) -> deliver("fetch " + url, () -> {
      if (error != null) {
        Throwable cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null
            ? error.getCause() : error;
        callback.executeVoid(null, null, null, null, String.valueOf(cause.getMessage() == null ? cause : cause.getMessage()));
        return;
      }
      List<String> flat = new ArrayList<>();
      response.headers().map().forEach((name, values) -> values.forEach(value -> {
        flat.add(name);
        flat.add(value);
      }));
      callback.executeVoid(response.statusCode(), flat.toArray(String[]::new), ByteBuffer.wrap(response.body()),
          response.uri().toString(), null);
    }));
    return new Abort(future);
  }

  private void deliver(String what, Runnable callback) {
    mainThread.execute(() -> scope.invoke(what, callback));
  }

  // ---- outgoing WebSocket ----

  /** JS-facing handle of an outgoing socket. */
  public static final class ClientSocket {
    private final CompletableFuture<java.net.http.WebSocket> socket;

    ClientSocket(CompletableFuture<java.net.http.WebSocket> socket) {
      this.socket = socket;
    }

    public void sendText(String text) {
      socket.thenAccept(ws -> ws.sendText(text, true));
    }

    public void sendBinary(Value data) {
      byte[] bytes = bytes(data);
      socket.thenAccept(ws -> ws.sendBinary(ByteBuffer.wrap(bytes), true));
    }

    public void close(int code, String reason) {
      socket.thenAccept(ws -> ws.sendClose(code, reason == null ? "" : reason));
    }
  }

  public ClientSocket connect(String url, String[] protocols, Value events) {
    java.net.http.WebSocket.Builder builder = HTTP.newWebSocketBuilder();
    if (protocols.length > 0) {
      builder.subprotocols(protocols[0], java.util.Arrays.copyOfRange(protocols, 1, protocols.length));
    }
    java.net.http.WebSocket.Listener listener = new java.net.http.WebSocket.Listener() {
      private final StringBuilder text = new StringBuilder();
      private final java.io.ByteArrayOutputStream binary = new java.io.ByteArrayOutputStream();

      @Override
      public void onOpen(java.net.http.WebSocket ws) {
        String protocol = ws.getSubprotocol();
        deliver("WebSocket open", () -> events.invokeMember("open", protocol));
        ws.request(1);
      }

      @Override
      public CompletionStage<?> onText(java.net.http.WebSocket ws, CharSequence data, boolean last) {
        text.append(data);
        if (last) {
          String message = text.toString();
          text.setLength(0);
          deliver("WebSocket message", () -> events.invokeMember("message", message, false));
        }
        ws.request(1);
        return null;
      }

      @Override
      public CompletionStage<?> onBinary(java.net.http.WebSocket ws, ByteBuffer data, boolean last) {
        byte[] chunk = new byte[data.remaining()];
        data.get(chunk);
        binary.writeBytes(chunk);
        if (last) {
          ByteBuffer message = ByteBuffer.wrap(binary.toByteArray());
          binary.reset();
          deliver("WebSocket message", () -> events.invokeMember("message", message, true));
        }
        ws.request(1);
        return null;
      }

      @Override
      public CompletionStage<?> onClose(java.net.http.WebSocket ws, int code, String reason) {
        deliver("WebSocket close", () -> events.invokeMember("close", code, reason));
        return null;
      }

      @Override
      public void onError(java.net.http.WebSocket ws, Throwable error) {
        deliver("WebSocket error", () -> events.invokeMember("error", String.valueOf(error.getMessage())));
      }
    };
    CompletableFuture<java.net.http.WebSocket> socket = builder.buildAsync(URI.create(url), listener);
    socket.whenComplete((ws, error) -> {
      if (error != null) {
        deliver("WebSocket error", () -> {
          events.invokeMember("error", String.valueOf(error.getMessage()));
          events.invokeMember("close", 1006, "");
        });
      }
    });
    scope.track(() -> socket.thenAccept(ws -> ws.abort()));
    return new ClientSocket(socket);
  }

  // ---- incoming requests from views (the 'fetch' and 'websocket' events) ----

  public Object player(String uuid) {
    return players.apply(UUID.fromString(uuid));
  }

  /**
   * Routes every tunneled view request to {@code dispatch(request, respond)}. The JS side calls
   * {@code respond(status, headers, body)} or {@code respond(null)} to pass the request on.
   */
  public Registration serveHttp(Value dispatch) {
    requireSpectra();
    return scope.track(spectra.http().route("/", (HttpRequest request) -> {
      CompletableFuture<HttpResponse> response = new CompletableFuture<>();
      List<String> flat = new ArrayList<>();
      request.headers().forEach((name, values) -> values.forEach(value -> {
        flat.add(name);
        flat.add(value);
      }));
      ProxyExecutable respond = args -> {
        if (args.length == 0 || args[0].isNull()) {
          response.complete(null);
          return null;
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Value list = args[1];
        for (long i = 0; i + 1 < list.getArraySize(); i += 2) {
          headers.computeIfAbsent(list.getArrayElement(i).asString().toLowerCase(Locale.ROOT),
              name -> new ArrayList<>()).add(list.getArrayElement(i + 1).asString());
        }
        response.complete(new HttpResponse(args[0].asInt(), headers, bytes(args[2])));
        return null;
      };
      boolean invoked = scope.invoke("fetch event", () -> {
        dispatch.executeVoid(request.player().toString(), request.viewId(), request.method(), request.path(),
            flat.toArray(String[]::new), ByteBuffer.wrap(request.body()), respond);
        return true;
      }, false);
      if (!invoked) {
        response.completeExceptionally(new IllegalStateException("fetch event handler failed"));
      }
      return response;
    }));
  }

  /** JS-facing server end of a view's socket. */
  public static final class ServerSocket {
    private final WebSocket socket;
    private final ScriptScope scope;

    ServerSocket(WebSocket socket, ScriptScope scope) {
      this.socket = socket;
      this.scope = scope;
    }

    public void sendText(String text) {
      socket.send(text);
    }

    public void sendBinary(Value data) {
      socket.send(bytes(data));
    }

    public void close(int code, String reason) {
      socket.close(code, reason == null ? "" : reason);
    }

    public boolean isOpen() {
      return socket.isOpen();
    }

    public void listen(Value events) {
      scope.track(socket.onMessage(message -> scope.invoke("socket message", () ->
          events.invokeMember("message", message.binary() ? ByteBuffer.wrap(message.data()) : message.text(),
              message.binary()))));
      scope.track(socket.onClose(close -> scope.invoke("socket close", () ->
          events.invokeMember("close", close.code(), close.reason()))));
    }
  }

  /** Routes every tunneled view socket to {@code dispatch(info..., accept)}; {@code accept()} returns the socket. */
  public Registration serveSockets(Value dispatch) {
    requireSpectra();
    return scope.track(spectra.sockets().route("/", socket -> {
      ServerSocket[] accepted = new ServerSocket[1];
      ProxyExecutable accept = args -> {
        if (accepted[0] == null) {
          accepted[0] = new ServerSocket(socket, scope);
        }
        return accepted[0];
      };
      scope.invoke("websocket event", () -> dispatch.executeVoid(socket.player().toString(), socket.viewId(),
          socket.path(), accept));
      return accepted[0] != null;
    }));
  }

  private void requireSpectra() {
    if (spectra == null) {
      throw new IllegalStateException("View requests are not available on this platform");
    }
  }
}
