package com.example.examplemod.event.l2;

import dev.xkmc.l2hostility.content.capability.chunk.ChunkDifficulty;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.events.HostilityInitEvent;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = "examplemod")
public class HostileGolemHostilityHandler {

    /**
     * 1. 过滤：阻止非敌对傀儡（如玩家制作、召唤的傀儡）初始化恶意等级和词条
     */
    @SubscribeEvent
    public static void onHostilityInitPre(HostilityInitEvent.Pre event) {
        if (event.getPhase() == HostilityInitEvent.InitPhase.INIT) {
            if (event.getEntity() instanceof AbstractGolemEntity<?, ?> golem) {
                // 如果该傀儡不是敌对傀儡，取消初始化，使其词条与等级为 0
                if (!golem.isHostile()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * 2. 生成检测：当敌对傀儡加入世界时，若尚未初始化恶意，手动触发初始化
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof AbstractGolemEntity<?, ?> golem) {
            // 确保是敌对阵营的傀儡
            if (golem.isHostile()) {
                initGolemHostility(golem);
            }
        }
    }

    /**
     * 为敌对傀儡初始化 L2Hostility 恶意数据
     */
    public static void initGolemHostility(AbstractGolemEntity<?, ?> golem) {
        Level level = golem.level();
        // 校验 L2Hostility 的实体类型过滤
        if (!LHMiscs.MOB.type().isProper(golem)) return;

        MobTraitCap cap = LHMiscs.MOB.type().getOrCreate(golem);
        if (!cap.isInitialized()) {
            // 获取生成位置所在的区块区域难度 (RegionalDifficultyModifier)
            ChunkDifficulty.at(level, golem.blockPosition()).ifPresent(chunkHolder -> {
                // 执行初始化（计算周围玩家等级、区域难度、分发词条和属性缩放）
                cap.init(level, golem, chunkHolder);
            });
        }
    }
}