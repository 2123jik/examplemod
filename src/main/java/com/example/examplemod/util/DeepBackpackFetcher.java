package com.example.examplemod.util;

import com.example.examplemod.network.RequestFetchRecipeIngredientsPayload.SlotReq;
import dev.xkmc.l2backpack.content.bag.AbstractBag;
import dev.xkmc.l2backpack.content.common.BaseBagItem;
import dev.xkmc.l2backpack.content.remote.dimensional.DimensionalItem;
import dev.xkmc.l2backpack.content.remote.player.EnderBackpackItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DeepBackpackFetcher {

    private DeepBackpackFetcher() {
    }

    public static void fillCraftingGrid(
            ServerPlayer player,
            List<SlotReq> requests,
            boolean maxTransfer
    ) {
        if (!(player.containerMenu instanceof CraftingMenu menu)
                || !menu.stillValid(player)
                || !validRequests(menu, requests)) {
            return;
        }

        if (!maxTransfer) {
            fillCraftingGridBySlots(player, requests);
            return;
        }

        int maxSets = Integer.MAX_VALUE;

        for (SlotReq req : requests) {
            ItemStack ingredient = req.stack();
            Slot slot = menu.getSlot(req.slot());

            int limit = Math.min(
                    ingredient.getMaxStackSize(),
                    slot.getMaxStackSize(ingredient)
            );

            maxSets = Math.min(
                    maxSets,
                    limit / ingredient.getCount()
            );
        }

        for (int sets = 1; sets <= maxSets; sets++) {
            List<SlotReq> round = new ArrayList<>(requests.size());

            for (SlotReq req : requests) {
                round.add(new SlotReq(
                        req.slot(),
                        req.stack().copyWithCount(
                                req.stack().getCount() * sets
                        )
                ));
            }

            fillCraftingGridBySlots(player, round);

            boolean complete = true;

            for (SlotReq req : round) {
                ItemStack actual =
                        menu.getSlot(req.slot()).getItem();

                if (!same(actual, req.stack())
                        || actual.getCount() < req.stack().getCount()) {
                    complete = false;
                    break;
                }
            }

            if (!complete) {
                break;
            }
        }

        player.getInventory().setChanged();
        menu.broadcastChanges();
    }

    public static void fillCraftingGridBySlots(
            ServerPlayer player,
            List<SlotReq> requests
    ) {
        if (!(player.containerMenu instanceof CraftingMenu menu)
                || !menu.stillValid(player)
                || !validRequests(menu, requests)) {
            return;
        }

        boolean changed = clearIncompatibleSlots(
                menu,
                requests,
                player
        );

        for (SlotReq req : requests) {
            Slot slot = menu.getSlot(req.slot());
            ItemStack required = req.stack();

            int limit = Math.min(
                    required.getMaxStackSize(),
                    slot.getMaxStackSize(required)
            );

            int targetCount = Math.min(
                    required.getCount(),
                    limit
            );

            ItemStack current = slot.getItem();

            if (!current.isEmpty() && !same(current, required)) {
                // clearIncompatibleSlots 已处理，保守跳过异常情况。
                continue;
            }

            int currentCount = current.isEmpty()
                    ? 0
                    : current.getCount();

            int needed = targetCount - currentCount;

            if (needed <= 0) {
                continue;
            }

            int extracted = extractFromSources(
                    player,
                    required,
                    needed
            );

            if (extracted <= 0) {
                continue;
            }

            ItemStack updated = current.isEmpty()
                    ? required.copyWithCount(extracted)
                    : current.copyWithCount(currentCount + extracted);

            slot.set(updated);
            changed = true;
        }

        if (changed) {
            player.getInventory().setChanged();

            // 使用原版 CraftingMenu 的结果更新流程。
            menu.slotsChanged(menu.getSlot(1).container);
            menu.broadcastChanges();
        }
    }

    private static boolean validRequests(
            CraftingMenu menu,
            List<SlotReq> requests
    ) {
        if (requests == null
                || requests.isEmpty()
                || requests.size() > 9) {
            return false;
        }

        boolean[] visited = new boolean[10];

        for (SlotReq req : requests) {
            if (req == null
                    || req.slot() < 1
                    || req.slot() > 9
                    || visited[req.slot()]
                    || req.stack().isEmpty()
                    || req.stack().getCount() <= 0) {
                return false;
            }

            visited[req.slot()] = true;

            Slot slot = menu.getSlot(req.slot());

            if (!slot.mayPlace(req.stack())
                    || slot.getMaxStackSize(req.stack()) <= 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean clearIncompatibleSlots(
            CraftingMenu menu,
            List<SlotReq> requests,
            ServerPlayer player
    ) {
        boolean changed = false;

        for (int index = 1; index <= 9; index++) {
            Slot slot = menu.getSlot(index);
            ItemStack current = slot.getItem();

            if (current.isEmpty()) {
                continue;
            }

            SlotReq wanted = null;

            for (SlotReq req : requests) {
                if (req.slot() == index) {
                    wanted = req;
                    break;
                }
            }

            if (wanted != null && same(current, wanted.stack())) {
                continue;
            }

            /*
             * 保留你原来 JEI 转移的行为：
             * 无关合成材料退回玩家库存，放不下则掉落。
             *
             * 这是合成格清理，不是压缩/解压余料处理。
             */
            ItemStack returned = current.copy();
            slot.set(ItemStack.EMPTY);

            if (!player.getInventory().add(returned)
                    && !returned.isEmpty()) {
                player.drop(returned, false);
            }

            changed = true;
        }

        return changed;
    }

    private static int extractFromSources(
            ServerPlayer player,
            ItemStack target,
            int requested
    ) {
        int remaining = requested;

        // 玩家主物品栏仅取现成材料，不自动解压，不进入背包物品。
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack current =
                    player.getInventory().getItem(slot);

            if (isStorageItem(current) || !same(current, target)) {
                continue;
            }

            int take = Math.min(
                    current.getCount(),
                    remaining
            );

            ItemStack updated = current.copy();
            updated.shrink(take);

            player.getInventory().setItem(slot, updated);
            remaining -= take;
        }

        if (remaining > 0) {
            remaining -= EnderBackpackStorage.extract(
                    player,
                    target,
                    remaining
            );
        }

        return requested - remaining;
    }

    private static boolean isStorageItem(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof BaseBagItem
                || stack.getItem() instanceof AbstractBag
                || stack.getItem() instanceof EnderBackpackItem
                || stack.getItem() instanceof DimensionalItem);
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return !a.isEmpty()
                && !b.isEmpty()
                && ItemStack.isSameItemSameComponents(a, b);
    }
}
