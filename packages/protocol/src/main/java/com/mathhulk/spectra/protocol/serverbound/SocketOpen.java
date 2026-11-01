package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;

import java.util.List;

/** A view opened {@code new WebSocket('/api/...')}. {@code socketId} is chosen by the client. */
public record SocketOpen(int socketId, String viewId, String path, List<String> protocols)
    implements ServerboundPacket {
  public SocketOpen {
    protocols = List.copyOf(protocols);
  }

  public static SocketOpen read(FrameReader reader) {
    return new SocketOpen(reader.readVarInt(), reader.readString(Limits.MAX_ID),
        reader.readString(Limits.MAX_HTTP_PATH),
        reader.readList(Limits.MAX_SOCKET_PROTOCOLS, r -> r.readString(Limits.MAX_HEADER_NAME)));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(socketId).writeString(viewId, Limits.MAX_ID).writeString(path, Limits.MAX_HTTP_PATH)
        .writeList(protocols, Limits.MAX_SOCKET_PROTOCOLS, (w, p) -> w.writeString(p, Limits.MAX_HEADER_NAME));
  }
}
