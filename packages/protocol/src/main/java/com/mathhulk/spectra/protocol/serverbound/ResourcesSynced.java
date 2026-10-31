package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.ServerboundPacket;

/** The client holds every resource of manifest {@code generation} and can render it. */
public record ResourcesSynced(long generation) implements ServerboundPacket {
  public static ResourcesSynced read(FrameReader reader) {
    return new ResourcesSynced(reader.readVarLong());
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarLong(generation);
  }
}
