package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;

/** More request body for {@code requestId}. */
public record HttpRequestBody(int requestId, byte[] data, boolean last) implements ServerboundPacket {
  public static HttpRequestBody read(FrameReader reader) {
    return new HttpRequestBody(reader.readVarInt(), reader.readBytes(Limits.MAX_SERVERBOUND_BODY), reader.readBool());
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(requestId).writeBytes(data).writeBool(last);
  }
}
