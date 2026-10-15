// EquipmentCabinetMenu.java
package com.nf121.examplemod.menu;

import com.nf121.examplemod.entity.EquipmentCabinetBlockEntity;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.nf121.examplemod.registry.ModRegistry.CABINET_MENU;

/**
 * 所有真实物品移动由服务端 Menu 完成。
 * UI 中的筛选和排序只改变显示顺序，不修改真实槽位编号。
 */
public final class EquipmentCabinetMenu extends AbstractContainerMenu {
    public static final int CABINET_SLOTS = EquipmentCabinetBlockEntity.CAPACITY;
    public static final int PLAYER_START = CABINET_SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;

    private final Container cabinet;

    public EquipmentCabinetMenu(
            int id, Inventory inventory, RegistryFriendlyByteBuf buffer
    ) {
        this(id, inventory, new SimpleContainer(CABINET_SLOTS));
        buffer.readBlockPos();
    }

    public EquipmentCabinetMenu(int id, Inventory inventory, Container cabinet) {
        super(CABINET_MENU.get(), id);
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
    /**
     * 服务端响应客户端的回收/分解请求
     * id 为要分解的柜子槽位编号 (0 ~ CABINET_SLOTS - 1)
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < CABINET_SLOTS && stillValid(player)) {
            Slot slot = this.getSlot(id);
            ItemStack stack = slot.getItem();

            if (!stack.isEmpty() && !SalvagingMenu.findMatch(player.level(), stack).isEmpty()) {
                Level level = player.level();

                // 1. 根据神化配方和装备耐久损耗计算回收产物
                List<ItemStack> outputs = SalvagingMenu.getSalvageResults(level, stack);

                // 2. 消耗掉柜子中的原装备
                slot.set(ItemStack.EMPTY);

                // 3. 产物返还给玩家背包，装不下则掉落到地上
                for (ItemStack out : outputs) {
                    if (!player.getInventory().add(out)) {
                        player.drop(out, false);
                    }
                }

                // 4. 播放神化原版分解的三重音效
                level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 0.99F, level.random.nextFloat() * 0.25F + 1.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.BLOCKS, 0.34F, level.random.nextFloat() * 0.2F + 0.8F);
                level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.45F, level.random.nextFloat() * 0.5F + 0.75F);

                return true;
            }
        }
        return super.clickMenuButton(player, id);
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
