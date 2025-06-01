package com.mathhulk.spectra.ui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ResourceS2CPayload(int id, int length, int index, byte[] chunk) implements CustomPacketPayload {
    public static final ResourceLocation RESOURCE_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "resource");
    public static final Type<ResourceS2CPayload> TYPE = new Type<>(RESOURCE_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ResourceS2CPayload::id,
            ByteBufCodecs.INT, ResourceS2CPayload::length,
            ByteBufCodecs.INT, ResourceS2CPayload::index,
            ByteBufCodecs.BYTE_ARRAY, ResourceS2CPayload::chunk,
            ResourceS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}