package com.mathhulk.spectra.protocol.clientbound;

import com.mathhulk.spectra.protocol.ClientboundPacket;
import com.mathhulk.spectra.protocol.FrameReader;
import com.mathhulk.spectra.protocol.FrameWriter;
import com.mathhulk.spectra.protocol.Limits;
import com.mathhulk.spectra.protocol.ResourcePath;

import java.util.Optional;

/** Opens (or replaces) the view {@code viewId}. {@code after} names a vanilla HUD element for layers. */
public record OpenView(String viewId, ViewKind kind, String resource, Layout layout, int zIndex, boolean visible,
    Optional<String> after) implements ClientboundPacket {
  public OpenView {
    ResourcePath.requireValid(resource);
  }

  public static OpenView read(FrameReader reader) {
    String viewId = reader.readString(Limits.MAX_ID);
    ViewKind kind = reader.readEnum(ViewKind.class);
    String resource = reader.readString(ResourcePath.MAX_LENGTH);
    Layout layout = Layout.read(reader);
    int zIndex = reader.readVarInt();
    boolean visible = reader.readBool();
    Optional<String> after = reader.readBool() ? Optional.of(reader.readString(Limits.MAX_ID)) : Optional.empty();
    return new OpenView(viewId, kind, resource, layout, zIndex, visible, after);
  }

  @Override
  public void write(FrameWriter writer) {
    writer.writeString(viewId, Limits.MAX_ID).writeEnum(kind).writeString(resource, ResourcePath.MAX_LENGTH);
    layout.write(writer);
    writer.writeVarInt(zIndex).writeBool(visible).writeBool(after.isPresent());
    after.ifPresent(value -> writer.writeString(value, Limits.MAX_ID));
  }
}
