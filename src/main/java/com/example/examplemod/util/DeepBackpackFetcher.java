package com.example.examplemod.util;

import com.example.examplemod.network.RequestFetchRecipeIngredientsPayload.SlotReq;
import dev.xkmc.l2backpack.compat.CuriosCompat;
import dev.xkmc.l2backpack.content.bag.AbstractBag;
import dev.xkmc.l2backpack.content.common.BaseBagItem;
import dev.xkmc.l2backpack.content.remote.common.StorageContainer;
import dev.xkmc.l2backpack.content.remote.dimensional.DimensionalItem;
import dev.xkmc.l2backpack.content.remote.player.EnderBackpackItem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeepBackpackFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger("DeepBackpackFetcher");

    public static void fillCraftingGridBySlots(ServerPlayer player, List<SlotReq> slotRequests) {
        if (!(player.containerMenu instanceof CraftingMenu craftingMenu)) {
            LOGGER.warn("[DeepCraft] 玩家当前未处于工作台界面");
            return;
        }

        ServerLevel level = player.serverLevel();

        // 步骤 1：清空当前工作台九宫格中不匹配的残留物品，退回给玩家背包，防止被覆盖或误 grow
        clearIncompatibleSlots(craftingMenu, slotRequests, player);

        boolean gridChanged = false;

        for (SlotReq req : slotRequests) {
            int targetSlotIndex = req.slot(); // 对应工作台 Slot 1~9
            if (targetSlotIndex < 1 || targetSlotIndex > 9) continue;

            ItemStack required = req.stack();
            if (required.isEmpty()) continue;

            ItemStack currentInSlot = craftingMenu.getSlot(targetSlotIndex).getItem();

            // 若已有完全相同的物品且数量足够，跳过
            if (isSameItemStrict(currentInSlot, required) && currentInSlot.getCount() >= required.getCount()) {
                continue;
            }

            // 槽位如果不为空，且物品种类与目标不符，绝对不能往上叠加！
            if (!currentInSlot.isEmpty() && !isSameItemStrict(currentInSlot, required)) {
                // 退回背包
                if (!player.getInventory().add(currentInSlot)) {
                    player.drop(currentInSlot, false);
                }
                craftingMenu.getSlot(targetSlotIndex).set(ItemStack.EMPTY);
                currentInSlot = ItemStack.EMPTY;
            }

            int currentCount = currentInSlot.isEmpty() ? 0 : currentInSlot.getCount();
            int countNeeded = required.getCount() - currentCount;
            if (countNeeded <= 0) continue;

            // 从全量来源（随身背包、Curios、末影箱等）安全抽取
            int extractedCount = extractFromAllSources(player, level, required, countNeeded);
            if (extractedCount > 0) {
                if (currentInSlot.isEmpty()) {
                    currentInSlot = required.copyWithCount(extractedCount);
                } else {
                    currentInSlot.grow(extractedCount);
                }
                craftingMenu.getSlot(targetSlotIndex).set(currentInSlot);

                // 强制同步该 Slot 给客户端
                player.connection.send(new ClientboundContainerSetSlotPacket(
                        craftingMenu.containerId,
                        craftingMenu.incrementStateId(),
                        targetSlotIndex,
                        currentInSlot
                ));
                gridChanged = true;
            }
        }

        if (gridChanged) {
            craftingMenu.broadcastChanges();
            refreshResultSlot(craftingMenu, level, player);
        }
    }

    /**
     * 将九宫格中与配方无关的物品退回玩家背包
     */
    private static void clearIncompatibleSlots(CraftingMenu menu, List<SlotReq> requests, ServerPlayer player) {
        for (int i = 1; i <= 9; i++) {
            ItemStack inSlot = menu.getSlot(i).getItem();
            if (inSlot.isEmpty()) continue;

            final int slotIdx = i;
            // 检查当前格子是否是配方所需的物品
            Optional<SlotReq> matchReq = requests.stream().filter(r -> r.slot() == slotIdx).findFirst();
            if (matchReq.isEmpty() || !isSameItemStrict(inSlot, matchReq.get().stack())) {
                // 不匹配，退还玩家
                if (!player.getInventory().add(inSlot)) {
                    player.drop(inSlot, false);
                }
                menu.getSlot(i).set(ItemStack.EMPTY);
                player.connection.send(new ClientboundContainerSetSlotPacket(
                        menu.containerId,
                        menu.incrementStateId(),
                        i,
                        ItemStack.EMPTY
                ));
            }
        }
    }

    private static int extractFromAllSources(ServerPlayer player, ServerLevel level, ItemStack target, int countNeeded) {
        int remain = countNeeded;

        // 1. 随身主背包（0-35）
        for (int i = 0; i < 36 && remain > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            remain -= tryExtractDirectOrDeep(stack, target, remain, level, player);
        }

        // 2. 副手
        if (remain > 0) {
            ItemStack offhand = player.getItemBySlot(EquipmentSlot.OFFHAND);
            remain -= tryExtractDeep(offhand, target, remain, level, player, 0);
        }

        if (remain > 0) {
            final int curRemain = remain;
            final int[] extracted = {0};

            CuriosApi.getCuriosInventory(player).ifPresent(curiosInventory -> {
                Optional<SlotResult> slotOpt = curiosInventory.findFirstCurio(DeepBackpackFetcher::isBackpack);
                if (slotOpt.isPresent()) {
                    SlotResult slotResult = slotOpt.get();
                    ItemStack backpack = slotResult.stack();

                    extracted[0] = tryExtractDeep(backpack, target, curRemain, level, player, 0);

                    if (extracted[0] > 0) {
                        SlotContext ctx = slotResult.slotContext();

                        curiosInventory.getStacksHandler(ctx.identifier()).ifPresent(stacksHandler -> {
                            var itemHandler = ctx.cosmetic() ? stacksHandler.getCosmeticStacks() : stacksHandler.getStacks();
                            itemHandler.setStackInSlot(ctx.index(), backpack);
                        });
                    }
                }
            });

            remain -= extracted[0];
        }

        return countNeeded - remain;
    }

    private static int tryExtractDirectOrDeep(ItemStack stack, ItemStack target, int count, ServerLevel level, ServerPlayer player) {
        if (stack.isEmpty()) return 0;
        if (isSameItemStrict(stack, target)) {
            int take = Math.min(stack.getCount(), count);
            stack.shrink(take);
            return take;
        }
        if (isBackpack(stack)) {
            return tryExtractDeep(stack, target, count, level, player, 0);
        }
        return 0;
    }

    private static int tryExtractDeep(ItemStack containerStack, ItemStack target, int count, ServerLevel level, ServerPlayer player, int depth) {
        if (depth > 6 || containerStack.isEmpty() || count <= 0) return 0;
        int extracted = 0;

        // A. 普通背包
        if (containerStack.getItem() instanceof BaseBagItem) {
            List<ItemStack> items = new ArrayList<>(BaseBagItem.getItems(containerStack));
            boolean changed = false;
            for (int i = 0; i < items.size() && extracted < count; i++) {
                ItemStack sub = items.get(i);
                if (sub.isEmpty()) continue;

                if (isSameItemStrict(sub, target)) {
                    int take = Math.min(sub.getCount(), count - extracted);
                    sub.shrink(take);
                    extracted += take;
                    changed = true;
                } else if (isBackpack(sub)) {
                    int subExtracted = tryExtractDeep(sub, target, count - extracted, level, player, depth + 1);
                    if (subExtracted > 0) {
                        extracted += subExtracted;
                        changed = true;
                    }
                }
            }
            if (changed) {
                BaseBagItem.setItems(containerStack, items);
            }
            return extracted;
        }

        // B. 功能袋
        if (containerStack.getItem() instanceof AbstractBag bag) {
            NonNullList<ItemStack> list = bag.getContent(containerStack);
            boolean changed = false;
            for (int i = 0; i < list.size() && extracted < count; i++) {
                ItemStack sub = list.get(i);
                if (sub.isEmpty()) continue;

                if (isSameItemStrict(sub, target)) {
                    int take = Math.min(sub.getCount(), count - extracted);
                    sub.shrink(take);
                    extracted += take;
                    changed = true;
                }
            }
            if (changed) {
                bag.setContent(containerStack, list);
            }
            return extracted;
        }

        // C. 末影背包
        if (containerStack.getItem() instanceof EnderBackpackItem) {
            Container enderInv = player.getEnderChestInventory();
            for (int i = 0; i < enderInv.getContainerSize() && extracted < count; i++) {
                ItemStack sub = enderInv.getItem(i);
                if (sub.isEmpty()) continue;

                if (isSameItemStrict(sub, target)) {
                    int take = Math.min(sub.getCount(), count - extracted);
                    sub.shrink(take);
                    extracted += take;
                    enderInv.setChanged();
                } else if (isBackpack(sub)) {
                    int subExtracted = tryExtractDeep(sub, target, count - extracted, level, player, depth + 1);
                    if (subExtracted > 0) {
                        extracted += subExtracted;
                        enderInv.setChanged();
                    }
                }
            }
            return extracted;
        }

        // D. 空间背包
        if (containerStack.getItem() instanceof DimensionalItem dimItem) {
            Optional<StorageContainer> storageOpt = dimItem.getContainer(containerStack, level);
            if (storageOpt.isPresent()) {
                Container storage = storageOpt.get().get();
                for (int i = 0; i < storage.getContainerSize() && extracted < count; i++) {
                    ItemStack sub = storage.getItem(i);
                    if (sub.isEmpty()) continue;

                    if (isSameItemStrict(sub, target)) {
                        int take = Math.min(sub.getCount(), count - extracted);
                        sub.shrink(take);
                        extracted += take;
                        storage.setChanged();
                    } else if (isBackpack(sub)) {
                        int subExtracted = tryExtractDeep(sub, target, count - extracted, level, player, depth + 1);
                        if (subExtracted > 0) {
                            extracted += subExtracted;
                            storage.setChanged();
                        }
                    }
                }
            }
            return extracted;
        }

        return 0;
    }

    private static void refreshResultSlot(CraftingMenu menu, ServerLevel level, ServerPlayer player) {
        List<ItemStack> gridItems = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            gridItems.add(menu.getSlot(i).getItem());
        }
        CraftingInput input = CraftingInput.of(3, 3, gridItems);

        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (match.isPresent()) {
            result = match.get().value().assemble(input, level.registryAccess());
        }

        menu.getSlot(0).set(result);
        player.connection.send(new ClientboundContainerSetSlotPacket(
                menu.containerId,
                menu.incrementStateId(),
                0,
                result
        ));
    }

    private static boolean isBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof BaseBagItem ||
                stack.getItem() instanceof AbstractBag ||
                stack.getItem() instanceof EnderBackpackItem ||
                stack.getItem() instanceof DimensionalItem;
    }

    /**
     * 严格检查物品种类，并且避免有复杂 NBT / DataComponents 的非原料被错误当作同类物品抽取
     */
    private static boolean isSameItemStrict(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        if (!a.is(b.getItem())) return false;
        // 如果两者包含自定义组件且不一致，不应视作同种原料
        return ItemStack.isSameItemSameComponents(a, b) || (!a.isDamageableItem() && !a.isEnchanted() && !b.isEnchanted());
    }
}