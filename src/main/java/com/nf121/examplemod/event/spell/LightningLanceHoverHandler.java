package com.nf121.examplemod.event.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class LightningLanceHoverHandler {

    // 雷枪的法术 ResourceLocation 字符串
    private static final String LIGHTNING_LANCE_ID = "irons_spellbooks:lightning_lance";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // 仅在服务端处理移动同步
        if (player.level().isClientSide) {
            return;
        }

        // 使用反编译源码中的静态方法获取 MagicData
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        // 1. 判断是否正在施法，并且施放的是雷枪
        if (magicData.isCasting() && LIGHTNING_LANCE_ID.equals(magicData.getCastingSpellId())) {
            
            // 获取当前蓄力进度（0.0 ~ 1.0）
            float progress = magicData.getCastCompletionPercent();
            Vec3 currentMotion = player.getDeltaMovement();

            // 2. 动态手感控制：
            // 前 30% 进度快速升空；30% 之后在空中悬停/极缓慢上升蓄力
            double targetUpwardSpeed;
            if (progress < 0.3F) {
                targetUpwardSpeed = 0.45D; // 快速起跳腾空
            } else {
                targetUpwardSpeed = 0.05D; // 空中微升悬停，对抗原版重力
            }

            // 给予垂直速度
            player.setDeltaMovement(currentMotion.x, targetUpwardSpeed, currentMotion.z);
            player.hasImpulse = true;

            // 3. 施加同步标志，防止客户端预测不同步
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.hurtMarked = true;
            }

            // 4. 重置摔落高度，避免施法结束掉下来摔死
            player.resetFallDistance();
        }
    }
}