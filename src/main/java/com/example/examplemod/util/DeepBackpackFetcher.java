package com.example.examplemod.util;

import com.example.examplemod.registry.ModPayloads.RequestFetchRecipeIngredientsPayload.SlotReq;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

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
            int requestedSets
    ) {
        if (!(player.containerMenu instanceof CraftingMenu menu)
                || !menu.stillValid(player)
                || !validRequests(menu, requests)) {
            return;
        }

        requestedSets = Math.max(1, Math.min(64, requestedSets));

        /*
         * 先计算合成格单槽能容纳的最大套数。
         *
         * 通常每个 Ingredient 每套需要 1 个，但仍然按照 req.stack()
         * 的数量进行通用计算。
         */
        int slotLimitedSets = requestedSets;

        for (SlotReq req : requests) {
            ItemStack ingredient = req.stack();
            Slot slot = menu.getSlot(req.slot());

            int perSet = ingredient.getCount();
            int stackLimit = Math.min(
                    ingredient.getMaxStackSize(),
                    slot.getMaxStackSize(ingredient)
            );

            if (perSet <= 0) {
                return;
            }

            slotLimitedSets = Math.min(
                    slotLimitedSets,
                    stackLimit / perSet
            );
        }

        if (slotLimitedSets <= 0) {
            return;
        }

        /*
         * 聚合同一种物品在一套配方中的总消耗。
         *
         * 例如两个槽都需要潜影壳，那么每套总共需要两个潜影壳，
         * 不能分别按每个槽拥有的总量计算。
         */
        Map<ItemKey, Integer> perCraftCount = new HashMap<>();

        for (SlotReq req : requests) {
            perCraftCount.merge(
                    new ItemKey(req.stack()),
                    req.stack().getCount(),
                    Integer::sum
            );
        }

        int availableSets = slotLimitedSets;

        for (Map.Entry<ItemKey, Integer> entry : perCraftCount.entrySet()) {
            int neededPerSet = entry.getValue();

            if (neededPerSet <= 0) {
                return;
            }

            int totalAvailable = countTotalAvailable(
                    player,
                    menu,
                    requests,
                    entry.getKey().stack()
            );

            availableSets = Math.min(
                    availableSets,
                    totalAvailable / neededPerSet
            );
        }

        /*
         * 连一整套都没有时不修改合成格。
         *
         * EMI 已经允许玩家点击按钮，但服务端最终检查发现材料不足后，
         * 直接保持原状。
         */
        if (availableSets <= 0) {
            return;
        }

        int setsToFill = Math.min(
                requestedSets,
                availableSets
        );

        List<SlotReq> expanded = new ArrayList<>(requests.size());

        for (SlotReq req : requests) {
            long wanted = (long) req.stack().getCount() * setsToFill;

            if (wanted <= 0 || wanted > Integer.MAX_VALUE) {
                return;
            }

            expanded.add(
                    new SlotReq(
                            req.slot(),
                            req.stack().copyWithCount((int) wanted)
                    )
            );
        }

        fillCraftingGridBySlots(
                player,
                expanded
        );
    }

    public static void fillRecipe(
            ServerPlayer player,
            ResourceLocation recipeId,
            int requestedSets
    ) {
        if (!(player.containerMenu instanceof CraftingMenu menu)
                || !menu.stillValid(player)) {
            return;
        }

        RecipeHolder<?> holder = player.serverLevel()
                .getRecipeManager()
                .byKey(recipeId)
                .orElse(null);

        if (holder == null
                || !(holder.value() instanceof CraftingRecipe recipe)) {
            return;
        }

        // 动态配方不能直接按照 Ingredient 列表填充。
        if (!(recipe instanceof ShapedRecipe)
                && !(recipe instanceof ShapelessRecipe)) {
            return;
        }

        List<SlotReq> requests = createRequests(
                player,
                menu,
                recipe
        );

        if (requests.isEmpty()) {
            return;
        }

        fillCraftingGrid(
                player,
                requests,
                requestedSets
        );
    }

    private static List<SlotReq> createRequests(
            ServerPlayer player,
            CraftingMenu menu,
            CraftingRecipe recipe
    ) {
        List<SlotReq> requests = new ArrayList<>();
        Map<ItemKey, Integer> reserved = new HashMap<>();

        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();

            if (width <= 0 || width > 3
                    || height <= 0 || height > 3) {
                return List.of();
            }

            List<Ingredient> ingredients =
                    shaped.getIngredients();

            if (ingredients.size() < width * height) {
                return List.of();
            }

            /*
             * 和原版配方书相同，较小的配方放在合成格左上方。
             *
             * CraftingMenu 槽位：
             * 0     = 输出
             * 1..9  = 3x3 输入
             */
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Ingredient ingredient =
                            ingredients.get(y * width + x);

                    if (ingredient.isEmpty()) {
                        continue;
                    }

                    ItemStack selected = selectIngredient(
                            player,
                            menu,
                            ingredient,
                            reserved
                    );

                    if (selected.isEmpty()) {
                        return List.of();
                    }

                    int craftingSlot = 1 + y * 3 + x;

                    requests.add(
                            new SlotReq(
                                    craftingSlot,
                                    selected.copyWithCount(1)
                            )
                    );

                    reserved.merge(
                            new ItemKey(selected),
                            1,
                            Integer::sum
                    );
                }
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            List<Ingredient> ingredients =
                    shapeless.getIngredients();

            if (ingredients.isEmpty()
                    || ingredients.size() > 9) {
                return List.of();
            }

            int craftingSlot = 1;

            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    continue;
                }

                ItemStack selected = selectIngredient(
                        player,
                        menu,
                        ingredient,
                        reserved
                );

                if (selected.isEmpty()) {
                    return List.of();
                }

                requests.add(
                        new SlotReq(
                                craftingSlot++,
                                selected.copyWithCount(1)
                        )
                );

                reserved.merge(
                        new ItemKey(selected),
                        1,
                        Integer::sum
                );
            }
        }

        return requests;
    }
    private static ItemStack selectIngredient(
            ServerPlayer player,
            CraftingMenu menu,
            Ingredient ingredient,
            Map<ItemKey, Integer> reserved
    ) {
        ItemStack[] candidates =
                ingredient.getItems();

        if (candidates.length == 0) {
            return ItemStack.EMPTY;
        }

        ItemStack best = ItemStack.EMPTY;
        long bestAvailable = Long.MIN_VALUE;

        for (ItemStack candidate : candidates) {
            if (candidate.isEmpty()
                    || EnderBackpackStorage.isStorageItem(candidate)) {
                continue;
            }

            /*
             * Ingredient#getItems 一般已经返回合法候选。
             * 再 test 一次可以避免部分自定义 Ingredient 返回异常候选。
             */
            if (!ingredient.test(candidate)) {
                continue;
            }

            long total = countCandidateEverywhere(
                    player,
                    menu,
                    candidate
            );

            int alreadyReserved =
                    reserved.getOrDefault(
                            new ItemKey(candidate),
                            0
                    );

            long available =
                    Math.max(0L, total - alreadyReserved);

            if (best.isEmpty()
                    || available > bestAvailable) {
                best = candidate.copyWithCount(1);
                bestAvailable = available;
            }
        }

        /*
         * 即使当前数量为 0 也返回一个配方候选。
         * fillCraftingGrid 会尽量填入已有材料，不会凭空创建。
         */
        return best;
    }
    private static long countCandidateEverywhere(
            ServerPlayer player,
            CraftingMenu menu,
            ItemStack target
    ) {
        long count = 0;

        // 当前 3x3 合成格。
        for (int slotIndex = 1; slotIndex <= 9; slotIndex++) {
            ItemStack current =
                    menu.getSlot(slotIndex).getItem();

            if (EnderBackpackStorage.same(current, target)) {
                count += current.getCount();
            }
        }

        // 玩家主物品栏和快捷栏。
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            ItemStack current =
                    player.getInventory().getItem(slotIndex);

            if (EnderBackpackStorage.isStorageItem(current)) {
                continue;
            }

            if (EnderBackpackStorage.same(current, target)) {
                count += current.getCount();
            }
        }

        /*
         * 末影背包系统：
         * - 玩家饰品栏佩戴的末影背包
         * - 玩家末影箱
         * - 普通嵌套背包
         * - 维度存储
         * - 压缩块解压后的折算数量
         */
        count += EnderBackpackStorage.countAvailable(
                player,
                target
        );

        return count;
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