package com.mathhulk.spectra.protocol.serverbound;

import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ResourceHash;
import com.mathhulk.spectra.protocol.ServerboundPacket;

import java.util.List;

/** Hashes from manifest {@code generation} that the client does not have yet. */
public record ResourceRequest(long generation, List<ResourceHash> hashes) implements ServerboundPacket {
  public ResourceRequest {
    hashes = List.copyOf(hashes);
  }

  public static ResourceRequest read(FrameReader reader) {
    return new ResourceRequest(reader.readVarLong(), reader.readList(Limits.MAX_REQUEST_HASHES, ResourceHash::read));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarLong(generation).writeList(hashes, Limits.MAX_REQUEST_HASHES, (w, h) -> h.write(w));
  }
}
