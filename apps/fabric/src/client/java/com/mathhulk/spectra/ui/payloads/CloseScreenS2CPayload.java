package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record CloseScreenS2CPayload(int id) implements CustomPacketPayload {
    public static final ResourceLocation CLOSE_SCREEN_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "enable_layer");
    public static final Type<CloseScreenS2CPayload> TYPE = new Type<>(CLOSE_SCREEN_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseScreenS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CloseScreenS2CPayload::id,
            CloseScreenS2CPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
