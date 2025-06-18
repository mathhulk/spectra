package com.mathhulk.spectra.ui.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record OpenScreenS2CPayload(String resource, boolean replace) implements CustomPacketPayload {
    public static final ResourceLocation OPEN_SCREEN_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "enable_layer");
    public static final Type<OpenScreenS2CPayload> TYPE = new Type<>(OPEN_SCREEN_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreenS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenScreenS2CPayload::resource,
            ByteBufCodecs.BOOL, OpenScreenS2CPayload::replace,
            OpenScreenS2CPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
