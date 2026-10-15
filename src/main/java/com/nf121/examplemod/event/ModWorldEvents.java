package com.nf121.examplemod.event;

import com.nf121.examplemod.ExampleMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public class ModWorldEvents {

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        // 使用 instanceof 模式匹配直接转为 ServerLevel，一步到位排除客户端与 null 隐患
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // 确保只在主世界加载时触发一次，避免下界/末地重复执行
            if (serverLevel.dimension() == ServerLevel.OVERWORLD) {
                // ServerLevel 提供了直接获取 GameRules 的方法，无需调用 getServer()
                GameRules gameRules = serverLevel.getGameRules();

                var rule = gameRules.getRule(GameRules.RULE_KEEPINVENTORY);
                if (!rule.get()) {
                    // 修改规则，并传入 serverLevel.getServer() 用于触发网路同步与通知
                    rule.set(true, serverLevel.getServer());
                }
            }
        }
    }
}