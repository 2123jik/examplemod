package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.particle.SpaceSlashParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public class ModCombatEvents {

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        // 当暴击判定成功时（只在客户端生成这道空间斩视觉效果）
        if (event.isVanillaCritical() && event.getEntity().level().isClientSide()) {
            Player player = event.getEntity();
            if (player.level() instanceof ClientLevel clientLevel && event.getTarget() != null) {
                // 空间斩降临：只斩 1 刀，纯着色器渲染，不带任何杂散粒子
//                SpaceSlashParticle.spawnSpaceSlash(clientLevel, event.getTarget());
            }
        }
    }
}