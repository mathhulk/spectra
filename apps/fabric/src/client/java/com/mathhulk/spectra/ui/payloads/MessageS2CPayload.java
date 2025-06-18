package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MessageS2CPayload(String message) implements CustomPacketPayload {
    public static final ResourceLocation MESSAGE_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "message");
    public static final CustomPacketPayload.Type<MessageS2CPayload> TYPE = new CustomPacketPayload.Type<>(MESSAGE_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageS2CPayload> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, MessageS2CPayload::message, MessageS2CPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}