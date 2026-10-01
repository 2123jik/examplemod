package com.example.examplemod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ArrowCritPayload(
    float multiplier, 
    float totalDamage, 
    float bonusDamage,
    double hitX, 
    double hitY, 
    double hitZ
) implements CustomPacketPayload {

    public static final Type<ArrowCritPayload> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath("your_mod_id", "arrow_crit"));

    public static final StreamCodec<ByteBuf, ArrowCritPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, ArrowCritPayload::multiplier,
        ByteBufCodecs.FLOAT, ArrowCritPayload::totalDamage,
        ByteBufCodecs.FLOAT, ArrowCritPayload::bonusDamage,
        ByteBufCodecs.DOUBLE, ArrowCritPayload::hitX,
        ByteBufCodecs.DOUBLE, ArrowCritPayload::hitY,
        ByteBufCodecs.DOUBLE, ArrowCritPayload::hitZ,
        ArrowCritPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}