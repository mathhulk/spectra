package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;

/** A message fragment from a view's socket; {@code fin} marks the last fragment of a message. */
public record SocketSend(int socketId, boolean binary, byte[] data, boolean fin) implements ServerboundPacket {
  public static SocketSend read(FrameReader reader) {
    return new SocketSend(reader.readVarInt(), reader.readBool(), reader.readBytes(Limits.MAX_SERVERBOUND_SOCKET_DATA),
        reader.readBool());
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(socketId).writeBool(binary).writeBytes(data).writeBool(fin);
  }
}
