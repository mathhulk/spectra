package com.mathhulk.spectra.fabric.net;

import com.mathhulk.spectra.protocol.Protocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * One raw Spectra protocol frame on the {@code spectra:main} channel. The body is the frame bytes with no extra
 * length prefix, which is exactly what Bukkit-style plugin messages carry.
 */
public record SpectraPayload(byte[] frame) implements CustomPacketPayload {
  public static final Type<SpectraPayload> TYPE = new Type<>(Identifier.parse(Protocol.CHANNEL));

  public static final StreamCodec<FriendlyByteBuf, SpectraPayload> CODEC = StreamCodec.of(
      (buffer, payload) -> buffer.writeBytes(payload.frame()),
      buffer -> {
        byte[] frame = new byte[buffer.readableBytes()];
        buffer.readBytes(frame);
        return new SpectraPayload(frame);
      });

  @Override
  public @NonNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
