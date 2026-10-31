package com.mathhulk.spectra.protocol;

/** A protocol packet. Implementations are immutable records. */
public sealed interface Packet permits ClientboundPacket, ServerboundPacket {
  void write(FrameWriter writer);
}
