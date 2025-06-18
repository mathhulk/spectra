package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HandshakeC2SPayload(int id) implements CustomPacketPayload {
  public static final ResourceLocation HANDSHAKE_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "handshake");
  public static final CustomPacketPayload.Type<HandshakeC2SPayload> TYPE = new CustomPacketPayload.Type<>(HANDSHAKE_PAYLOAD_TYPE);
  public static final StreamCodec<RegistryFriendlyByteBuf, HandshakeC2SPayload> CODEC = StreamCodec.composite(ByteBufCodecs.INT, HandshakeC2SPayload::id, HandshakeC2SPayload::new);

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}