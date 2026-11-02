package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.ProtocolException;

/**
 * Position and size of a view in GUI-scaled pixels. A width or height of 0 fills that axis. Offsets move the view
 * away from its anchor (towards the screen's center).
 */
public record Layout(Anchor anchor, int x, int y, int width, int height) {
  public static final Layout FULLSCREEN = new Layout(Anchor.TOP_LEFT, 0, 0, 0, 0);

  public Layout {
    if (width < 0 || height < 0) {
      throw new ProtocolException("Negative view size");
    }
  }

  public static Layout read(FrameReader reader) {
    return new Layout(reader.readEnum(Anchor.class), reader.readVarInt(), reader.readVarInt(), reader.readVarInt(),
        reader.readVarInt());
  }

  public void write(FrameWriter writer) {
    writer.writeEnum(anchor).writeVarInt(x).writeVarInt(y).writeVarInt(width).writeVarInt(height);
  }
}
