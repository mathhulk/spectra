package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

/** The server closed (or rejected) {@code socketId}. */
public record SocketClosed(int socketId, int code, String reason) implements ClientboundPacket {
  public static SocketClosed read(FrameReader reader) {
    return new SocketClosed(reader.readVarInt(), reader.readVarInt(), reader.readString(Limits.MAX_SHORT_TEXT));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(socketId).writeVarInt(code).writeString(reason, Limits.MAX_SHORT_TEXT);
  }
}
