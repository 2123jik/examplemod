package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

public record SwapEquipmentPayload(EquipmentSlot slot) implements CustomPacketPayload {

    public static final Type<SwapEquipmentPayload> TYPE = 
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "swap_equipment"));

    // 将 EquipmentSlot 枚举编解码为整数传输
    public static final StreamCodec<RegistryFriendlyByteBuf, SwapEquipmentPayload> STREAM_CODEC = 
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.map(i -> EquipmentSlot.values()[i], Enum::ordinal),
                    SwapEquipmentPayload::slot,
                    SwapEquipmentPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}