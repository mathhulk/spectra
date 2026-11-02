package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;

import java.util.Set;

/** Server's answer to {@code Hello}. When {@code accepted} is false nothing else will be sent. */
public record Welcome(int version, boolean accepted, String reason, String serverId, Set<String> capabilities)
    implements ClientboundPacket {
  public Welcome {
    capabilities = Set.copyOf(capabilities);
  }

  public static Welcome read(FrameReader reader) {
    return new Welcome(
        reader.readVarInt(),
        reader.readBool(),
        reader.readString(Limits.MAX_SHORT_TEXT),
        reader.readString(Limits.MAX_ID),
        Set.copyOf(reader.readList(Limits.MAX_CAPABILITIES, r -> r.readString(Limits.MAX_CAPABILITY))));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarInt(version)
        .writeBool(accepted)
        .writeString(reason, Limits.MAX_SHORT_TEXT)
        .writeString(serverId, Limits.MAX_ID)
        .writeList(capabilities.stream().sorted().toList(), Limits.MAX_CAPABILITIES,
            (w, c) -> w.writeString(c, Limits.MAX_CAPABILITY));
  }
}
