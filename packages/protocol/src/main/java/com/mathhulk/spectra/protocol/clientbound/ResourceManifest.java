package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ProtocolException;
import com.mathhulk.spectra.protocol.ResourceHash;
import com.mathhulk.spectra.protocol.ResourcePath;

import java.util.List;

/**
 * One part of the server's resource manifest for a given {@code generation}. A client acts on a generation only
 * once all {@code parts} have arrived.
 */
public record ResourceManifest(long generation, int part, int parts, List<Entry> entries)
    implements ClientboundPacket {
  public record Entry(String path, ResourceHash hash, long size) {
    public Entry {
      ResourcePath.requireValid(path);
      if (size < 0) {
        throw new ProtocolException("Negative resource size");
      }
    }

    static Entry read(FrameReader reader) {
      return new Entry(reader.readString(ResourcePath.MAX_LENGTH), ResourceHash.read(reader), reader.readVarLong());
    }

    void write(FrameWriter writer) {
      writer.writeString(path, ResourcePath.MAX_LENGTH);
      hash.write(writer);
      writer.writeVarLong(size);
    }

    /** Upper bound of this entry's encoded size, used by senders to split manifests. */
    public int encodedSizeBound() {
      return 5 + path.length() * 3 + ResourceHash.LENGTH + 10;
    }
  }

  public ResourceManifest {
    if (parts < 1 || part < 0 || part >= parts) {
      throw new ProtocolException("Invalid manifest part " + part + "/" + parts);
    }
    entries = List.copyOf(entries);
  }

  public static ResourceManifest read(FrameReader reader) {
    return new ResourceManifest(reader.readVarLong(), reader.readVarInt(), reader.readVarInt(),
        reader.readList(Limits.MAX_MANIFEST_ENTRIES, Entry::read));
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeVarLong(generation).writeVarInt(part).writeVarInt(parts)
        .writeList(entries, Limits.MAX_MANIFEST_ENTRIES, (w, e) -> e.write(w));
  }
}
