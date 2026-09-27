package com.example.examplemod.content.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;

public class PortableBrewingMenu extends BrewingStandMenu {
    private final Container container;
    private final ContainerData data;
    private final Level level;
    private final Runnable onClose;

    public PortableBrewingMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, Level level, Runnable onClose) {
        super(containerId, playerInventory, container, data);
        this.container = container;
        this.data = data;
        this.level = level;
        this.onClose = onClose;
    }

    @Override
    public void broadcastChanges() {
        if (!level.isClientSide) {
            tickBrewing();
        }
        super.broadcastChanges();
    }

    private void tickBrewing() {
        int brewTime = data.get(0);
        int fuel = data.get(1);

        ItemStack fuelItem = container.getItem(4);
        if (fuel <= 0 && fuelItem.is(Items.BLAZE_POWDER)) {
            fuel = 20;
            fuelItem.shrink(1);
            data.set(1, fuel);
            container.setChanged();
        }

        ItemStack ingredient = container.getItem(3);
        PotionBrewing potionBrewing = level.potionBrewing();

        boolean canBrew = false;
        if (fuel > 0 && !ingredient.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                ItemStack potion = container.getItem(i);
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
                data.set(1, fuel);
                for (int i = 0; i < 3; i++) {
                    ItemStack potion = container.getItem(i);
                    if (!potion.isEmpty() && potionBrewing.hasMix(potion, ingredient)) {
                        container.setItem(i, potionBrewing.mix(ingredient, potion));
                    }
                }
                ingredient.shrink(1);
                container.setChanged();
            }
            data.set(0, brewTime);
        } else {
            if (brewTime > 0) data.set(0, 0);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (onClose != null) onClose.run();
    }
}