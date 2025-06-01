package com.mathhulk.spectra.ui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenBrowserS2CPayload(int id) implements CustomPacketPayload {
    public static final ResourceLocation OPEN_BROWSER_PAYLOAD_TYPE = ResourceLocation.fromNamespaceAndPath("spectra", "open-browser");
    public static final CustomPacketPayload.Type<OpenBrowserS2CPayload> TYPE = new CustomPacketPayload.Type<>(OPEN_BROWSER_PAYLOAD_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBrowserS2CPayload> CODEC = StreamCodec.composite(ByteBufCodecs.INT, OpenBrowserS2CPayload::id, OpenBrowserS2CPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}