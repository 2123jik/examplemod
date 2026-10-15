package com.nf121.examplemod.util;

import dev.xkmc.l2backpack.events.CapabilityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class BackpackSortHelper {

    public static void sortInventoryToBackpack(ServerPlayer player, boolean fromContainerOnly) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) return;

        if (menu instanceof InventoryMenu && fromContainerOnly) {
            return;
        }

        if (fromContainerOnly && isEnderOrBackpackMenu(player, menu)) {
            return;
        }

        boolean changed = false;

        for (Slot slot : menu.slots) {
            if (!slot.mayPickup(player)) continue;

            boolean isPlayerInventorySlot = (slot.container == player.getInventory());
            if (fromContainerOnly && isPlayerInventorySlot) continue;
            if (!fromContainerOnly && !isPlayerInventorySlot) continue;

            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || isBackpackItem(stack)) continue;

            ItemStack toInsert = stack.copy();
            int originalCount = toInsert.getCount();

            CapabilityEvents.tryInsertItem(player, toInsert);

            int insertedCount = originalCount - toInsert.getCount();
            if (insertedCount > 0) {
                slot.remove(insertedCount);
                slot.setChanged();
                changed = true;
            }
        }

        if (changed) {
            menu.broadcastChanges();
            // 收纳动作完成后，标记脏状态，触发后续延迟自动整理
            BackpackAutoCompressor.markDirty(player);
        }
    }

    private static boolean isEnderOrBackpackMenu(ServerPlayer player, AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu chestMenu) {
            if (chestMenu.getContainer() instanceof PlayerEnderChestContainer) {
                return true;
            }
        }
        String menuClassName = menu.getClass().getName();
        return menuClassName.contains("l2backpack") || menuClassName.contains("Backpack");
    }

    private static boolean isBackpackItem(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("backpack") || itemId.contains("shulker_box");
    }
}