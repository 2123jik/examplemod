package com.example.examplemod.loot;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class DiabloQualityRoller {

    // 1. 直接复用你定义的 Tag，并封装成枚举方便映射
    public enum EquipmentQuality {
        UNIQUE(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "quality/unique"))),
        SET   (TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "quality/set"))),
        RARE  (TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "quality/rare"))),
        MAGIC (TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "quality/magic"))),
        WHITE (TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("examplemod", "base/all")));

        private final TagKey<Item> tag;

        EquipmentQuality(TagKey<Item> tag) {
            this.tag = tag;
        }

        public TagKey<Item> getTag() {
            return tag;
        }
    }

    // 2. 解耦后的掉落上下文（各模组数据统一汇总到这里）
    public record DropContext(
            int mobLevel,          // 莱特兰恶意：怪物等级（未安装时可设为默认或按维度估算）
            int playerDifficulty,  // 莱特兰恶意：玩家难度等级
            int modifierCount,     // 怪物身上有效的属性修饰符条数
            double luck,           // 玩家总幸运值 (Attributes.LUCK + 特殊Curios加成)
            double worldFactor     // 当前World乘区 (维度 + 神化WorldTier + 难度)
    ) {}

    // 3. 级联掷骰：按 Unique -> Set -> Rare -> Magic -> White 顺序判定
    public static EquipmentQuality rollQuality(DropContext ctx, RandomSource random) {
        // 将幸运值换算成百分比 MF（例如 1 点 Luck = 40 MF，负幸运按 0 或惩罚处理）
        double mfPercent = Math.max(0.0, ctx.luck() * 40.0);
        // 如果存在负幸运，给一个额外的惩罚系数
        double badLuckPenalty = ctx.luck() < 0 ? Math.max(0.2, 1.0 + ctx.luck() * 0.1) : 1.0;

        // 综合环境乘区 = 等级差修正 * 修饰符数量修正 * 世界修正 * Boss修正 * 厄运惩罚
        double envMultiplier = calcLevelDeltaFactor(ctx.mobLevel(), ctx.playerDifficulty())
                * calcModifierFactor(ctx.modifierCount())
                * ctx.worldFactor()
                * badLuckPenalty;

        // --- ① 判定 暗金 (Unique) ---
        if (ctx.mobLevel() >= 15) { // 最低等级门槛，可放进Config
            double baseUnique = 0.004; // 基础 0.4%
            double effMf = effectiveMf(mfPercent, 250.0);
            double uniqueChance = clampChance(baseUnique * (1.0 + effMf / 100.0) * envMultiplier, 0.35);
            if (random.nextDouble() < uniqueChance) {
                return EquipmentQuality.UNIQUE;
            }
        }

        // --- ② 判定 套装 (Set) ---
        if (ctx.mobLevel() >= 8) {
            double baseSet = 0.012; // 基础 1.2%
            double effMf = effectiveMf(mfPercent, 500.0);
            double setChance = clampChance(baseSet * (1.0 + effMf / 100.0) * envMultiplier, 0.45);
            if (random.nextDouble() < setChance) {
                return EquipmentQuality.SET;
            }
        }

        // --- ③ 判定 亮金 (Rare) ---
        double baseRare = 0.05; // 基础 5%
        double effMfRare = effectiveMf(mfPercent, 600.0);
        double rareChance = clampChance(baseRare * (1.0 + effMfRare / 100.0) * envMultiplier, 0.65);
        if (random.nextDouble() < rareChance) {
            return EquipmentQuality.RARE;
        }

        // --- ④ 判定 魔法 (Magic) ---
        double baseMagic = 0.18; // 基础 18%（Magic不走递减，直接吃满MF）
        double magicChance = clampChance(baseMagic * (1.0 + mfPercent / 100.0) * envMultiplier, 0.85);
        if (random.nextDouble() < magicChance) {
            return EquipmentQuality.MAGIC;
        }

        // --- ⑤ 保底 白装 (White / Base) ---
        return EquipmentQuality.WHITE;
    }

    // D2 经典 MF 递减公式: (MF * factor) / (MF + factor)
    private static double effectiveMf(double mf, double factor) {
        if (mf <= 0) return 0;
        return (mf * factor) / (mf + factor);
    }

    // 怪物等级与玩家难度的差距修正
    private static double calcLevelDeltaFactor(int mobLevel, int playerDiff) {
        int delta = mobLevel - playerDiff;
        if (delta >= 0) {
            // 越级打怪：每级 +2%，最多 +80%
            return Math.min(1.8, 1.0 + delta * 0.02);
        } else if (delta >= -10) {
            // 差距在10级以内，不惩罚
            return 1.0;
        } else {
            // 虐菜惩罚：超过10级后每级降低 4%，最低保底 0.15x
            return Math.max(0.15, 1.0 + (delta + 10) * 0.04);
        }
    }

    // 怪物属性修饰符条数修正（非线性防膨胀）
    private static double calcModifierFactor(int modifierCount) {
        if (modifierCount <= 0) return 1.0;
        return Math.min(2.5, 1.0 + 0.18 * Math.sqrt(modifierCount));
    }

    private static double clampChance(double chance, double maxCap) {
        return Math.max(0.0, Math.min(maxCap, chance));
    }
}