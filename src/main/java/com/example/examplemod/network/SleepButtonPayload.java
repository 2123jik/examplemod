package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SleepButtonPayload() implements CustomPacketPayload {

    public static final SleepButtonPayload INSTANCE =
            new SleepButtonPayload();

    public static final Type<SleepButtonPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            ExampleMod.MODID,
                            "sleep_button"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SleepButtonPayload
            > STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}