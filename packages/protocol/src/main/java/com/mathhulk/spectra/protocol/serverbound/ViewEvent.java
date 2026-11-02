package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;

/** Lifecycle event for a view, so the server's view state stays accurate. */
public record ViewEvent(String viewId, Type type, String detail) implements ServerboundPacket {
  /** Ordinals are part of the wire format: only append. */
  public enum Type {
    OPENED, CLOSED, FAILED
  }

  public static ViewEvent read(FrameReader reader) {
    return new ViewEvent(reader.readString(Limits.MAX_ID), reader.readEnum(Type.class),
        reader.readString(Limits.MAX_SHORT_TEXT));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeString(viewId, Limits.MAX_ID).writeEnum(type).writeString(detail, Limits.MAX_SHORT_TEXT);
  }
}
