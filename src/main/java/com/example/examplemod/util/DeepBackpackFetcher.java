package com.example.examplemod.util;

import com.example.examplemod.network.RequestFetchRecipeIngredientsPayload.SlotReq;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DeepBackpackFetcher {

    private DeepBackpackFetcher() {
    }

    private record ItemKey(ItemStack stack) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemKey other)) return false;
            return EnderBackpackStorage.same(this.stack, other.stack);
        }

        @Override
        public int hashCode() {
            return stack.getItem().hashCode();
        }
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

        // 1. 获取槽位与物品本身支持的最大堆叠上限
        int maxSets = Integer.MAX_VALUE;
        for (SlotReq req : requests) {
            ItemStack ingredient = req.stack();
            Slot slot = menu.getSlot(req.slot());
            int limit = Math.min(ingredient.getMaxStackSize(), slot.getMaxStackSize(ingredient));
            maxSets = Math.min(maxSets, limit / ingredient.getCount());
        }

        if (maxSets <= 0) {
            return;
        }

        // 2. 核心修复：聚合统计同种材料在一次合成中的总消耗量（例如两个格子都要潜影壳，每套共消耗 2 个）
        Map<ItemKey, Integer> perCraftCount = new HashMap<>();
        for (SlotReq req : requests) {
            perCraftCount.merge(new ItemKey(req.stack()), req.stack().getCount(), Integer::sum);
        }

        // 3. 计算所有材料的木桶短板，得出真正能完全满足的合成套数
        int possibleSets = maxSets;
        for (Map.Entry<ItemKey, Integer> entry : perCraftCount.entrySet()) {
            int neededPerSet = entry.getValue();
            int totalAvailable = countTotalAvailable(player, menu, requests, entry.getKey().stack());
            possibleSets = Math.min(possibleSets, totalAvailable / neededPerSet);
        }

        // 至少尝试填入 1 套（如果连 1 套都凑不齐，填入现有的材料供玩家查看缺料）
        int setsToFill = Math.max(1, possibleSets);

        List<SlotReq> round = new ArrayList<>(requests.size());
        for (SlotReq req : requests) {
            round.add(new SlotReq(req.slot(), req.stack().copyWithCount(req.stack().getCount() * setsToFill)));
        }

        fillCraftingGridBySlots(player, round);
    }

    private static int countTotalAvailable(
            ServerPlayer player,
            CraftingMenu menu,
            List<SlotReq> requests,
            ItemStack target
    ) {
        long count = 0;

        // 1. 合成格中当前已放置的同种材料
        for (SlotReq req : requests) {
            if (EnderBackpackStorage.same(req.stack(), target)) {
                ItemStack inSlot = menu.getSlot(req.slot()).getItem();
                if (EnderBackpackStorage.same(inSlot, target)) {
                    count += inSlot.getCount();
                }
            }
        }

        // 2. 玩家主物品栏中的可用材料
        for (int i = 0; i < 36; i++) {
            ItemStack inInv = player.getInventory().getItem(i);
            if (!EnderBackpackStorage.isStorageItem(inInv) && EnderBackpackStorage.same(inInv, target)) {
                count += inInv.getCount();
            }
        }

        // 3. 末影背包及其嵌套容器内的材料（含解压）
        count += EnderBackpackStorage.countAvailable(player, target);

        return (int) Math.min(Integer.MAX_VALUE, count);
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

        boolean changed = clearIncompatibleSlots(menu, requests, player);

        for (SlotReq req : requests) {
            Slot slot = menu.getSlot(req.slot());
            ItemStack required = req.stack();

            int limit = Math.min(required.getMaxStackSize(), slot.getMaxStackSize(required));
            int targetCount = Math.min(required.getCount(), limit);

            ItemStack current = slot.getItem();
            if (!current.isEmpty() && !EnderBackpackStorage.same(current, required)) {
                continue;
            }

            int currentCount = current.isEmpty() ? 0 : current.getCount();
            int needed = targetCount - currentCount;
            if (needed <= 0) {
                continue;
            }

            int extracted = extractFromSources(player, required, needed);
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
            menu.slotsChanged(menu.getSlot(1).container);
            menu.broadcastChanges();
            BackpackAutoCompressor.markDirty(player);
        }
    }

    private static boolean validRequests(CraftingMenu menu, List<SlotReq> requests) {
        if (requests == null || requests.isEmpty() || requests.size() > 9) {
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
            if (!slot.mayPlace(req.stack()) || slot.getMaxStackSize(req.stack()) <= 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean clearIncompatibleSlots(CraftingMenu menu, List<SlotReq> requests, ServerPlayer player) {
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

            if (wanted != null && EnderBackpackStorage.same(current, wanted.stack())) {
                continue;
            }

            ItemStack returned = current.copy();
            slot.set(ItemStack.EMPTY);

            if (!player.getInventory().add(returned) && !returned.isEmpty()) {
                player.drop(returned, false);
            }
            changed = true;
        }
        return changed;
    }

    private static int extractFromSources(ServerPlayer player, ItemStack target, int requested) {
        int remaining = requested;

        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (EnderBackpackStorage.isStorageItem(current) || !EnderBackpackStorage.same(current, target)) {
                continue;
            }

            int take = Math.min(current.getCount(), remaining);
            ItemStack updated = current.copy();
            updated.shrink(take);
            player.getInventory().setItem(slot, updated);
            remaining -= take;
        }

        if (remaining > 0) {
            remaining -= EnderBackpackStorage.extract(player, target, remaining);
        }
        return requested - remaining;
    }
}