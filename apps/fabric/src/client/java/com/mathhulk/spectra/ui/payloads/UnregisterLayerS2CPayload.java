package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record UnregisterLayerS2CPayload(String layer, String resource) implements CustomPacketPayload {
    public static final ResourceLocation UNREGISTER_LAYER_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "unregister_layer");
    public static final Type<UnregisterLayerS2CPayload> TYPE = new Type<>(UNREGISTER_LAYER_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, UnregisterLayerS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UnregisterLayerS2CPayload::layer,
            ByteBufCodecs.STRING_UTF8, UnregisterLayerS2CPayload::resource,
            UnregisterLayerS2CPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
