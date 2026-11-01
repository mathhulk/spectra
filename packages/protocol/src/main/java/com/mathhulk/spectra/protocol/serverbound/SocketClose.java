package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;

/** The view closed its socket (or the view went away). */
public record SocketClose(int socketId, int code, String reason) implements ServerboundPacket {
  public static SocketClose read(FrameReader reader) {
    return new SocketClose(reader.readVarInt(), reader.readVarInt(), reader.readString(Limits.MAX_SHORT_TEXT));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(socketId).writeVarInt(code).writeString(reason, Limits.MAX_SHORT_TEXT);
  }
}
