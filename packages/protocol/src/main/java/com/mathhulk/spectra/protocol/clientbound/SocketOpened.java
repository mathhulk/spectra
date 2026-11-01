package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

/** The server accepted {@code socketId}; {@code protocol} is the selected subprotocol or empty. */
public record SocketOpened(int socketId, String protocol) implements ClientboundPacket {
  public static SocketOpened read(FrameReader reader) {
    return new SocketOpened(reader.readVarInt(), reader.readString(Limits.MAX_HEADER_NAME));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(socketId).writeString(protocol, Limits.MAX_HEADER_NAME);
  }
}
