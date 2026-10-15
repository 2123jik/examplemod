// EquipmentCabinetBlockEntity.java
package com.example.examplemod.entity;

import com.example.examplemod.menu.EquipmentCabinetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import static com.example.examplemod.registry.ModRegistry.CABINET_ENTITY;

public final class EquipmentCabinetBlockEntity extends BlockEntity
        implements Container, MenuProvider {

    public static final int CAPACITY = 256;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(CAPACITY, ItemStack.EMPTY);

    public EquipmentCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(CABINET_ENTITY.get(), pos, state);
    }
    /*
     * 包装已有 Container，直接使用 items 库存。
     *
     * InvWrapper 会通过 Container 的 setItem/removeItem 写入，
     * 因而沿用已有的 setChanged() 和持久化逻辑。
     *
     * 不要另外创建 ItemStackHandler，否则容易出现界面库存和管道库存
     * 各存一份物品的问题。
     */
    private final IItemHandler itemHandler = new InvWrapper(this);

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * 允许不可堆叠物品。兼容大多数武器、防具、工具及模组饰品。
     * 不按 LootCategory 拒绝入库，避免模组装备未分类就无法存放。
     */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1;
    }

    @Override
    public int getContainerSize() {
        return CAPACITY;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("装备柜");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EquipmentCabinetMenu(id, inventory, this);
    }
}
