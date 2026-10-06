package com.example.examplemod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;

public final class CuriosPreviewCompat {
    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private CuriosPreviewCompat() {}

    public static boolean isCurio(ItemStack stack) {
        if (!LOADED || stack.isEmpty()) return false;
        return CuriosPreviewImpl.isCurio(stack);
    }

    /**
     * 仅用于 UI 预览用实体（Dummy Entity），会清空旧饰品并将该物品设置到首个对应槽位
     */
    public static boolean applySinglePreviewCurio(LivingEntity dummyEntity, ItemStack stack) {
        if (!LOADED || stack.isEmpty() || dummyEntity == null) return false;
        return CuriosPreviewImpl.applyPreview(dummyEntity, stack);
    }

    private static class CuriosPreviewImpl {
        private static boolean isCurio(ItemStack stack) {
            // 不需要依赖非空 level，直接传入 isClient = true 即可
            Map<String, ISlotType> slotTypes = CuriosApi.getItemStackSlots(stack, true);
            return slotTypes != null && !slotTypes.isEmpty();
        }

        private static boolean applyPreview(LivingEntity dummy, ItemStack stack) {
            var curiosOpt = CuriosApi.getCuriosInventory(dummy);
            if (curiosOpt.isEmpty()) return false;

            ICuriosItemHandler handler = curiosOpt.get();

            // 1. 清空假人身上的全部饰品槽
            handler.getCurios().forEach((slotId, stacksHandler) -> {
                IDynamicStackHandler stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    stacks.setStackInSlot(i, ItemStack.EMPTY);
                }
                IDynamicStackHandler cosmeticStacks = stacksHandler.getCosmeticStacks();
                for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                    cosmeticStacks.setStackInSlot(i, ItemStack.EMPTY);
                }
            });

            // 2. 找到对应槽位装备
            Map<String, ISlotType> slotTypes = CuriosApi.getItemStackSlots(stack, true);
            for (String slotId : slotTypes.keySet()) {
                var stacksHandler = handler.getCurios().get(slotId);
                if (stacksHandler != null && stacksHandler.getStacks().getSlots() > 0) {
                    stacksHandler.getStacks().setStackInSlot(0, stack.copy());
                    return true;
                }
            }

            return false;
        }
    }
}