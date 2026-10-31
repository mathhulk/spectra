package com.mathhulk.spectra.protocol;

import com.mathhulk.spectra.protocol.clientbound.*;
import com.mathhulk.spectra.protocol.serverbound.*;

import java.util.List;
import java.util.function.Function;

/**
 * Packet IDs and frame encoding. IDs are part of the wire format: append new packets, never reorder.
 */
public final class Packets {
  private record Type<T extends Packet>(Class<T> type, Function<FrameReader, T> reader) {
  }

  private static final List<Type<? extends ClientboundPacket>> CLIENTBOUND = List.of(
      new Type<>(Welcome.class, Welcome::read),
      new Type<>(ResourceManifest.class, ResourceManifest::read),
      new Type<>(ResourceChunk.class, ResourceChunk::read),
      new Type<>(OpenView.class, OpenView::read),
      new Type<>(SetViewVisible.class, SetViewVisible::read),
      new Type<>(CloseView.class, CloseView::read),
      new Type<>(ViewMessage.class, ViewMessage::read));

  private static final List<Type<? extends ServerboundPacket>> SERVERBOUND = List.of(
      new Type<>(Hello.class, Hello::read),
      new Type<>(ResourceRequest.class, ResourceRequest::read),
      new Type<>(ResourcesSynced.class, ResourcesSynced::read),
      new Type<>(ViewPost.class, ViewPost::read),
      new Type<>(ViewEvent.class, ViewEvent::read));

  private Packets() {
  }

  private static int idOf(List<? extends Type<?>> types, Packet packet) {
    for (int i = 0; i < types.size(); i++) {
      if (types.get(i).type() == packet.getClass()) {
        return i;
      }
    }
    throw new IllegalArgumentException("Unregistered packet " + packet.getClass().getName());
  }

  private static byte[] encode(List<? extends Type<?>> types, Packet packet, int maxSize) {
    FrameWriter writer = new FrameWriter();
    writer.writeVarInt(idOf(types, packet));
    packet.write(writer);
    if (writer.size() > maxSize) {
      throw new ProtocolException(packet.getClass().getSimpleName() + " frame is " + writer.size()
          + " bytes, limit is " + maxSize);
    }
    return writer.toByteArray();
  }

  private static <T extends Packet> T decode(List<? extends Type<? extends T>> types, byte[] frame, int maxSize) {
    if (frame.length > maxSize) {
      throw new ProtocolException("Frame is " + frame.length + " bytes, limit is " + maxSize);
    }
    FrameReader reader = new FrameReader(frame);
    int id = reader.readVarInt();
    if (id < 0 || id >= types.size()) {
      throw new ProtocolException("Unknown packet id " + id);
    }
    T packet = types.get(id).reader().apply(reader);
    reader.expectEnd();
    return packet;
  }

  public static byte[] encode(ClientboundPacket packet) {
    return encode(CLIENTBOUND, packet, Limits.MAX_CLIENTBOUND_FRAME);
  }

  public static byte[] encode(ServerboundPacket packet) {
    return encode(SERVERBOUND, packet, Limits.MAX_SERVERBOUND_FRAME);
  }

  public static ClientboundPacket decodeClientbound(byte[] frame) {
    return decode(CLIENTBOUND, frame, Limits.MAX_CLIENTBOUND_FRAME);
  }

  public static ServerboundPacket decodeServerbound(byte[] frame) {
    return decode(SERVERBOUND, frame, Limits.MAX_SERVERBOUND_FRAME);
  }
}
