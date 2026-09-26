package com.example.examplemod.util;

import dev.xkmc.l2artifacts.content.config.StatType;
import dev.xkmc.l2artifacts.content.core.StatEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class ArtifactStatFormatter {

    public static Component format(Component originalText, StatEntry entry, int level, boolean isMain) {
        if (originalText == null) return null;

        StatType type = (StatType) entry.type().value();
        double baseVal = type.getBaseValue();
        // entry.value() 返回的是乘了 base 的数值，除以 baseVal 得到真实的倍率 (multiplier)
        double multiplier = baseVal > 0 ? (entry.value() / baseVal) : entry.value();

        double percent;

        if (isMain) {
            // 主词条理论最大倍率：初始 base_high + 当前等级 * main_high
            double maxTheoryMultiplier = type.base_high() + level * type.main_high();
            if (maxTheoryMultiplier <= 0) return originalText;

            percent = (multiplier / maxTheoryMultiplier) * 100.0;
        } else {
            // 副词条单次最大倍率为 base_high
            double singleMax = type.base_high();
            if (singleMax <= 0) return originalText;

            // 向上取整精准推算强化命中次数
            int rolls = Math.max(1, (int) Math.ceil((multiplier - 1e-5) / singleMax));
            double maxTheoryMultiplier = rolls * singleMax;

            percent = (multiplier / maxTheoryMultiplier) * 100.0;
        }

        percent = Math.min(100.0, Math.max(0.0, percent));

        // 颜色评级
        ChatFormatting color;
        boolean isPerfect = false;

        if (percent >= 99.95) {
            color = ChatFormatting.GOLD;
            isPerfect = true;
        } else if (percent >= 90.0) {
            color = ChatFormatting.YELLOW;
        } else if (percent >= 75.0) {
            color = ChatFormatting.GREEN;
        } else if (percent >= 50.0) {
            color = ChatFormatting.WHITE;
        } else {
            color = ChatFormatting.GRAY;
        }

        MutableComponent result = Component.empty();

        // 满 Roll 在最开头添加金色星星
        if (isPerfect) {
            result.append(Component.literal("★ ").withStyle(ChatFormatting.GOLD));
        }

        result.append(originalText);
        result.append(Component.literal(String.format(" (%.0f%%)", percent)).withStyle(Style.EMPTY.withColor(color)));

        return result;
    }
}