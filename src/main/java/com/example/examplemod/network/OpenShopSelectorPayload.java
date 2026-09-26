package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenShopSelectorPayload() implements CustomPacketPayload {
    public static final OpenShopSelectorPayload INSTANCE =
            new OpenShopSelectorPayload();

    public static final Type<OpenShopSelectorPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            ExampleMod.MODID,
                            "open_shop_selector"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenShopSelectorPayload
            > STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}