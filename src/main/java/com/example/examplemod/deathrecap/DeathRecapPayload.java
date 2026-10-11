package com.example.examplemod.deathrecap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record DeathRecapPayload(List<DamageEventSnapshot> records) implements CustomPacketPayload {
    public static final Type<DeathRecapPayload> TYPE = 
            new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "death_recap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathRecapPayload> STREAM_CODEC = 
            DamageEventSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(DeathRecapPayload::new, DeathRecapPayload::records);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}