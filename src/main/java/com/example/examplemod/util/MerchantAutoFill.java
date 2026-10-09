package com.example.examplemod.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "examplemod")
public final class MerchantAutoFill {

    /*
     * key 为菜单对象，value 只保存交易索引。
     * 菜单关闭并失去引用后可正常回收。
     *
     * 以下状态只在服务端主线程访问。
     */
    private static final Map<MerchantMenu, Integer> SELECTED =
            new WeakHashMap<>();

    private static final Set<MerchantMenu> FILLING =
            Collections.newSetFromMap(
                    new IdentityHashMap<MerchantMenu, Boolean>()
            );

    // 普通交易后最多约 0.25 秒补下一笔。
    private static final int INTERVAL_TICKS = 5;

    private MerchantAutoFill() {
    }

    public static void select(
            MerchantMenu menu,
            int selectedIndex
    ) {
        SELECTED.put(menu, selectedIndex);
    }

    public static void forget(MerchantMenu menu) {
        SELECTED.remove(menu);
    }

    public static void fill(
            ServerPlayer player,
            MerchantMenu menu
    ) {
        if (player.containerMenu != menu
                || !menu.stillValid(player)
                || !FILLING.add(menu)) {
            return;
        }

        try {
            /*
             * 原版初始选择为第一笔交易。
             * 收到选择更新后使用实际索引。
             */
            int selectedIndex = SELECTED.getOrDefault(menu, 0);

            EnderBackpackStorage.fillMerchant(
                    player,
                    menu,
                    selectedIndex
            );
        } finally {
            FILLING.remove(menu);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.containerMenu instanceof MerchantMenu menu)) {
            return;
        }

        if (Math.floorMod(
                player.tickCount + player.getId(),
                INTERVAL_TICKS
        ) != 0) {
            return;
        }

        fill(player, menu);
    }
}
