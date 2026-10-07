package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity instanceof Animal) {
            
            // 收集新需要添加的掉落物实体，避免在遍历 drops 列表时直接修改导致 ConcurrentModificationException
            List<ItemEntity> extraDrops = new ArrayList<>();

            for (ItemEntity drop : event.getDrops()) {
                ItemStack originalStack = drop.getItem();
                if (originalStack.isEmpty()) continue;

                // 还需要增加 9 倍的数量（原本的 1 倍 + 9 倍 = 10 倍）
                int additionalCount = originalStack.getCount() * 9;

                // 拆分逻辑：物品堆最大堆叠通常为 64，超过 64 需要拆分成多个 ItemEntity
                while (additionalCount > 0) {
                    int addAmount = Math.min(additionalCount, originalStack.getMaxStackSize());
                    
                    ItemStack newStack = originalStack.copyWithCount(addAmount);
                    ItemEntity newDrop = new ItemEntity(
                            entity.level(),
                            drop.getX(), drop.getY(), drop.getZ(),
                            newStack
                    );
                    
                    // 复制原始掉落物的拾取延迟和运动向量
                    newDrop.setPickUpDelay(1);
                    newDrop.setDeltaMovement(drop.getDeltaMovement());

                    extraDrops.add(newDrop);
                    additionalCount -= addAmount;
                }
            }

            // 将计算出的额外 9 倍掉落加入最终掉落列表
            event.getDrops().addAll(extraDrops);
        }
    }
}