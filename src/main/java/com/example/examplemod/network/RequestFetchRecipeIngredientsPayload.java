package com.example.examplemod.network;

import com.example.examplemod.util.DeepBackpackFetcher;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record RequestFetchRecipeIngredientsPayload(
        List<SlotReq> items,
        boolean maxTransfer
) implements CustomPacketPayload {

    public record SlotReq(int slot, ItemStack stack) {

        public static final StreamCodec<RegistryFriendlyByteBuf, SlotReq>
                STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                SlotReq::slot,
                ItemStack.STREAM_CODEC,
                SlotReq::stack,
                SlotReq::new
        );
    }

    public static final Type<RequestFetchRecipeIngredientsPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "examplemod",
                            "fetch_recipe_ingredients"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RequestFetchRecipeIngredientsPayload
            > STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(
                    ArrayList::new,
                    SlotReq.STREAM_CODEC,
                    9
            ),
            RequestFetchRecipeIngredientsPayload::items,
            ByteBufCodecs.BOOL,
            RequestFetchRecipeIngredientsPayload::maxTransfer,
            RequestFetchRecipeIngredientsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!(player.containerMenu instanceof CraftingMenu menu)) {
                return;
            }

            if (!menu.stillValid(player)) {
                return;
            }

            if (items.isEmpty() || items.size() > 9) {
                return;
            }

            boolean[] visited = new boolean[10];

            for (SlotReq req : items) {
                int slot = req.slot();
                ItemStack stack = req.stack();

                if (slot < 1 || slot > 9 || visited[slot]) {
                    return;
                }

                visited[slot] = true;

                // 客户端只描述单份配方，批量数量由服务端计算。
                if (stack.isEmpty() || stack.getCount() != 1) {
                    return;
                }

                if (!menu.getSlot(slot).mayPlace(stack)
                        || menu.getSlot(slot).getMaxStackSize(stack) < 1) {
                    return;
                }
            }

            DeepBackpackFetcher.fillCraftingGrid(
                    player,
                    items,
                    1
            );
        });
    }
}
