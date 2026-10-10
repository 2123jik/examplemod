package com.example.examplemod.network;

import com.example.examplemod.util.DeepBackpackFetcher;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestEmiCraftingPayload(
        ResourceLocation recipeId,
        int requestedSets
) implements CustomPacketPayload {

    public static final Type<RequestEmiCraftingPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "examplemod",
                            "emi_craft"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RequestEmiCraftingPayload
            > STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            RequestEmiCraftingPayload::recipeId,

            ByteBufCodecs.VAR_INT,
            RequestEmiCraftingPayload::requestedSets,

            RequestEmiCraftingPayload::new
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

            int sets = Math.max(
                    1,
                    Math.min(64, requestedSets)
            );

            DeepBackpackFetcher.fillRecipe(
                    player,
                    recipeId,
                    sets
            );
        });
    }
}
