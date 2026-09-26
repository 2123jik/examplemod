package com.example.examplemod.item;

import com.example.examplemod.menu.PortableFurnaceMenu;
import com.example.examplemod.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class PortableFurnaceItem extends Item {
    public PortableFurnaceItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PortableFurnaceData data = held.getOrDefault(ModDataComponents.FURNACE_DATA.get(), PortableFurnaceData.EMPTY);

            SimpleContainer container = new SimpleContainer(3);
            for (int i = 0; i < 3; i++) {
                if (i < data.items().size()) container.setItem(i, data.items().get(i));
            }

            SimpleContainerData syncData = new SimpleContainerData(4);
            syncData.set(0, data.litTime());
            syncData.set(1, data.litDuration());
            syncData.set(2, data.cookingProgress());
            syncData.set(3, data.cookingTotalTime() <= 0 ? 200 : data.cookingTotalTime());

            serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) -> new PortableFurnaceMenu(
                    id, inv, container, syncData, p.level(),
                    () -> {
                        // 关闭界面保存
                        List<ItemStack> newItems = List.of(container.getItem(0), container.getItem(1), container.getItem(2));
                        held.set(ModDataComponents.FURNACE_DATA.get(), new PortableFurnaceData(
                                newItems, syncData.get(0), syncData.get(1), syncData.get(2), syncData.get(3)
                        ));
                    }
            ), Component.translatable("container.furnace")));
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    // 背包后台离线烧炼
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player)) return;

        PortableFurnaceData data = stack.getOrDefault(ModDataComponents.FURNACE_DATA.get(), PortableFurnaceData.EMPTY);
        List<ItemStack> items = new ArrayList<>(data.items());
        while (items.size() < 3) items.add(ItemStack.EMPTY);

        ItemStack input = items.get(0);
        ItemStack fuel = items.get(1);
        ItemStack output = items.get(2);

        int litTime = data.litTime();
        int litDuration = data.litDuration();
        int cookingProgress = data.cookingProgress();
        int cookingTotal = data.cookingTotalTime() <= 0 ? 200 : data.cookingTotalTime();

        if (litTime > 0) litTime--;

        if (!input.isEmpty()) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            var recipeHolder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level).orElse(null);

            if (recipeHolder != null) {
                ItemStack result = recipeHolder.value().assemble(recipeInput, level.registryAccess());
                boolean canInsert = output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize());

                if (canInsert) {
                    if (litTime <= 0 && !fuel.isEmpty()) {
                        int burn = fuel.getBurnTime(RecipeType.SMELTING);
                        if (burn > 0) {
                            litTime = burn;
                            litDuration = burn;
                            ItemStack remainder = fuel.getCraftingRemainingItem();
                            fuel.shrink(1);
                            if (fuel.isEmpty() && !remainder.isEmpty()) {
                                items.set(1, remainder);
                            }
                        }
                    }

                    if (litTime > 0) {
                        cookingProgress++;
                        if (cookingProgress >= cookingTotal) {
                            cookingProgress = 0;
                            input.shrink(1);
                            if (output.isEmpty()) {
                                items.set(2, result.copy());
                            } else {
                                output.grow(result.getCount());
                            }
                        }
                    }
                } else {
                    cookingProgress = 0;
                }
            } else {
                cookingProgress = 0;
            }
        } else {
            cookingProgress = 0;
        }

        stack.set(ModDataComponents.FURNACE_DATA.get(), new PortableFurnaceData(items, litTime, litDuration, cookingProgress, cookingTotal));
    }
}