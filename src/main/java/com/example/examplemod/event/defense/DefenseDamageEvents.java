package com.example.examplemod.event.defense;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber
public class DefenseDamageEvents {

    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerGrabManager.GrabData data = PlayerGrabManager.getGrabData(player);

            // 正在防御中
            if (data != null && data.isDefending) {
                // 1. 播放盾牌挡下重击的声音
                player.level().playSound(
                    null, 
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, 
                    SoundSource.PLAYERS, 
                    1.0f, 0.8f + player.getRandom().nextFloat() * 0.4f
                );

                // 2. 完全免疫伤害（或 event.setAmount(event.getAmount() * 0.2f) 减免 80%）
                event.setCanceled(true);

                // 3. （可选）海龟的坚硬龟壳承受微量耐久/伤害，或者弹开近战敌人
                if (event.getSource().getDirectEntity() != null) {
                    event.getSource().getDirectEntity().push(
                        player.getLookAngle().x * 0.5,
                        0.2,
                        player.getLookAngle().z * 0.5
                    );
                }
            }
        }
    }
}