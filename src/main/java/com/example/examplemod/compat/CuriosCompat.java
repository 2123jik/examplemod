package com.example.examplemod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;

// 1. 显式声明仅客户端使用，防止服务端意外加载崩溃
@OnlyIn(Dist.CLIENT)
public final class CuriosCompat {
    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private CuriosCompat() {}

    public static boolean isCurio(ItemStack stack) {
        if (!LOADED || stack.isEmpty()) return false;
        return CuriosImpl.isCurio(stack);
    }

    public static boolean tryEquipCurio(Player player, ItemStack stack) {
        if (!LOADED || stack.isEmpty() || player == null) return false;
        return CuriosImpl.tryEquip(player, stack);
    }

    private static class CuriosImpl {
        private static boolean isCurio(ItemStack stack) {
            Level level = Minecraft.getInstance().level;
            if (level == null) return false;

            Map<String, ISlotType> slotTypes = CuriosApi.getItemStackSlots(stack, level);
            return slotTypes != null && !slotTypes.isEmpty();
        }

        private static boolean tryEquip(Player player, ItemStack stack) {
            // 2. 核心修正：优先使用传入实体自带的世界对象
            Level level = player.level();

            Map<String, ISlotType> slotTypes = CuriosApi.getItemStackSlots(stack, level);
            if (slotTypes == null || slotTypes.isEmpty()) return false;

            var curiosOpt = CuriosApi.getCuriosInventory(player);
            if (curiosOpt.isEmpty()) return false;

            ICuriosItemHandler handler = curiosOpt.get();

            // 3. 彻底清空假人残留：同时清空装备栏与时装外观栏
            handler.getCurios().forEach((slotId, stacksHandler) -> {
                // 清空装备栏
                IDynamicStackHandler stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    stacks.setStackInSlot(i, ItemStack.EMPTY);
                }
                // 清空时装栏（防止残留外观遮挡新饰品）
                IDynamicStackHandler cosmeticStacks = stacksHandler.getCosmeticStacks();
                for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                    cosmeticStacks.setStackInSlot(i, ItemStack.EMPTY);
                }
            });

            // 4. 将饰品放入第一个可用槽位
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