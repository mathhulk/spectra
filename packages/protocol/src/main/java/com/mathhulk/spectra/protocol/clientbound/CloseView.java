package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

/** Closes and destroys a view. */
public record CloseView(String viewId) implements ClientboundPacket {
  public static CloseView read(FrameReader reader) {
    return new CloseView(reader.readString(Limits.MAX_ID));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeString(viewId, Limits.MAX_ID);
  }
}
