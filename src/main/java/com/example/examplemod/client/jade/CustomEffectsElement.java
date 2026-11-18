package com.example.examplemod.client.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

import java.util.Comparator;
import java.util.List;

public class CustomEffectsElement extends Element {

    private static final int ICON_SIZE = 18;
    private static final int CELL_W = 24;      // 单个卡片宽
    private static final int CELL_H = 30;      // 单个卡片高
    private static final int MAX_PER_ROW = 6;  // 一行最多 6 个

    private static final int COL_BG = 0xCC101010;
    private static final int COL_EFFECT_GOOD = 0xFF64CB85;
    private static final int COL_EFFECT_BAD = 0xFFE07070;

    private final List<MobEffectInstance> effects;
    private final int totalRows;
    private final int totalCols;

    public CustomEffectsElement(List<MobEffectInstance> effects) {
        this.effects = effects.stream()
                .sorted(Comparator
                        .comparingInt((MobEffectInstance inst) -> inst.getEffect().value().isBeneficial() ? 0 : 1)
                        .thenComparing(inst -> inst.getEffect().value().getDescriptionId()))
                .toList();

        int count = this.effects.size();
        this.totalCols = Math.min(count, MAX_PER_ROW);
        this.totalRows = (count + MAX_PER_ROW - 1) / MAX_PER_ROW;
    }

    @Override
    public Vec2 getSize() {
        if (effects.isEmpty()) {
            return Vec2.ZERO;
        }
        return new Vec2(totalCols * CELL_W, totalRows * CELL_H);
    }

    @Override
    public void render(GuiGraphics g, float x, float y, float maxX, float maxY) {
        if (effects.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);

        for (int i = 0; i < effects.size(); i++) {
            int col = i % MAX_PER_ROW;
            int row = i / MAX_PER_ROW;
            int cellX = col * CELL_W;
            int cellY = row * CELL_H;

            renderSingleEffect(g, mc, effects.get(i), cellX, cellY);
        }

        g.pose().popPose();
    }

    private void renderSingleEffect(GuiGraphics g, Minecraft mc, MobEffectInstance inst, int x, int y) {
        int borderColor = inst.getEffect().value().isBeneficial() ? COL_EFFECT_GOOD : COL_EFFECT_BAD;

        // 边框与背景
        g.fill(x, y, x + 22, y + 22, borderColor);
        g.fill(x + 1, y + 1, x + 21, y + 21, COL_BG);
        g.flush();

        // 药水图标
        TextureAtlasSprite sprite = mc.getMobEffectTextures().get(inst.getEffect());
        g.blit(x + 2, y + 2, 0, ICON_SIZE, ICON_SIZE, sprite);
        g.flush();

        // 等级数字
        if (inst.getAmplifier() > 0) {
            String amp = String.valueOf(inst.getAmplifier() + 1);
            g.drawString(mc.font, amp, x + 21 - mc.font.width(amp), y + 12, 0xFFFFFFFF, true);
        }

        // 剩余时间
        String duration = formatDuration(inst);
        int textColor = !inst.isInfiniteDuration() && inst.getDuration() <= 200 ? 0xFFFF7777 : 0xFFFFFFFF;
        g.drawString(mc.font, duration, x + 11 - mc.font.width(duration) / 2, y + 23, textColor, true);

        g.flush();
    }

    private static String formatDuration(MobEffectInstance inst) {
        if (inst.isInfiniteDuration()) return "∞";
        long seconds = (Math.max(0L, inst.getDuration()) + 19L) / 20L;
        if (seconds < 60L) return seconds + "s";
        if (seconds < 3600L) return ((seconds + 59L) / 60L) + "m";
        return ((seconds + 3599L) / 3600L) + "h";
    }
}