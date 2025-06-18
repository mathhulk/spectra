package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record RegisterLayerS2CPayload(String layer, String resource, int x, int y, int width, int height, boolean enabled) implements CustomPacketPayload {
    public static final ResourceLocation REGISTER_LAYER_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "register_layer");
    public static final Type<RegisterLayerS2CPayload> TYPE = new Type<>(REGISTER_LAYER_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, RegisterLayerS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RegisterLayerS2CPayload::layer,
            ByteBufCodecs.STRING_UTF8, RegisterLayerS2CPayload::resource,
            ByteBufCodecs.INT, RegisterLayerS2CPayload::x,
            ByteBufCodecs.INT, RegisterLayerS2CPayload::y,
            ByteBufCodecs.INT, RegisterLayerS2CPayload::width,
            ByteBufCodecs.INT, RegisterLayerS2CPayload::height,
            ByteBufCodecs.BOOL, RegisterLayerS2CPayload::enabled,
            RegisterLayerS2CPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
