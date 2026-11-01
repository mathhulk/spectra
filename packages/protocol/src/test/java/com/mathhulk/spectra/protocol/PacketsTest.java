package com.mathhulk.spectra.protocol;

import com.mathhulk.spectra.protocol.clientbound.*;
import com.mathhulk.spectra.protocol.serverbound.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PacketsTest {
  private static final ResourceHash HASH = ResourceHash.compute("hello".getBytes(StandardCharsets.UTF_8));

  private static void roundTrip(ClientboundPacket packet) {
    assertEquals(packet, Packets.decodeClientbound(Packets.encode(packet)));
  }

  private static void roundTrip(ServerboundPacket packet) {
    assertEquals(packet, Packets.decodeServerbound(Packets.encode(packet)));
  }

  @Test
  void clientboundRoundTrip() {
    roundTrip(new Welcome(Protocol.VERSION, true, "", "server-1", Set.of(Capability.SCREENS, Capability.LAYERS)));
    roundTrip(new ResourceManifest(42, 0, 2, List.of(new ResourceManifest.Entry("ui/index.html", HASH, 5))));
    roundTrip(new OpenView("shop", ViewKind.SCREEN, "shop/index.html", Layout.FULLSCREEN, 0, true, Optional.empty()));
    roundTrip(new OpenView("hud", ViewKind.LAYER, "hud.html", new Layout(Anchor.BOTTOM_RIGHT, -4, 8, 100, 40), 3,
        false, Optional.of("chat")));
    roundTrip(new SetViewVisible("hud", false));
    roundTrip(new CloseView("hud"));
    roundTrip(new ViewMessage("", "{\"a\":1}"));
  }

  @Test
  void chunkRoundTripPreservesBytes() {
    byte[] data = {1, 2, 3, (byte) 0xFF};
    ResourceChunk decoded = (ResourceChunk) Packets.decodeClientbound(
        Packets.encode(new ResourceChunk(HASH, 1, 3, data)));
    assertEquals(HASH, decoded.hash());
    assertEquals(1, decoded.index());
    assertEquals(3, decoded.count());
    assertArrayEquals(data, decoded.data());
  }

  @Test
  void serverboundRoundTrip() {
    roundTrip(new Hello(Protocol.VERSION, "1.0.0", Set.of(Capability.RESOURCES)));
    roundTrip(new ResourceRequest(7, List.of(HASH)));
    roundTrip(new ResourcesSynced(7));
    roundTrip(new ViewPost("shop", "{\"buy\":\"sword\"}"));
    roundTrip(new ViewEvent("shop", ViewEvent.Type.CLOSED, "escape"));
  }

  @Test
  void rejectsUnknownIdsTrailingBytesAndTruncation() {
    assertThrows(ProtocolException.class, () -> Packets.decodeServerbound(new byte[] {99}));

    byte[] frame = Packets.encode(new ResourcesSynced(1));
    byte[] trailing = java.util.Arrays.copyOf(frame, frame.length + 1);
    assertThrows(ProtocolException.class, () -> Packets.decodeServerbound(trailing));

    byte[] hello = Packets.encode(new Hello(1, "x", Set.of("a")));
    byte[] truncated = java.util.Arrays.copyOf(hello, hello.length - 1);
    assertThrows(ProtocolException.class, () -> Packets.decodeServerbound(truncated));
  }

  @Test
  void rejectsOversizedFramesAndLists() {
    assertThrows(ProtocolException.class, () -> Packets.decodeServerbound(new byte[Limits.MAX_SERVERBOUND_FRAME + 1]));

    // A list header claiming more hashes than allowed must fail before allocating anything.
    FrameWriter writer = new FrameWriter().writeVarInt(1).writeVarLong(0).writeVarInt(Integer.MAX_VALUE);
    assertThrows(ProtocolException.class, () -> Packets.decodeServerbound(writer.toByteArray()));

    String huge = "x".repeat(Limits.MAX_SERVERBOUND_JSON + 1);
    assertThrows(ProtocolException.class, () -> Packets.encode(new ViewPost("v", huge)));
  }

  @Test
  void maxSizeRequestFitsInServerboundFrame() {
    List<ResourceHash> hashes = java.util.Collections.nCopies(Limits.MAX_REQUEST_HASHES, HASH);
    assertTrue(Packets.encode(new ResourceRequest(Long.MAX_VALUE, hashes)).length <= Limits.MAX_SERVERBOUND_FRAME);
  }

  @Test
  void maxSizeChunkFitsInClientboundFrame() {
    byte[] data = new byte[Limits.MAX_CHUNK_SIZE];
    assertTrue(Packets.encode(new ResourceChunk(HASH, 0, 1, data)).length <= Limits.MAX_CLIENTBOUND_FRAME);
  }

  @Test
  void varIntsRoundTripIncludingNegatives() {
    for (int value : new int[] {0, 1, 127, 128, 25565, Integer.MAX_VALUE, -1, Integer.MIN_VALUE}) {
      assertEquals(value, new FrameReader(new FrameWriter().writeVarInt(value).toByteArray()).readVarInt());
    }
    for (long value : new long[] {0, 300, Long.MAX_VALUE, -1, Long.MIN_VALUE}) {
      assertEquals(value, new FrameReader(new FrameWriter().writeVarLong(value).toByteArray()).readVarLong());
    }
  }

  @Test
  void tunnelPacketsRoundTrip() {
    roundTrip(new SocketOpened(1, ""));
    roundTrip(new SocketClosed(1, 1000, "bye"));
    roundTrip(new NetworkPolicy(List.of("https://*.example.com")));
    roundTrip(new SocketOpen(1, "v", "/api/chat", List.of("json")));
    roundTrip(new SocketClose(1, 1001, "gone"));
    var request = (com.mathhulk.spectra.protocol.serverbound.HttpRequest) Packets.decodeServerbound(Packets.encode(
        new com.mathhulk.spectra.protocol.serverbound.HttpRequest(5, "v", "POST", "/api/x?y=1",
            List.of(new Header("X-Test", "1")), new byte[] {1, 2}, false)));
    assertEquals("x-test", request.headers().getFirst().name());
    assertArrayEquals(new byte[] {1, 2}, request.body());
  }

  @Test
  void maxSizeTunnelPacketsFitTheirFrames() {
    var body = new com.mathhulk.spectra.protocol.serverbound.HttpRequest(Integer.MAX_VALUE, "v".repeat(Limits.MAX_ID),
        "OPTIONS", "/".repeat(Limits.MAX_HTTP_PATH), List.of(), new byte[Limits.MAX_SERVERBOUND_BODY], false);
    assertTrue(Packets.encode(body).length <= Limits.MAX_SERVERBOUND_FRAME);
    var send = new SocketSend(Integer.MAX_VALUE, true, new byte[Limits.MAX_SERVERBOUND_SOCKET_DATA], false);
    assertTrue(Packets.encode(send).length <= Limits.MAX_SERVERBOUND_FRAME);
  }

  @Test
  void networkPolicyMatchesOriginsAndWildcards() {
    NetworkPolicy policy = new NetworkPolicy(List.of("https://api.example.com", "https://*.tiles.example.org"));
    assertTrue(policy.allows("https://api.example.com/v1/players"));
    assertTrue(policy.allows("wss://api.example.com/live"));
    assertTrue(policy.allows("https://a.b.tiles.example.org/1/2/3.png"));
    assertFalse(policy.allows("https://tiles.example.org/x"));
    assertFalse(policy.allows("http://api.example.com/"));
    assertFalse(policy.allows("https://api.example.com:8443/"));
    assertFalse(policy.allows("https://evil.com/?https://api.example.com"));
    assertFalse(policy.allows("https://api.example.com.evil.com/"));
  }
}
