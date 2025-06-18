package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record EnableLayerS2CPayload(String layer, String resource) implements CustomPacketPayload {
    public static final ResourceLocation ENABLE_LAYER_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "enable_layer");
    public static final Type<EnableLayerS2CPayload> TYPE = new Type<>(ENABLE_LAYER_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, EnableLayerS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EnableLayerS2CPayload::layer,
            ByteBufCodecs.STRING_UTF8, EnableLayerS2CPayload::resource,
            EnableLayerS2CPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
