package com.example.examplemod.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "examplemod")
public final class BackpackAutoCompressor {

    private static final int INTERVAL_TICKS = 20;

    private BackpackAutoCompressor() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 错开不同玩家的检查时间。
        if (Math.floorMod(
                player.tickCount + player.getId(),
                INTERVAL_TICKS
        ) != 0) {
            return;
        }

        /*
         * 当前玩家打开工作台或其他容器时暂停自动整理。
         * 主动 JEI 取料仍由 DeepBackpackFetcher 处理。
         */
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }

        if (EnderBackpackStorage.compact(player)) {
            player.inventoryMenu.broadcastChanges();
        }
    }
}
