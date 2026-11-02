package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

/** Shows or hides an open view without destroying it. */
public record SetViewVisible(String viewId, boolean visible) implements ClientboundPacket {
  public static SetViewVisible read(FrameReader reader) {
    return new SetViewVisible(reader.readString(Limits.MAX_ID), reader.readBool());
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeString(viewId, Limits.MAX_ID).writeBool(visible);
  }
}
