package com.example.examplemod.event;

import com.example.examplemod.util.WeaponReplacer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber
public class MobWeaponEventHandler {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // 1. 只在服务端处理逻辑，客户端直接跳过
        if (event.getLevel().isClientSide()) return;

        // 2. 核心避坑：如果是从磁盘加载的已有实体（如存档重进、区块重载），绝对不要再次替换！
        if (event.loadedFromDisk()) return;

        // 3. 筛选目标实体（比如僵尸，或者直接判断 Mob）
        if (event.getEntity() instanceof Mob mob) {

            // 4. 读取主手物品
            ItemStack mainHandStack = mob.getItemBySlot(EquipmentSlot.MAINHAND);

            if (!mainHandStack.isEmpty()) {
                // 5. 尝试按照“原生注册属性”同级/同档位替换
                ItemStack replacedStack = WeaponReplacer.tryReplace(mainHandStack, mob.getRandom());

                // 6. 如果发生了替换，放回主手
                if (replacedStack != mainHandStack) {
                    mob.setItemSlot(EquipmentSlot.MAINHAND, replacedStack);
                }
            }
        }
    }
}