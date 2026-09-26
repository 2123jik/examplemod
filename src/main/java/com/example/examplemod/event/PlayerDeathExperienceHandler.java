package com.example.examplemod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "examplemod")
public class PlayerDeathExperienceHandler {

    /**
     * 处理玩家重生时的经验继承：扣除 1 级并清空进度条
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 只处理死亡重生的情况（排除从末地传送门返回等非死亡 clone）
        if (!event.isWasDeath()) {
            return;
        }

        ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();
        ServerPlayer newPlayer = (ServerPlayer) event.getEntity();

        // 计算新等级（最低为 0）
        int newLevel = Math.max(0, oldPlayer.experienceLevel - 1);

        newPlayer.experienceProgress = 0.0F;
        newPlayer.experienceLevel = newLevel;
        
        // 建议：重新计算并校准 totalExperience（可选，但更规范）
        newPlayer.totalExperience = calculateTotalXpForLevel(newLevel);
    }

    /**
     * 阻止玩家死亡时在地上生成经验球，避免刷经验
     */
    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCanceled(true); // 取消经验球掉落
        }
    }

    /**
     * 辅助方法：计算对应等级的基础总经验值
     */
    private static int calculateTotalXpForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }
}