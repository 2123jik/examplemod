package com.example.examplemod.entity;

import icyllis.modernui.text.SpannableStringBuilder;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.text.style.ForegroundColorSpan;
import icyllis.modernui.text.style.StyleSpan;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;

public final class ModernTooltipHelper {
    private ModernTooltipHelper() {}

    /**
     * 将 Minecraft 原生 Component 列表转化为带颜色的 Modern UI 富文本
     */
    public static CharSequence buildModernTooltip(List<Component> lines) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            appendComponent(builder, line);
            if (i < lines.size() - 1) {
                builder.append("\n");
            }
        }
        return builder;
    }

    private static void appendComponent(SpannableStringBuilder builder, Component component) {
        component.visit((style, text) -> {
            if (text.isEmpty()) return java.util.Optional.empty();

            int start = builder.length();
            builder.append(text);
            int end = builder.length();

            // 1. 颜色提取
            TextColor textColor = style.getColor();
            int color = textColor != null ? (textColor.getValue() | 0xFF000000) : 0xFFAAAAAA;
            builder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // 2. 粗体 / 斜体
            if (style.isBold() && style.isItalic()) {
                builder.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (style.isBold()) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (style.isItalic()) {
                builder.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            return java.util.Optional.empty();
        }, Style.EMPTY);
    }
}