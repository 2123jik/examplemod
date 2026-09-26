package com.example.examplemod.menu;

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
    private final Runnable onClose;

    public PortableFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, Level level, Runnable onClose) {
        super(containerId, playerInventory, container, data);
        this.container = container;
        this.data = data;
        this.level = level;
        this.onClose = onClose;
    }

    // 核心：打开界面期间，服务端每 tick 都会执行这里！
    @Override
    public void broadcastChanges() {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            tickSmelting(serverLevel);
        }
        super.broadcastChanges(); // 自动向客户端同步槽位物品与火苗、进度条
    }

    private void tickSmelting(ServerLevel level) {
        ItemStack input = container.getItem(0);
        ItemStack fuel = container.getItem(1);
        ItemStack output = container.getItem(2);

        int litTime = data.get(0);
        int litDuration = data.get(1);
        int cookingProgress = data.get(2);
        int cookingTotal = data.get(3) <= 0 ? 200 : data.get(3);

        boolean isLit = litTime > 0;
        if (isLit) {
            litTime--;
            data.set(0, litTime);
        }

        if (!input.isEmpty()) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            var recipeHolder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level).orElse(null);

            if (recipeHolder != null) {
                ItemStack result = recipeHolder.value().assemble(recipeInput, level.registryAccess());
                boolean canInsert = output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize());

                if (canInsert) {
                    // 火灭了且有燃料，自动点火
                    if (litTime <= 0 && !fuel.isEmpty()) {
                        int burn = fuel.getBurnTime(RecipeType.SMELTING);
                        if (burn > 0) {
                            litTime = burn;
                            litDuration = burn;
                            data.set(0, litTime);
                            data.set(1, litDuration);
                            
                            // 消耗燃料（如岩浆桶返还空桶）
                            ItemStack remainder = fuel.getCraftingRemainingItem();
                            fuel.shrink(1);
                            if (fuel.isEmpty() && !remainder.isEmpty()) {
                                container.setItem(1, remainder);
                            }
                            container.setChanged();
                        }
                    }

                    // 正在燃烧，推进度条
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
                    }
                } else {
                    data.set(2, 0);
                }
            } else {
                data.set(2, 0);
            }
        } else {
            data.set(2, 0);
        }
    }

    // 关掉界面时，把最新物品和进度写回 ItemStack
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (onClose != null) {
            onClose.run();
        }
    }
}