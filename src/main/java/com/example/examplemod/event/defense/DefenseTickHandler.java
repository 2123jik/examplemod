package com.example.examplemod.event.defense;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class DefenseTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        PlayerGrabManager.GrabData data = PlayerGrabManager.getGrabData(player);
        if (data == null) return;

        LivingEntity turtle = data.entity;
        if (!turtle.isAlive() || turtle.level() != player.level()) {
            PlayerGrabManager.release(player);
            return;
        }

        Vec3 look = player.getLookAngle();

        if (data.isDefending) {
            // 防御时：吸附在胸前准星正前方 1.2 格处
            Vec3 shieldPos = player.getEyePosition()
                    .add(look.scale(1.2D))
                    .subtract(0, 0.45D, 0);

            turtle.teleportTo(shieldPos.x, shieldPos.y, shieldPos.z);
            turtle.setYRot(player.getYRot());
            turtle.setXRot(player.getXRot());
            turtle.setYHeadRot(player.getYRot());
            turtle.setYBodyRot(player.getYRot());

            // 举盾移速微调
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.85, 1.0, 0.85));
        } else {
            // 未防御时：收在身旁/身后悬浮跟随
            Vec3 sideOffset = look.reverse().scale(0.6D);
            Vec3 carryPos = player.position().add(sideOffset).add(0, 0.4D, 0);
            turtle.teleportTo(carryPos.x, carryPos.y, carryPos.z);
        }

        turtle.setDeltaMovement(Vec3.ZERO);
        turtle.resetFallDistance();

        if (player instanceof ServerPlayer sp) {
            sp.hurtMarked = true;
        }
    }
}