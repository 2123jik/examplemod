package com.nf121.examplemod.item;

import com.nf121.examplemod.menu.PortableBrewingMenu;
import com.nf121.examplemod.registry.ModDataComponents;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class PortableBrewingStandItem extends Item {
    public PortableBrewingStandItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PortableBrewingData data = held.getOrDefault(ModDataComponents.BREWING_DATA.get(), PortableBrewingData.EMPTY);

            SimpleContainer container = new SimpleContainer(5);
            for (int i = 0; i < 5; i++) {
                if (i < data.items().size()) container.setItem(i, data.items().get(i));
            }

            SimpleContainerData syncData = new SimpleContainerData(2);
            syncData.set(0, data.brewTime());
            syncData.set(1, data.fuel());

            serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) -> new PortableBrewingMenu(
                    id, inv, container, syncData, p.level(),
                    () -> {
                        List<ItemStack> list = new ArrayList<>();
                        for (int i = 0; i < 5; i++) list.add(container.getItem(i));
                        held.set(ModDataComponents.BREWING_DATA.get(), new PortableBrewingData(list, syncData.get(0), syncData.get(1)));
                    }
            ), Component.translatable("container.brewing")));
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player)) return;

        PortableBrewingData data = stack.getOrDefault(ModDataComponents.BREWING_DATA.get(), PortableBrewingData.EMPTY);
        List<ItemStack> items = new ArrayList<>(data.items());
        while (items.size() < 5) items.add(ItemStack.EMPTY);

        int brewTime = data.brewTime();
        int fuel = data.fuel();

        ItemStack fuelItem = items.get(4);
        if (fuel <= 0 && fuelItem.is(Items.BLAZE_POWDER)) {
            fuel = 20;
            fuelItem.shrink(1);
        }

        ItemStack ingredient = items.get(3);
        PotionBrewing potionBrewing = level.potionBrewing();

        boolean canBrew = false;
        if (fuel > 0 && !ingredient.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                ItemStack potion = items.get(i);
                if (!potion.isEmpty() && potionBrewing.hasMix(potion, ingredient)) {
                    canBrew = true;
                    break;
                }
            }
        }

        if (canBrew) {
            brewTime++;
            if (brewTime >= 400) {
                brewTime = 0;
                fuel--;
                for (int i = 0; i < 3; i++) {
                    ItemStack potion = items.get(i);
                    if (!potion.isEmpty() && potionBrewing.hasMix(potion, ingredient)) {
                        items.set(i, potionBrewing.mix(ingredient, potion));
                    }
                }
                ingredient.shrink(1);
            }
        } else {
            brewTime = 0;
        }

        stack.set(ModDataComponents.BREWING_DATA.get(), new PortableBrewingData(items, brewTime, fuel));
    }
}