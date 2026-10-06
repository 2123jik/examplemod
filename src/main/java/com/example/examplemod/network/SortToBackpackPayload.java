package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.util.BackpackSortHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SortToBackpackPayload(boolean fromContainerOnly) implements CustomPacketPayload {
    public static final Type<SortToBackpackPayload> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "sort_to_backpack"));

    public static final StreamCodec<ByteBuf, SortToBackpackPayload> STREAM_CODEC = 
        ByteBufCodecs.BOOL.map(SortToBackpackPayload::new, SortToBackpackPayload::fromContainerOnly);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SortToBackpackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                BackpackSortHelper.sortInventoryToBackpack(serverPlayer, payload.fromContainerOnly());
            }
        });
    }
}