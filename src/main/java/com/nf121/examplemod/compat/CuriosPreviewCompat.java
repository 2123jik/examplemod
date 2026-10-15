package com.nf121.examplemod.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public final class CuriosPreviewCompat {
    private static final boolean LOADED =
            ModList.get().isLoaded("curios");

    private CuriosPreviewCompat() {}

    public static boolean isCurio(ItemStack stack) {
        if (!LOADED || stack.isEmpty()) {
            return false;
        }

        return CuriosPreviewImpl.isCurio(stack);
    }

    /**
     * 仅用于本模组创建的 UI 预览实体。
     * 清空普通饰品槽和外观饰品槽。
     *
     * 不要对真实玩家调用。
     */
    public static void clearPreviewCurios(LivingEntity dummyEntity) {
        if (!LOADED || dummyEntity == null) {
            return;
        }

        CuriosPreviewImpl.clearPreview(dummyEntity);
    }

    /**
     * 仅用于本模组创建的 UI 预览实体。
     *
     * 先清空已有饰品，再将物品副本放入一个可用槽位。
     * 传入空物品也会清空旧饰品，并返回 false。
     *
     * 不要对真实玩家调用。
     */
    public static boolean applySinglePreviewCurio(
            LivingEntity dummyEntity,
            ItemStack stack
    ) {
        if (!LOADED || dummyEntity == null) {
            return false;
        }

        return CuriosPreviewImpl.applyPreview(dummyEntity, stack);
    }

    /*
     * Curios API 的实际访问全部留在内部类。
     * 外层公开方法的签名不暴露 Curios 类型。
     */
    private static final class CuriosPreviewImpl {
        private CuriosPreviewImpl() {}

        private static boolean isCurio(ItemStack stack) {
            Map<String, ISlotType> slots =
                    CuriosApi.getItemStackSlots(stack, true);

            return slots != null && !slots.isEmpty();
        }

        private static void clearPreview(LivingEntity dummy) {
            var inventory = CuriosApi.getCuriosInventory(dummy);
            if (inventory.isPresent()) {
                clearHandler(inventory.get());
            }
        }

        private static boolean applyPreview(
                LivingEntity dummy,
                ItemStack stack
        ) {
            var inventory = CuriosApi.getCuriosInventory(dummy);
            if (inventory.isEmpty()) {
                return false;
            }

            ICuriosItemHandler handler = inventory.get();
            clearHandler(handler);

            if (stack.isEmpty()) {
                return false;
            }

            Map<String, ISlotType> slotTypes =
                    CuriosApi.getItemStackSlots(stack, true);

            if (slotTypes == null || slotTypes.isEmpty()) {
                return false;
            }

            var slotIds = new ArrayList<>(slotTypes.keySet());
            Collections.sort(slotIds);

            for (String slotId : slotIds) {
                var stacksHandler = handler.getCurios().get(slotId);
                if (stacksHandler == null) {
                    continue;
                }

                IDynamicStackHandler stacks = stacksHandler.getStacks();

                for (int index = 0; index < stacks.getSlots(); index++) {
                    if (!stacks.isItemValid(index, stack)) {
                        continue;
                    }

                    ItemStack previewStack = stack.copy();
                    previewStack.setCount(1);
                    stacks.setStackInSlot(index, previewStack);
                    return true;
                }
            }

            return false;
        }

        private static void clearHandler(ICuriosItemHandler handler) {
            for (var stacksHandler : handler.getCurios().values()) {
                clearStacks(stacksHandler.getStacks());
                clearStacks(stacksHandler.getCosmeticStacks());
            }
        }

        private static void clearStacks(IDynamicStackHandler stacks) {
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (!stacks.getStackInSlot(index).isEmpty()) {
                    stacks.setStackInSlot(index, ItemStack.EMPTY);
                }
            }
        }
    }
}
