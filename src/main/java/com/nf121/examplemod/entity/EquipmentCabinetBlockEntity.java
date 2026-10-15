package com.nf121.examplemod.entity;

import com.nf121.examplemod.menu.EquipmentCabinetMenu;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.Collections;

import static com.nf121.examplemod.registry.ModRegistry.CABINET_ENTITY;

public final class EquipmentCabinetBlockEntity extends BlockEntity
        implements Container, MenuProvider {

    public static final int CAPACITY = 256;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(CAPACITY, ItemStack.EMPTY);

    // 推荐提供支持 Registrate BlockEntityFactory 的 3 参数构造函数
    public EquipmentCabinetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 保留 2 参数构造函数兼容方块的 newBlockEntity
    public EquipmentCabinetBlockEntity(BlockPos pos, BlockState state) {
        this(CABINET_ENTITY.get(), pos, state);
    }

    private final IItemHandler itemHandler = new InvWrapper(this);

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

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
        // 关键修复：绝对不能调用 items.clear()！用 Collections.fill 保持列表容量为 256
        Collections.fill(this.items, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 关键修复：绝不能调用 items.clear()，先填满空物品，再读取 NBT
        Collections.fill(this.items, ItemStack.EMPTY);
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
