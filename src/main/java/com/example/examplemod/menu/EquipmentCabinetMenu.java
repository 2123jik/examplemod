// EquipmentCabinetMenu.java
package com.example.examplemod.menu;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.EquipmentCabinetBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 所有真实物品移动由服务端 Menu 完成。
 * UI 中的筛选和排序只改变显示顺序，不修改真实槽位编号。
 */
public final class EquipmentCabinetMenu extends AbstractContainerMenu {
    public static final int CABINET_SLOTS = EquipmentCabinetBlockEntity.CAPACITY;
    public static final int PLAYER_START = CABINET_SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;

    private final Container cabinet;

    // 客户端工厂。读取 openMenu 写入的 BlockPos，物品通过 Menu 槽位同步。
    public EquipmentCabinetMenu(
            int id, Inventory inventory, RegistryFriendlyByteBuf buffer
    ) {
        this(id, inventory, new SimpleContainer(CABINET_SLOTS));
        buffer.readBlockPos();
    }

    public EquipmentCabinetMenu(int id, Inventory inventory, Container cabinet) {
        super(ExampleMod.CABINET_MENU.get(), id);
        checkContainerSize(cabinet, CABINET_SLOTS);
        this.cabinet = cabinet;
        cabinet.startOpen(inventory.player);

        for (int slot = 0; slot < CABINET_SLOTS; slot++) {
            addSlot(new Slot(cabinet, slot, -10000, -10000) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return EquipmentCabinetBlockEntity.accepts(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, -10000, -10000));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, -10000, -10000));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return cabinet.stillValid(player);
    }

    /**
     * 此界面只提供整件存取，关闭普通点击、拖拽、数字键换槽等路径。
     * 因而客户端不需要维护鼠标携带物品或原版槽位坐标。
     */
    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (type == ClickType.QUICK_MOVE
                && button == 0
                && slot >= 0
                && slot < slots.size()
                && stillValid(player)) {
            super.clicked(slot, button, type, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) {
            return ItemStack.EMPTY;
        }

        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();

        if (index < CABINET_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!EquipmentCabinetBlockEntity.accepts(stack)
                    || !moveItemStackTo(stack, 0, CABINET_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        source.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        cabinet.stopOpen(player);
    }
}
