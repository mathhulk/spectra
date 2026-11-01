package com.mathhulk.spectra.runtime.js;

import com.mathhulk.spectra.api.views.ViewSpec;
import com.mathhulk.spectra.core.FakePlatform;
import com.mathhulk.spectra.core.SpectraConfig;
import com.mathhulk.spectra.core.SpectraServer;
import com.mathhulk.spectra.core.scripts.ScriptManager;
import com.mathhulk.spectra.protocol.Capability;
import com.mathhulk.spectra.protocol.Header;
import com.mathhulk.spectra.protocol.Packets;
import com.mathhulk.spectra.protocol.Protocol;
import com.mathhulk.spectra.protocol.clientbound.HttpResponse;
import com.mathhulk.spectra.protocol.clientbound.ResourceManifest;
import com.mathhulk.spectra.protocol.clientbound.SocketMessage;
import com.mathhulk.spectra.protocol.clientbound.SocketOpened;
import com.mathhulk.spectra.protocol.serverbound.Hello;
import com.mathhulk.spectra.protocol.serverbound.HttpRequest;
import com.mathhulk.spectra.protocol.serverbound.ResourcesSynced;
import com.mathhulk.spectra.protocol.serverbound.SocketOpen;
import com.mathhulk.spectra.protocol.serverbound.SocketSend;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class CompatibilityTest {
  @TempDir
  Path directory;

  private FakePlatform platform;
  private SpectraServer server;
  private JsLanguage language;
  private ScriptManager manager;
  private final List<String> recorded = new CopyOnWriteArrayList<>();
  private final UUID player = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    platform = new FakePlatform(directory.resolve("data"));
    server = new SpectraServer(platform, SpectraConfig.DEFAULTS);
    server.start();
    language = new JsLanguage(platform.mainThread(), platform.logger(), getClass().getClassLoader(), directory,
        server, uuid -> "player:" + uuid);
    Files.createDirectories(directory.resolve("scripts"));
    manager = new ScriptManager(platform, directory.resolve("scripts"), List.of(language),
        scope -> Map.of("record", (Consumer<Object>) value -> recorded.add(String.valueOf(value))));
  }

  @AfterEach
  void tearDown() {
    manager.close();
    language.close();
    server.close();
  }

  private void run(String code) throws Exception {
    Files.writeString(directory.resolve("scripts/test.mjs"), code);
    manager.loadAll();
    manager.enableAll();
  }

  private void await(int count) throws InterruptedException {
    for (int i = 0; i < 500 && recorded.size() < count; i++) {
      platform.tick();
      Thread.sleep(10);
    }
  }

  @Test
  void webPlatformGlobals() throws Exception {
    run("""
        const url = new URL('/a/b?x=1&y=two#frag', 'https://example.com:8443/base/');
        url.searchParams.append('z', '3 4');
        record(url.href);
        record([url.origin, url.pathname, url.searchParams.get('y')].join(' '));
        const bytes = new TextEncoder().encode('héllo 🌍');
        record(bytes.length + ' ' + new TextDecoder().decode(bytes));
        record(atob(btoa('spectra')));
        const clone = structuredClone({ a: [1, { b: new Map([[1, 2]]) }] });
        record(clone.a[1].b.get(1));
        const target = new EventTarget();
        target.addEventListener('ping', (e) => record('event ' + e.detail), { once: true });
        target.dispatchEvent(new CustomEvent('ping', { detail: 1 }));
        target.dispatchEvent(new CustomEvent('ping', { detail: 2 }));
        const controller = new AbortController();
        controller.signal.addEventListener('abort', () => record('aborted ' + controller.signal.aborted));
        controller.abort();
        record(/^[0-9a-f-]{36}$/.test(crypto.randomUUID()));
        const response = Response.json({ ok: true }, { status: 201 });
        record(response.status + ' ' + response.headers.get('content-type'));
        record(JSON.stringify(await response.json()));
        record(typeof performance.now() + ' ' + typeof process.env + ' ' + process.platform.length > 0);
        queueMicrotask(() => record('microtask'));
        """);
    await(12);
    assertEquals("https://example.com:8443/a/b?x=1&y=two&z=3+4#frag", recorded.get(0));
    assertEquals("https://example.com:8443 /a/b two", recorded.get(1));
    assertEquals("11 héllo 🌍", recorded.get(2));
    assertEquals("spectra", recorded.get(3));
    assertEquals("2", recorded.get(4));
    assertEquals("event 1", recorded.get(5));
    assertEquals("aborted true", recorded.get(6));
    assertEquals("true", recorded.get(7));
    assertEquals("201 application/json", recorded.get(8));
    assertEquals("{\"ok\":true}", recorded.get(9));
    assertTrue(recorded.contains("microtask"));
  }

  @Test
  void nodeBuiltins() throws Exception {
    run("""
        import path from 'node:path';
        import { join } from 'path';
        import { EventEmitter } from 'events';
        import fs from 'node:fs';
        import { readFile } from 'fs/promises';
        import { createHash, randomBytes } from 'crypto';
        import { Buffer } from 'node:buffer';
        import { format } from 'node:util';
        import os from 'node:os';
        import assert from 'node:assert';

        record(path.join('/a', 'b', '../c', 'd.txt') + ' ' + join('x', 'y') + ' ' + path.extname('f.tar.gz'));
        record(path.relative('/a/b/c', '/a/d') + ' ' + path.dirname('/a/b/c.txt') + ' ' + path.basename('/a/b/c.txt', '.txt'));
        const emitter = new EventEmitter();
        emitter.once('hi', (name) => record('hello ' + name));
        emitter.emit('hi', 'node');
        emitter.emit('hi', 'again');
        fs.mkdirSync('out/nested', { recursive: true });
        fs.writeFileSync('out/nested/file.txt', 'written by fs');
        record(fs.readFileSync('out/nested/file.txt', 'utf8') + ' ' + fs.existsSync('out/nested') + ' ' + fs.readdirSync('out').join());
        record(createHash('sha256').update('abc').digest('hex'));
        record(Buffer.from('spectra').toString('base64') + ' ' + Buffer.from('c3BlY3RyYQ==', 'base64').toString());
        record(randomBytes(8).length + ' ' + format('%s=%d', 'answer', 42) + ' ' + (os.platform().length > 0));
        assert.strictEqual(1, 1);
        record(await readFile('out/nested/file.txt', 'utf8'));
        """);
    await(8);
    assertEquals("/a/c/d.txt x/y .gz", recorded.get(0));
    assertEquals("../../d /a/b c", recorded.get(1));
    assertEquals("hello node", recorded.get(2));
    assertEquals("written by fs true nested", recorded.get(3));
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", recorded.get(4));
    assertEquals("c3BlY3RyYQ== spectra", recorded.get(5));
    assertEquals("8 answer=42 true", recorded.get(6));
    assertEquals("written by fs", recorded.get(7));
    assertEquals("written by fs", Files.readString(directory.resolve("out/nested/file.txt")));
  }

  @Test
  void fetchReachesRealHttpServers() throws Exception {
    HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    http.createContext("/echo", exchange -> {
      byte[] body = exchange.getRequestBody().readAllBytes();
      byte[] reply = ("{\"method\":\"" + exchange.getRequestMethod() + "\",\"body\":\""
          + new String(body, StandardCharsets.UTF_8) + "\",\"agent\":\"" + exchange.getRequestHeaders().getFirst("x-test")
          + "\"}").getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("content-type", "application/json");
      exchange.sendResponseHeaders(200, reply.length);
      exchange.getResponseBody().write(reply);
      exchange.close();
    });
    http.start();
    try {
      run("""
          const response = await fetch('http://127.0.0.1:%d/echo', {
            method: 'POST', body: 'payload', headers: { 'x-test': 'spectra' },
          });
          record(response.status + ' ' + response.headers.get('content-type'));
          const data = await response.json();
          record(data.method + ' ' + data.body + ' ' + data.agent);
          try { await fetch('http://127.0.0.1:1/'); } catch (error) { record('failed ' + (error instanceof TypeError)); }
          """.formatted(http.getAddress().getPort()));
      await(3);
      assertEquals("200 application/json", recorded.get(0));
      assertEquals("POST payload spectra", recorded.get(1));
      assertEquals("failed true", recorded.get(2));
    } finally {
      http.stop(0);
    }
  }

  private void connectPlayerWithView() throws Exception {
    Files.createDirectories(directory.resolve("data/resources"));
    Files.writeString(directory.resolve("data/resources/app.html"), "app");
    server.rescanResources();
    server.join(player);
    server.handleFrame(player, Packets.encode(new Hello(Protocol.VERSION, "test", Set.of(Capability.RESOURCES,
        Capability.SCREENS, Capability.HTTP, Capability.SOCKETS))));
    long generation = platform.drain(player, ResourceManifest.class).getLast().generation();
    server.handleFrame(player, Packets.encode(new ResourcesSynced(generation)));
    server.views().open(player, "app", ViewSpec.screen("app.html"));
  }

  @Test
  void viewRequestsReachFetchAndWebSocketListeners() throws Exception {
    run("""
        addEventListener('fetch', (event) => {
          const url = new URL(event.request.url);
          if (url.pathname !== '/api/hello') return;
          event.respondWith((async () => Response.json({
            greeting: 'hi ' + url.searchParams.get('name'),
            player: String(event.player),
            body: await event.request.text(),
          }))());
        });
        addEventListener('websocket', (event) => {
          const socket = event.accept();
          socket.addEventListener('message', (message) => socket.send('echo:' + message.data));
        });
        """);
    connectPlayerWithView();

    server.handleFrame(player, Packets.encode(new HttpRequest(1, "app", "POST", "/api/hello?name=steve",
        List.of(new Header("content-type", "text/plain")), "body!".getBytes(StandardCharsets.UTF_8), true)));
    HttpResponse response = platform.drain(player, HttpResponse.class).getFirst();
    assertEquals(200, response.status());
    assertEquals("{\"greeting\":\"hi steve\",\"player\":\"player:" + player + "\",\"body\":\"body!\"}",
        new String(response.body(), StandardCharsets.UTF_8));

    server.handleFrame(player, Packets.encode(new HttpRequest(2, "app", "GET", "/api/other", List.of(),
        new byte[0], true)));
    assertEquals(404, platform.drain(player, HttpResponse.class).getFirst().status());

    server.handleFrame(player, Packets.encode(new SocketOpen(5, "app", "/api/chat", List.of())));
    assertEquals(5, platform.drain(player, SocketOpened.class).getFirst().socketId());
    server.handleFrame(player, Packets.encode(new SocketSend(5, false, "ping".getBytes(StandardCharsets.UTF_8), true)));
    assertEquals("echo:ping", new String(platform.drain(player, SocketMessage.class).getFirst().data(),
        StandardCharsets.UTF_8));
  }
}
