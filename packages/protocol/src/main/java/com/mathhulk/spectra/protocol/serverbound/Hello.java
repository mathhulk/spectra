package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ServerboundPacket;

import java.util.Set;

/** First packet a client sends: its protocol version, mod version, and capabilities. */
public record Hello(int version, String modVersion, Set<String> capabilities) implements ServerboundPacket {
  public Hello {
    capabilities = Set.copyOf(capabilities);
  }

  public static Hello read(FrameReader reader) {
    return new Hello(reader.readVarInt(), reader.readString(Limits.MAX_ID),
        Set.copyOf(reader.readList(Limits.MAX_CAPABILITIES, r -> r.readString(Limits.MAX_CAPABILITY))));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(version).writeString(modVersion, Limits.MAX_ID)
        .writeList(capabilities.stream().sorted().toList(), Limits.MAX_CAPABILITIES,
            (w, c) -> w.writeString(c, Limits.MAX_CAPABILITY));
  }
}
