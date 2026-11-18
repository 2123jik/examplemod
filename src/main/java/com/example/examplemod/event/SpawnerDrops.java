package com.example.examplemod.event;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import static com.example.examplemod.util.Common.*;

@EventBusSubscriber
public final class SpawnerDrops {

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (
                !(event.getBreaker() instanceof Player player) ||
                        !event.getState().is(Blocks.SPAWNER)) {
            return;
        }

        WorldTier tier = WorldTier.getTier(player);

        if (!roll(event.getLevel().random, tier)) {
            return;
        }
        addDrop(event, createGatewayPearl(tier));

        addDrop(event, createRandomLowestGem(event.getLevel().random)
        );
        addDrop(event, new ItemStack(Items.IRON_INGOT, 5)
        );
    }
}