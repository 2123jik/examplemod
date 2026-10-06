package com.example.examplemod.item;

import com.example.examplemod.menu.PortableFurnaceMenu;
import com.example.examplemod.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class PortableFurnaceItem extends Item {
    public PortableFurnaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openFurnaceMenu(serverPlayer, held);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action == ClickAction.SECONDARY && slot.allowModification(player)) {
            if (other.isEmpty()) {
                if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    openFurnaceMenu(serverPlayer, stack);
                }
                return true;
            }
        }
        return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action == ClickAction.SECONDARY && slot.allowModification(player)) {
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                openFurnaceMenu(serverPlayer, stack);
            }
            return true;
        }
        return super.overrideStackedOnOther(stack, slot, action, player);
    }

    private void openFurnaceMenu(ServerPlayer serverPlayer, ItemStack furnaceStack) {
        PortableFurnaceData data = furnaceStack.getOrDefault(ModDataComponents.FURNACE_DATA.get(), PortableFurnaceData.EMPTY);

        SimpleContainer container = new SimpleContainer(3);
        for (int i = 0; i < 3; i++) {
            if (i < data.items().size()) {
                container.setItem(i, data.items().get(i).copy());
            }
        }

        SimpleContainerData syncData = new SimpleContainerData(4);
        syncData.set(0, data.litTime());
        syncData.set(1, data.litDuration());
        syncData.set(2, data.cookingProgress());
        syncData.set(3, data.cookingTotalTime() <= 0 ? 200 : data.cookingTotalTime());

        serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) -> new PortableFurnaceMenu(
                id, inv, container, syncData, p.level(),
                // 同步写回数据组件的回调
                () -> {
                    List<ItemStack> newItems = List.of(
                            container.getItem(0).copy(),
                            container.getItem(1).copy(),
                            container.getItem(2).copy()
                    );
                    furnaceStack.set(ModDataComponents.FURNACE_DATA.get(), new PortableFurnaceData(
                            newItems, syncData.get(0), syncData.get(1), syncData.get(2), syncData.get(3)
                    ));
                }
        ), Component.translatable("container.furnace")));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.examplemod.portable_furnace.tooltip")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        // 如果玩家正在打开便携熔炉菜单，跳过背包Tick，避免与菜单Tick重复烧炼
        if (player.containerMenu instanceof PortableFurnaceMenu) {
            return;
        }

        PortableFurnaceData data = stack.getOrDefault(ModDataComponents.FURNACE_DATA.get(), PortableFurnaceData.EMPTY);

        List<ItemStack> items = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            if (i < data.items().size()) {
                items.add(data.items().get(i).copy());
            } else {
                items.add(ItemStack.EMPTY);
            }
        }

        ItemStack input = items.get(0);
        ItemStack fuel = items.get(1);
        ItemStack output = items.get(2);

        int litTime = data.litTime();
        int litDuration = data.litDuration();
        int cookingProgress = data.cookingProgress();
        int cookingTotal = data.cookingTotalTime() <= 0 ? 200 : data.cookingTotalTime();

        boolean isDirty = false;

        // 1. 燃料燃烧时间递减
        if (litTime > 0) {
            litTime--;
            isDirty = true;
        }

        // 2. 检查配方和烧炼逻辑
        if (!input.isEmpty()) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            var recipeHolder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level).orElse(null);

            if (recipeHolder != null) {
                cookingTotal = recipeHolder.value().getCookingTime();
                ItemStack result = recipeHolder.value().assemble(recipeInput, level.registryAccess());
                boolean canInsert = output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize());

                if (canInsert) {
                    // 没有火时尝试点燃
                    if (litTime <= 0 && !fuel.isEmpty()) {
                        int burn = fuel.getBurnTime(RecipeType.SMELTING);
                        if (burn > 0) {
                            litTime = burn;
                            litDuration = burn;
                            ItemStack remainder = fuel.getCraftingRemainingItem();
                            fuel.shrink(1);
                            if (fuel.isEmpty()) {
                                items.set(1, remainder.copy());
                            }
                            isDirty = true;
                        }
                    }

                    // 燃烧中推进烧炼进度
                    if (litTime > 0) {
                        cookingProgress++;
                        isDirty = true;
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
                } else if (cookingProgress > 0) {
                    cookingProgress = 0;
                    isDirty = true;
                }
            } else if (cookingProgress > 0) {
                cookingProgress = 0;
                isDirty = true;
            }
        } else if (cookingProgress > 0) {
            cookingProgress = 0;
            isDirty = true;
        }

        // 只要状态改变就写回组件，确保无论是否打开界面，进度都能被持久化
        if (isDirty) {
            stack.set(ModDataComponents.FURNACE_DATA.get(), new PortableFurnaceData(
                    items, litTime, litDuration, cookingProgress, cookingTotal
            ));
        }
    }
}