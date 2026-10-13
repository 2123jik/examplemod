package com.example.examplemod.event.defense;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class GrabInteractionEvents {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();

        // 仅在主手空手/下蹲时触发
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isCrouching()) {
            return;
        }

        // 手上已抓着生物时，下蹲右键放下
        if (PlayerGrabManager.isHoldingEntity(player)) {
            if (!player.level().isClientSide) {
                PlayerGrabManager.release(player);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 抓起海龟
//        if (event.getTarget() instanceof LivingEntity target && event.getTarget() instanceof Turtle) {
//            if (!player.level().isClientSide) {
//                PlayerGrabManager.grab(player, target);
//            }
//            event.setCanceled(true);
//            event.setCancellationResult(InteractionResult.SUCCESS);
//        }
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerGrabManager.GrabData data = PlayerGrabManager.getGrabData(player);

            // 正在防御时格挡
            if (data != null && data.isDefending) {
                // 1. 播放龟壳格挡清脆铁砧音效
                player.level().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                    0.6F, 1.8F
                );

                // 2. 免疫本次伤害
                event.setCanceled(true);

                // 3. 震退敌人
                if (event.getSource().getDirectEntity() != null) {
                    Vec3 knockback = player.getLookAngle().scale(0.8D);
                    event.getSource().getDirectEntity().push(knockback.x, 0.25D, knockback.z);
                }
            }
        }
    }
}