package com.example.examplemod.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = "examplemod")
public final class MerchantAutoFill {

    private static final Map<MerchantMenu, Integer> SELECTED = new WeakHashMap<>();
    private static final Set<MerchantMenu> FILLING = Collections.newSetFromMap(new IdentityHashMap<>());

    // 记录每个菜单最后一次尝试失败时的“状态指纹”，防止材料不足时每个 Tick 都在疯狂重试！
    private static final Map<MerchantMenu, FailedSignature> FAILED_CACHE = new WeakHashMap<>();

    private static final int INTERVAL_TICKS = 10;

    private record FailedSignature(int tradeIndex, ItemStack slot0, ItemStack slot1) {
        boolean matches(int currentTrade, ItemStack current0, ItemStack current1) {
            return tradeIndex == currentTrade
                    && EnderBackpackStorage.equal(slot0, current0)
                    && EnderBackpackStorage.equal(slot1, current1);
        }
    }

    private MerchantAutoFill() {
    }

    public static void select(MerchantMenu menu, int selectedIndex) {
        SELECTED.put(menu, selectedIndex);
        FAILED_CACHE.remove(menu); // 玩家主动切换交易项目，允许再次尝试
    }

    public static void forget(MerchantMenu menu) {
        SELECTED.remove(menu);
        FAILED_CACHE.remove(menu);
    }

    public static void fill(ServerPlayer player, MerchantMenu menu) {
        if (player.containerMenu != menu || !menu.stillValid(player) || !FILLING.add(menu)) {
            return;
        }

        try {
            int selectedIndex = SELECTED.getOrDefault(menu, 0);
            if (selectedIndex < 0 || selectedIndex >= menu.getOffers().size()) {
                return;
            }

            MerchantOffer offer = menu.getOffers().get(selectedIndex);
            if (offer.isOutOfStock()) {
                return;
            }

            ItemStack slot0 = menu.getSlot(0).getItem();
            ItemStack slot1 = menu.getSlot(1).getItem();

            // 如果当前槽位已经满足该交易，直接返回
            if (offer.satisfiedBy(slot0, slot1)) {
                return;
            }

            // 状态防抖：如果当前输入槽和交易未改变且上次已判定材料不足，立刻跳过！
            FailedSignature failed = FAILED_CACHE.get(menu);
            if (failed != null && failed.matches(selectedIndex, slot0, slot1)) {
                return;
            }

            boolean success = EnderBackpackStorage.fillMerchant(player, menu, selectedIndex);
            if (!success) {
                // 标记失败，在槽位或交易切换前不再重复计算
                FAILED_CACHE.put(menu, new FailedSignature(selectedIndex, slot0.copy(), slot1.copy()));
            } else {
                FAILED_CACHE.remove(menu);
                BackpackAutoCompressor.markDirty(player);
            }
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

        if (Math.floorMod(player.tickCount + player.getId(), INTERVAL_TICKS) != 0) {
            return;
        }

        fill(player, menu);
    }
}