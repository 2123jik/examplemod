package com.example.examplemod.event;

import com.example.examplemod.util.WeaponReplacer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class MobEquipmentHandler {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {

        if (event.getLevel().isClientSide()) return;


        if (event.loadedFromDisk()) return;


        if (event.getEntity() instanceof Mob mob) {
            
            EquipmentSlot[] handSlots = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

            for (EquipmentSlot slot : handSlots) {
                ItemStack currentWeapon = mob.getItemBySlot(slot);
                ItemStack replaced = WeaponReplacer.tryReplace(currentWeapon, mob.getRandom());

                if (replaced != currentWeapon) {
                    mob.setItemSlot(slot, replaced);
                }
            }
        }
    }
}