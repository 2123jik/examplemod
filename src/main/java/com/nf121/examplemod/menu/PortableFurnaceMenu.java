package com.nf121.examplemod.menu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class PortableFurnaceMenu extends FurnaceMenu {
    private final Container container;
    private final ContainerData data;
    private final Level level;
    private final Runnable saveCallback;

    public PortableFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, Level level, Runnable saveCallback) {
        super(containerId, playerInventory, container, data);
        this.container = container;
        this.data = data;
        this.level = level;
        this.saveCallback = saveCallback;
    }

    @Override
    public void broadcastChanges() {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            tickSmelting(serverLevel);
        }
        super.broadcastChanges();
    }

    private void tickSmelting(ServerLevel level) {
        ItemStack input = container.getItem(0);
        ItemStack fuel = container.getItem(1);
        ItemStack output = container.getItem(2);

        int litTime = data.get(0);
        int litDuration = data.get(1);
        int cookingProgress = data.get(2);
        int cookingTotal = data.get(3) <= 0 ? 200 : data.get(3);

        boolean isDirty = false;

        if (litTime > 0) {
            litTime--;
            data.set(0, litTime);
            isDirty = true;
        }

        if (!input.isEmpty()) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            var recipeHolder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level).orElse(null);

            if (recipeHolder != null) {
                cookingTotal = recipeHolder.value().getCookingTime();
                data.set(3, cookingTotal);

                ItemStack result = recipeHolder.value().assemble(recipeInput, level.registryAccess());
                boolean canInsert = output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize());

                if (canInsert) {
                    if (litTime <= 0 && !fuel.isEmpty()) {
                        int burn = fuel.getBurnTime(RecipeType.SMELTING);
                        if (burn > 0) {
                            litTime = burn;
                            litDuration = burn;
                            data.set(0, litTime);
                            data.set(1, litDuration);

                            ItemStack remainder = fuel.getCraftingRemainingItem();
                            fuel.shrink(1);
                            if (fuel.isEmpty()) {
                                container.setItem(1, remainder.copy());
                            }
                            container.setChanged();
                            isDirty = true;
                        }
                    }

                    if (litTime > 0) {
                        cookingProgress++;
                        if (cookingProgress >= cookingTotal) {
                            cookingProgress = 0;
                            input.shrink(1);
                            if (output.isEmpty()) {
                                container.setItem(2, result.copy());
                            } else {
                                output.grow(result.getCount());
                            }
                            container.setChanged();
                        }
                        data.set(2, cookingProgress);
                        isDirty = true;
                    }
                } else if (cookingProgress > 0) {
                    data.set(2, 0);
                    isDirty = true;
                }
            } else if (cookingProgress > 0) {
                data.set(2, 0);
                isDirty = true;
            }
        } else if (cookingProgress > 0) {
            data.set(2, 0);
            isDirty = true;
        }

        // 当界面打开并处于燃烧中时，每秒或者数据变化时同步更新到物品数据
        if (isDirty && saveCallback != null && (level.getGameTime() % 20 == 0 || container.getItem(0).isEmpty() || !container.getItem(2).isEmpty())) {
            saveCallback.run();
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (saveCallback != null) {
            saveCallback.run();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (saveCallback != null) {
            saveCallback.run();
        }
    }
}