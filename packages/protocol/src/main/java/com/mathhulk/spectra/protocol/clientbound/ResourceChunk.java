package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ProtocolException;
import com.mathhulk.spectra.protocol.ResourceHash;

/** Chunk {@code index} of {@code count} for the resource with content {@code hash}. */
public record ResourceChunk(ResourceHash hash, int index, int count, byte[] data) implements ClientboundPacket {
  public ResourceChunk {
    if (count < 1 || count > Limits.MAX_CHUNKS || index < 0 || index >= count) {
      throw new ProtocolException("Invalid chunk " + index + "/" + count);
    }
    if (data.length > Limits.MAX_CHUNK_SIZE) {
      throw new ProtocolException("Chunk too large: " + data.length);
    }
  }

  public static ResourceChunk read(FrameReader reader) {
    return new ResourceChunk(ResourceHash.read(reader), reader.readVarInt(), reader.readVarInt(),
        reader.readBytes(Limits.MAX_CHUNK_SIZE));
  }

  @Override
  public void write(FrameWriter writer) {
    hash.write(writer);
    writer.writeVarInt(index).writeVarInt(count).writeBytes(data);
  }
}
