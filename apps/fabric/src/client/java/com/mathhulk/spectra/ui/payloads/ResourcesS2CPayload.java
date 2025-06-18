package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public record ResourcesS2CPayload(HashMap<String, String> resources) implements CustomPacketPayload {
    public static final ResourceLocation RESOURCES_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "resources");
    public static final Type<ResourcesS2CPayload> TYPE = new Type<>(RESOURCES_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourcesS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
            ResourcesS2CPayload::resources,
            ResourcesS2CPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}