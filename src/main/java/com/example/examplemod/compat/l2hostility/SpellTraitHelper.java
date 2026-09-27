package com.example.examplemod.compat.l2hostility;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class SpellTraitHelper {

    /** 学派色 → 0xRRGGBB，暗色自动提亮，兼容所有附属学派。 */
    public static int getDynamicColor(AbstractSpell spell) {
        if (spell == null) return 0xFFFFFF;
        SchoolType school = spell.getSchoolType();
        if (school == null) return 0xFFFFFF;

        Vector3f c = school.getTargetingColor();
        if (c == null) return 0xFFFFFF;

        int r = Mth.clamp((int) (c.x() * 255.0F), 0, 255);
        int g = Mth.clamp((int) (c.y() * 255.0F), 0, 255);
        int b = Mth.clamp((int) (c.z() * 255.0F), 0, 255);

        // 暗色提亮，保证在深色背景上可读
        int lum = (r * 299 + g * 587 + b * 114) / 1000;
        if (lum < 60) {
            r = Math.min(255, r + 80);
            g = Math.min(255, g + 80);
            b = Math.min(255, b + 80);
        }
        return (r << 16) | (g << 8) | b;
    }

    /** 词条等级线性映射到 [spellMin, spellMax]。 */
    public static int scaleSpellLevel(int traitLevel, int maxTraitLevel, AbstractSpell spell) {
        if (spell == null) return 1;
        int minS = spell.getMinLevel();
        int maxS = spell.getMaxLevel();
        if (minS >= maxS) return minS;

        int maxT = Math.max(1, maxTraitLevel);
        if (maxT == 1) return minS;

        float progress = (float) (traitLevel - 1) / (float) (maxT - 1);
        int result = Math.round(minS + progress * (maxS - minS));
        return Mth.clamp(result, minS, maxS);
    }

    /**
     * 冷却 = 法术原生冷却，但至少覆盖吟唱时长 + 缓冲。
     * 不设上限 —— 上限是对法术设计者平衡的破坏。
     */
    public static int getSafeCooldown(AbstractSpell spell) {
        if (spell == null) return 60;
        int nativeCd = spell.getSpellCooldown();
        int castTime = spell.getCastTime(spell.getMinLevel());
        int floor = castTime + 40;
        return Math.max(nativeCd, Math.max(floor, 20));
    }
}