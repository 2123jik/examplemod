package com.example.examplemod.loot;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.xkmc.l2hostility.content.logic.DifficultyLevel;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.List;

public class DropContextBuilder {

    private static final List<Holder<Attribute>> TRACKED_ATTRIBUTES = List.of(
            Attributes.MAX_HEALTH,
            Attributes.ATTACK_DAMAGE,
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.KNOCKBACK_RESISTANCE
    );

    /**
     * 构建掉落上下文，全面做 null 安全校验
     * @param lootLuck 战利品表原本携带的 luck（箱子开出时可用）
     */
    public static DiabloQualityRoller.DropContext build(ServerLevel level, LivingEntity mob, ServerPlayer player, float lootLuck) {
        // 1. 幸运值：玩家在场取属性（吃满Curios/神化），否则取 LootContext 传入的 luck
        double luck = player != null ? player.getAttributeValue(Attributes.LUCK) : lootLuck;

        // 2. 怪物等级与玩家难度（莱特兰恶意兼容）
        int mobLevel = 0;
        int playerDiff = 0;
        if (ModList.get().isLoaded("l2hostility")) {
            if (mob != null) mobLevel = DifficultyLevel.ofAny(mob);
            if (player != null) playerDiff = DifficultyLevel.ofAny(player);
        }

        // 3. 统计词缀数（仅对生物生效）
        int modifierCount = (mob != null) ? countCombatModifiers(mob) : 0;

        // 4. 计算世界乘区（维度 + 神化 WorldTier）
        double worldFactor = calcWorldFactor(level, player);

        return new DiabloQualityRoller.DropContext(mobLevel, playerDiff, modifierCount, luck, worldFactor);
    }

    private static int countCombatModifiers(LivingEntity mob) {
        int count = 0;
        for (Holder<Attribute> attr : TRACKED_ATTRIBUTES) {
            AttributeInstance instance = mob.getAttribute(attr);
            if (instance == null) continue;
            for (var modifier : instance.getModifiers()) {
                if (!modifier.id().getNamespace().equals("minecraft")) {
                    count++;
                }
            }
        }
        return count;
    }

    private static double calcWorldFactor(ServerLevel level, ServerPlayer player) {
        double factor = 1.0;
        if (level.dimension() == Level.NETHER) {
            factor *= 1.35;
        } else if (level.dimension() == Level.END) {
            factor *= 1.65;
        }

        // 加上神化 1.21+ 的 WorldTier 系数（Haven -> Pinnacle）
        if (ModList.get().isLoaded("apotheosis") && player != null) {
            factor *= (1.0 + 0.1 * WorldTier.getTier(player).ordinal());
        }
        return factor;
    }
}