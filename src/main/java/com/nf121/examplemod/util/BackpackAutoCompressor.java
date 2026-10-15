package com.nf121.examplemod.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "examplemod")
public final class BackpackAutoCompressor {

    // 关界面或捡起物品后的防抖延迟（10 ticks = 0.5 秒）
    private static final int EVENT_DELAY_TICKS = 10;
    // 兜底心跳检查间隔（40 ticks = 2 秒）
    private static final int HEARTBEAT_INTERVAL = 40;

    private static final Map<UUID, Integer> DIRTY_COOLDOWNS = new ConcurrentHashMap<>();

    private BackpackAutoCompressor() {
    }

    public static void markDirty(ServerPlayer player) {
        DIRTY_COOLDOWNS.put(player.getUUID(), EVENT_DELAY_TICKS);
    }

    /**
     * 1. 玩家关闭任何背包、箱子等容器时触发（最符合直觉的触发点）
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            markDirty(player);
        }
    }

    /**
     * 2. 玩家在地上捡起掉落物时触发
     */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            markDirty(player);
        }
    }

    /**
     * 3. Tick 处理：兼顾即时响应与低频保底轮询
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 当前玩家还开着箱子或背包界面，绝不执行压缩（防刷物、防打扰）
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }

        UUID uuid = player.getUUID();
        Integer cooldown = DIRTY_COOLDOWNS.get(uuid);

        boolean shouldRun = false;

        if (cooldown != null) {
            if (cooldown > 0) {
                DIRTY_COOLDOWNS.put(uuid, cooldown - 1);
            } else {
                DIRTY_COOLDOWNS.remove(uuid);
                shouldRun = true; // 事件防抖计时结束，执行
            }
        } else {
            // 40 ticks 保底轮询兜底（错开不同玩家的 tick）
            if (Math.floorMod(player.tickCount + player.getId(), HEARTBEAT_INTERVAL) == 0) {
                shouldRun = true;
            }
        }

        if (shouldRun) {
            // hasEquippedEnderBackpack 内部耗时几乎为 0，未佩戴末影背包直接快速返回
            if (EnderBackpackStorage.hasEquippedEnderBackpack(player)) {
                if (EnderBackpackStorage.compact(player)) {
                    player.inventoryMenu.broadcastChanges();
                }
            }
        }
    }
}