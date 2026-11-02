package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

/** A message fragment for a view's socket; {@code fin} marks the last fragment. */
public record SocketMessage(int socketId, boolean binary, byte[] data, boolean fin) implements ClientboundPacket {
  public static SocketMessage read(FrameReader reader) {
    return new SocketMessage(reader.readVarInt(), reader.readBool(),
        reader.readBytes(Limits.MAX_CLIENTBOUND_SOCKET_DATA), reader.readBool());
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(socketId).writeBool(binary).writeBytes(data).writeBool(fin);
  }
}
