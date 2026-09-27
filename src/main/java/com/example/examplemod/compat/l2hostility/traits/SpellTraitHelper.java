package com.example.examplemod.compat.l2hostility.traits;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import org.joml.Vector3f;

public class SpellTraitHelper {

    /**
     * 1. 动态颜色计算：
     * 从 SchoolType 的 targetingColor 向量转换提取 0xRRGGBB。
     * 完美兼容未来所有附属模组自定义的新学派。
     */
    public static int getDynamicColor(AbstractSpell spell) {
        if (spell == null) return 0xFFFFFF;
        SchoolType school = spell.getSchoolType();
        if (school == null) return 0xFFFFFF;

        Vector3f colorVec = school.getTargetingColor();
        if (colorVec == null) return 0xFFFFFF;

        int r = Math.clamp((int) (colorVec.x() * 255.0F), 0, 255);
        int g = Math.clamp((int) (colorVec.y() * 255.0F), 0, 255);
        int b = Math.clamp((int) (colorVec.z() * 255.0F), 0, 255);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * 2. 等级自适应算法：
     * 保证词条等级（不管是 1~5 还是 1~10）与法术自身的 [minLevel, maxLevel] 线性对齐。
     */
    public static int scaleSpellLevel(int traitLevel, int maxTraitLevel, AbstractSpell spell) {
        int minSpellLv = spell.getMinLevel();
        int maxSpellLv = spell.getMaxLevel();
        if (minSpellLv >= maxSpellLv) {
            return minSpellLv;
        }

        int maxT = Math.max(1, maxTraitLevel);
        float progress = (float) (traitLevel - 1) / (float) Math.max(1, maxT - 1);
        int result = Math.round(minSpellLv + progress * (maxSpellLv - minSpellLv));
        return Math.clamp(result, minSpellLv, maxSpellLv);
    }

    /**
     * 3. 施法周期计算：
     * 基于法术自身的 Cooldown 动态配置，并约束在合理防卡顿区间 [40 ticks, 200 ticks]。
     */
    public static int getSafeCooldown(AbstractSpell spell) {
        int cd = spell.getSpellCooldown();
        return cd <= 0 ? 60 : Math.clamp(cd, 40, 200);
    }
}