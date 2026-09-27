package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.Random;

// 使用 @EventBusSubscriber 自动注册到 NeoForge 游戏事件总线
@EventBusSubscriber(modid = ExampleMod.MODID)
public class AnimalKillRewardHandler {

    // 预定义的正面效果列表
    // 注意：在 1.21+ 中，MobEffects 字段返回的是 Holder<MobEffect>
    private static final List<Holder<MobEffect>> REWARD_EFFECTS = List.of(
            MobEffects.MOVEMENT_SPEED,    // 迅捷 (Speed)
            MobEffects.DAMAGE_BOOST,       // 力量 (Strength)
            MobEffects.DAMAGE_RESISTANCE,  // 抗性提升 (Resistance)
            MobEffects.ABSORPTION,         // 吸收 (Absorption)
            MobEffects.DIG_SPEED           // 急迫 (Haste)
    );

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        // 1. 仅在服务端处理逻辑，避免客户端产生幽灵效果或不同步
        if (victim.level().isClientSide()) {
            return;
        }

        // 2. 判断受害者类型
        // 方案 A：精确匹配指定的常见农场动物（猪、牛、羊、鸡）
        boolean isTargetAnimal = victim instanceof Pig 
                              || victim instanceof Cow 
                              || victim instanceof Sheep 
                              || victim instanceof Chicken;

        // 方案 B：如果你希望包含游戏中所有的动物（包括兔子、马等），可以直接用：
        // boolean isTargetAnimal = victim instanceof Animal;

        if (isTargetAnimal) {
            // 3. 检查直接/间接击杀者是否为玩家
            if (event.getSource().getEntity() instanceof Player player) {
                // 4. 从列表中随机选择一个 Effect
                Holder<MobEffect> chosenEffect = REWARD_EFFECTS.get(RANDOM.nextInt(REWARD_EFFECTS.size()));

                // 5. 设置持续时间和等级
                // 持续时间单位为游戏刻（20 ticks = 1秒），例如 20 秒为 20 * 20
                // amplifier: 0 代表 I 级，1 代表 II 级
                int duration = 20 * 20; 
                int amplifier = 0;

                // 给予玩家药水效果
                player.addEffect(new MobEffectInstance(chosenEffect, duration, amplifier));
            }
        }
    }
}