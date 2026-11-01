package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Header;
import com.mathhulk.spectra.protocol.Limits;

import java.util.List;

/** Start of the response to {@code requestId}; the body continues in {@link HttpResponseBody} unless last. */
public record HttpResponse(int requestId, int status, List<Header> headers, byte[] body, boolean last)
    implements ClientboundPacket {
  public HttpResponse {
    headers = List.copyOf(headers);
  }

  public static HttpResponse read(FrameReader reader) {
    return new HttpResponse(reader.readVarInt(), reader.readVarInt(), Header.readList(reader),
        reader.readBytes(Limits.MAX_CLIENTBOUND_BODY), reader.readBool());
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(requestId).writeVarInt(status);
    Header.writeList(writer, headers);
    writer.writeBytes(body).writeBool(last);
  }
}
