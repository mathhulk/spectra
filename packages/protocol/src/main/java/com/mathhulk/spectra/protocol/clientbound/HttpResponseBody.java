package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

/** More response body for {@code requestId}. */
public record HttpResponseBody(int requestId, byte[] data, boolean last) implements ClientboundPacket {
  public static HttpResponseBody read(FrameReader reader) {
    return new HttpResponseBody(reader.readVarInt(), reader.readBytes(Limits.MAX_CLIENTBOUND_BODY), reader.readBool());
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(requestId).writeBytes(data).writeBool(last);
  }
}
