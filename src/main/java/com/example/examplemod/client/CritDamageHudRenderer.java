package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ArrowCritPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

// Modern UI 缓动插值
import icyllis.modernui.animation.TimeInterpolator;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class CritDamageHudRenderer {

    private static ArrowCritPayload activeCrit = null;
    private static long startTime = 0;
    private static final long TOTAL_DURATION_MS = 1000; // 动画持续 1 秒

    // ================= 颜色渐变控制轴 (可根据需求随时增减) =================
    // 倍率锚点：低于 1.0x 对应浅鹅黄，1.5x 达到原版常规满暴烈焰橙，2.0x+ 突破极限到达炽热神话紫
    private static final float[] MULT_STOPS = { 1.00f,    1.25f,    1.50f,    2.00f };
    private static final int[]   COLOR_STOPS = {
            0xFFF9C4, // 1.00x: 柔和浅鹅黄 (起步暴击)
            0xFFD54F, // 1.25x: 明亮黄金色 (中等暴击)
            0xFF5722, // 1.50x: 烈焰橙红色 (原版常规满暴)
            0xE040FB  // 2.00x+: 突破极限的炽热神话紫 (超限大暴击)
    };

    public static void triggerCrit(ArrowCritPayload payload) {
        activeCrit = payload;
        startTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (activeCrit == null) return;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > TOTAL_DURATION_MS) {
            activeCrit = null;
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        float t = (float) elapsed / TOTAL_DURATION_MS;

        // ================= 1. 动效缓动插值 =================
        float baseScale;
        float alpha;
        float yOffset;

        // [0.0 ~ 0.25]: 爆发弹出 (Overshoot 回弹)
        if (t < 0.25f) {
            float inProgress = t / 0.25f;
            baseScale = 1.35f * TimeInterpolator.OVERSHOOT.getInterpolation(inProgress);
            alpha = inProgress;
            yOffset = -10.0f * inProgress;
        }
        // [0.25 ~ 0.70]: 悬浮展示
        else if (t < 0.70f) {
            baseScale = 1.35f;
            alpha = 1.0f;
            float stayProgress = (t - 0.25f) / 0.45f;
            yOffset = -10.0f - (stayProgress * 6.0f);
        }
        // [0.70 ~ 1.0]: 减速淡出上浮
        else {
            float outProgress = (t - 0.70f) / 0.30f;
            baseScale = 1.35f - (outProgress * 0.2f);
            alpha = 1.0f - TimeInterpolator.DECELERATE.getInterpolation(outProgress);
            yOffset = -16.0f - (outProgress * 12.0f);
        }

        int alpha255 = (int) (Mth.clamp(alpha, 0.0f, 1.0f) * 255);
        if (alpha255 <= 4) return;

        // ================= 2. 文字与动态颜色计算 =================
        float multiplier = activeCrit.multiplier();

        // 纯文字：仅倍率，去除星星和加号后缀
        String text = String.format("x%.2f", multiplier);

        // 连续渐变色：根据倍率在渐变轴上平滑插值 RGB
        int textRgb = getContinuousCritColor(multiplier);
        int finalColor = (alpha255 << 24) | textRgb;

        // 文字中心点计算
        float textHalfWidth = font.width(text) / 2.0f;
        float textHalfHeight = font.lineHeight / 2.0f;

        // 准星上方基准坐标
        float baseCenterX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f - 18.0f + yOffset;


        renderCritText(graphics, font, baseCenterX - 25f, centerY-25F, baseScale, text, textHalfWidth, textHalfHeight, finalColor);

    }

    /**
     * 纯文字暴击渲染：文字中心为锚点缩放，带原生清晰立体阴影
     */
    private static void renderCritText(GuiGraphics graphics, Font font, float x, float y,
                                       float scale, String text, float halfW, float halfH, int color) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0f);
        pose.scale(scale, scale, 1.0f);

        // dropShadow = true 开启原生文字描边阴影，复杂背景下不糊
        graphics.drawString(font, text, -halfW, -halfH, color, true);

        pose.popPose();
    }

    /**
     * 核心算法：多段连续颜色插值 (RGB Lerp)
     * 根据 multiplier 在 MULT_STOPS 轴上的位置，无缝过渡颜色
     */
    private static int getContinuousCritColor(float multiplier) {
        // 低于下限直接取最低色
        if (multiplier <= MULT_STOPS[0]) {
            return COLOR_STOPS[0];
        }
        // 高于最高阈值直接取最高神话色
        int maxIndex = MULT_STOPS.length - 1;
        if (multiplier >= MULT_STOPS[maxIndex]) {
            return COLOR_STOPS[maxIndex];
        }

        // 寻找当前倍率处于哪两个锚点区间之间
        for (int i = 0; i < maxIndex; i++) {
            float startMult = MULT_STOPS[i];
            float endMult = MULT_STOPS[i + 1];

            if (multiplier >= startMult && multiplier <= endMult) {
                // 计算当前区间的百分比 [0.0 ~ 1.0]
                float ratio = (multiplier - startMult) / (endMult - startMult);
                return lerpRgb(COLOR_STOPS[i], COLOR_STOPS[i + 1], ratio);
            }
        }

        return COLOR_STOPS[0];
    }

    /**
     * 对两个 RGB 颜色进行平滑线性插值
     */
    private static int lerpRgb(int colorA, int colorB, float ratio) {
        ratio = Mth.clamp(ratio, 0.0f, 1.0f);

        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;

        int r = (int) (rA + ratio * (rB - rA));
        int g = (int) (gA + ratio * (gB - gA));
        int b = (int) (bA + ratio * (bB - bA));

        return (r << 16) | (g << 8) | b;
    }
}