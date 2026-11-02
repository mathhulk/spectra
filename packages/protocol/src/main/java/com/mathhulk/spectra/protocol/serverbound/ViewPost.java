package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;

/** JSON a view sent with {@code window.spectra.send(...)}. Untrusted input. */
public record ViewPost(String viewId, String json) implements ServerboundPacket {
  public static ViewPost read(FrameReader reader) {
    return new ViewPost(reader.readString(Limits.MAX_ID), reader.readString(Limits.MAX_SERVERBOUND_JSON));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeString(viewId, Limits.MAX_ID).writeString(json, Limits.MAX_SERVERBOUND_JSON);
  }
}
