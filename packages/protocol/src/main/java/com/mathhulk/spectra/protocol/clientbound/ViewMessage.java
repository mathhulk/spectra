package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

/** JSON message for a view. An empty {@code viewId} targets every open view. */
public record ViewMessage(String viewId, String json) implements ClientboundPacket {
  public static ViewMessage read(FrameReader reader) {
    return new ViewMessage(reader.readString(Limits.MAX_ID), reader.readString(Limits.MAX_CLIENTBOUND_JSON));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeString(viewId, Limits.MAX_ID).writeString(json, Limits.MAX_CLIENTBOUND_JSON);
  }
}
