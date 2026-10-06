package com.example.examplemod.network;

import com.example.examplemod.util.DeepBackpackFetcher;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record RequestFetchRecipeIngredientsPayload(List<SlotReq> items) implements CustomPacketPayload {

    public record SlotReq(int slot, ItemStack stack) {
        public static final StreamCodec<RegistryFriendlyByteBuf, SlotReq> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SlotReq::slot,
                ItemStack.STREAM_CODEC, SlotReq::stack, // 此时由于已过滤掉空物品，可安全使用严格模式
                SlotReq::new
        );
    }

    public static final Type<RequestFetchRecipeIngredientsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "fetch_recipe_ingredients"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestFetchRecipeIngredientsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, SlotReq.STREAM_CODEC),
            RequestFetchRecipeIngredientsPayload::items,
            RequestFetchRecipeIngredientsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // 将 List<SlotReq> 转给服务端直接填放指定 slot
                DeepBackpackFetcher.fillCraftingGridBySlots(serverPlayer, this.items);
            }
        });
    }
}