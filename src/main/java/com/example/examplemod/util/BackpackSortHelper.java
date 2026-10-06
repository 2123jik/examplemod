package com.example.examplemod.util;

import dev.xkmc.l2backpack.events.CapabilityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BackpackSortHelper {

    public static void sortInventoryToBackpack(ServerPlayer player, boolean fromContainerOnly) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) return;

        // 【防御 1】如果是玩家自己的原生物品栏界面，根本没有外部容器，直接拒绝 fromContainerOnly
        if (menu instanceof InventoryMenu && fromContainerOnly) {
            return;
        }

        // 【防御 2】禁止在打开末影箱/末影背包时，从外部容器向身上吸收（防止把末影箱存入末影背包造成闭环）
        if (fromContainerOnly && isEnderOrBackpackMenu(player, menu)) {
            return;
        }

        boolean changed = false;

        for (Slot slot : menu.slots) {
            // 安全检查：忽略没有提取权限的槽位（例如铁砧输出槽、只读槽）
            if (!slot.mayPickup(player)) continue;

            boolean isPlayerInventorySlot = (slot.container == player.getInventory());

            // 槽位类型过滤
            if (fromContainerOnly && isPlayerInventorySlot) continue;
            if (!fromContainerOnly && !isPlayerInventorySlot) continue;

            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            // 【防御 3】防止将有容器属性/自身就是背包的物品收纳（防止套娃吃包）
            if (isBackpackItem(stack)) continue;

            // 【防御 4：核心对象隔离】创建副本进行测试，严禁直接把容器内的引用传给 tryInsertItem
            ItemStack toInsert = stack.copy();
            int originalCount = toInsert.getCount();

            CapabilityEvents.tryInsertItem(player, toInsert);

            int insertedCount = originalCount - toInsert.getCount();

            // 如果确实有物品被吸收了，安全地从原槽位移除对应数量
            if (insertedCount > 0) {
                slot.remove(insertedCount);
                slot.setChanged();
                changed = true;
            }
        }

        if (changed) {
            menu.broadcastChanges();
        }
    }

    /**
     * 判断当前打开的容器是否是末影箱或背包容器
     */
    private static boolean isEnderOrBackpackMenu(ServerPlayer player, AbstractContainerMenu menu) {
        // 检查是否打开了原版末影箱
        if (menu instanceof ChestMenu chestMenu) {
            if (chestMenu.getContainer() instanceof PlayerEnderChestContainer) {
                return true;
            }
        }
        // 检查类的全名（避免类不存在时报错，排除 l2backpack 的各种容器）
        String menuClassName = menu.getClass().getName();
        return menuClassName.contains("l2backpack") || menuClassName.contains("Backpack");
    }

    /**
     * 判断物品是否是背包自身，防止自吞
     */
    private static boolean isBackpackItem(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("backpack") || itemId.contains("shulker_box");
    }
}