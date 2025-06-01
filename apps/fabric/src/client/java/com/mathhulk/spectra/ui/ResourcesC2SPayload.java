package com.mathhulk.spectra.ui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public record ResourcesC2SPayload(HashMap<Integer, String> resources) implements CustomPacketPayload {
    public static final ResourceLocation RESOURCES_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "resources");
    public static final Type<ResourcesC2SPayload> TYPE = new Type<>(RESOURCES_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourcesC2SPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, ByteBufCodecs.STRING_UTF8), ResourcesC2SPayload::resources,
            ResourcesC2SPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}