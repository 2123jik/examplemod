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
        // 1. 仅在服务端处理，防止客户端与服务端数据不一致
        if (event.getLevel().isClientSide()) return;

        // 2. 仅对新生成的实体生效，排除从存档载入的实体（避免每次读档重复刷新武器）
        if (event.loadedFromDisk()) return;

        // 3. 拦截所有生物（包括原版僵尸/骷髅/猪灵，以及模组生成的怪物）
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