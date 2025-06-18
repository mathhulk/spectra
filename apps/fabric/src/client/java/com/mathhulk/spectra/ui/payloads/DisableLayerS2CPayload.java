package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record DisableLayerS2CPayload(String layer, String resource) implements CustomPacketPayload {
    public static final ResourceLocation DISABLE_LAYER_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "disable_layer");
    public static final Type<DisableLayerS2CPayload> TYPE = new Type<>(DISABLE_LAYER_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, DisableLayerS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DisableLayerS2CPayload::layer,
            ByteBufCodecs.STRING_UTF8, DisableLayerS2CPayload::resource,
            DisableLayerS2CPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}