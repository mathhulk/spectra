package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Header;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;

import java.util.List;

/**
 * Start of an HTTP request a view made to its own origin under {@code /api/}. {@code requestId} is chosen by the
 * client and unique among its in-flight requests. When {@code last} is false the body continues in
 * {@link HttpRequestBody} packets.
 */
public record HttpRequest(int requestId, String viewId, String method, String path, List<Header> headers,
    byte[] body, boolean last) implements ServerboundPacket {
  public HttpRequest {
    headers = List.copyOf(headers);
  }

  public static HttpRequest read(FrameReader reader) {
    return new HttpRequest(reader.readVarInt(), reader.readString(Limits.MAX_ID),
        reader.readString(Limits.MAX_HTTP_METHOD), reader.readString(Limits.MAX_HTTP_PATH), Header.readList(reader),
        reader.readBytes(Limits.MAX_SERVERBOUND_BODY), reader.readBool());
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(requestId).writeString(viewId, Limits.MAX_ID).writeString(method, Limits.MAX_HTTP_METHOD)
        .writeString(path, Limits.MAX_HTTP_PATH);
    Header.writeList(writer, headers);
    writer.writeBytes(body).writeBool(last);
  }
}
