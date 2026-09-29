package com.example.examplemod.client;

import icyllis.modernui.mc.ExtendedGuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

public class StatusHudRenderer {

    // 基础尺寸常量
    private static final int START_X = 12;
    private static final int START_Y = 12;
    private static final int BAR_WIDTH = 110;
    private static final int BAR_HEIGHT = 10;
    private static final int BAR_SPACING = 14;
    private static final float CORNER_RADIUS = 3.5f;

    public static void render(GuiGraphics gr, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // 仅在非观战模式且非隐藏 HUD 时渲染
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }

        ExtendedGuiGraphics exGr = new ExtendedGuiGraphics(gr);
        Font font = mc.font;

        int currentY = START_Y;

        // ================= 1. 渲染生命值 (Health) =================
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorptionAmount(); // 吸收伤害（黄心）
        float totalHealthRatio = Mth.clamp(health / maxHealth, 0.0f, 1.0f);

        // 绘制血条暗底
        drawBarBackground(exGr, START_X, currentY, BAR_WIDTH, BAR_HEIGHT);

        // 绘制血条渐变填充 (深红 -> 亮红)
        int fillWidth = (int) (BAR_WIDTH * totalHealthRatio);
        if (fillWidth > 0) {
            exGr.setGradient(ExtendedGuiGraphics.Orientation.LEFT_RIGHT, 0xFFD32F2F, 0xFFFF5252);
            exGr.fillRoundRect(START_X, currentY, START_X + fillWidth, currentY + BAR_HEIGHT, CORNER_RADIUS);
        }

        // 如果有吸收伤害（金心），叠加一层金黄色细描边或指示条
        if (absorption > 0) {
            float absRatio = Mth.clamp(absorption / maxHealth, 0.0f, 1.0f);
            int absWidth = (int) (BAR_WIDTH * absRatio);
            exGr.setColor(0xFFFFD700);
            exGr.setStrokeWidth(1.2f);
            exGr.strokeRoundRect(START_X, currentY, START_X + absWidth, currentY + BAR_HEIGHT, CORNER_RADIUS);
        }

        // 边框描边
        drawBarBorder(exGr, START_X, currentY, BAR_WIDTH, BAR_HEIGHT);

        // 绘制血量文本 (例如: "20.0 / 20")
        String hpText = String.format("%.1f", health + absorption);
        gr.drawString(font, hpText, START_X + BAR_WIDTH + 6, currentY + 1, 0xFFFFFF, true);

        currentY += BAR_SPACING;

        // ================= 2. 渲染饱食度 & 饱和度 (Food & Saturation) =================
        // 创造模式下玩家不消耗饱食度，可根据需求选择是否显示
        FoodData foodData = player.getFoodData();
        int foodLevel = foodData.getFoodLevel();
        float saturation = foodData.getSaturationLevel();
        float foodRatio = Mth.clamp(foodLevel / 20.0f, 0.0f, 1.0f);

        // 绘制饱食度暗底
        drawBarBackground(exGr, START_X, currentY, BAR_WIDTH, BAR_HEIGHT);

        // 绘制饱食度条 (橙棕 -> 金橙)
        int foodFillWidth = (int) (BAR_WIDTH * foodRatio);
        if (foodFillWidth > 0) {
            exGr.setGradient(ExtendedGuiGraphics.Orientation.LEFT_RIGHT, 0xFFE65100, 0xFFFFB74D);
            exGr.fillRoundRect(START_X, currentY, START_X + foodFillWidth, currentY + BAR_HEIGHT, CORNER_RADIUS);
        }

        // 饱和度额外展示：在饱食度上方叠加细进度条（金色/浅黄）
        float satRatio = Mth.clamp(saturation / 20.0f, 0.0f, 1.0f);
        int satFillWidth = (int) (BAR_WIDTH * satRatio);
        if (satFillWidth > 0) {
            exGr.setColor(0xAAFFF176); // 半透明浅金
            exGr.fillRoundRect(START_X + 1, currentY + 1, START_X + satFillWidth - 1, currentY + 3, 1.0f);
        }

        // 边框描边
        drawBarBorder(exGr, START_X, currentY, BAR_WIDTH, BAR_HEIGHT);

        // 绘制饱食度文本 (例如: "20")
        String foodText = String.valueOf(foodLevel);
        gr.drawString(font, foodText, START_X + BAR_WIDTH + 6, currentY + 1, 0xFFE0E0E0, true);
    }

    /**
     * 绘制通用的半透明深色圆角矩形槽底
     */
    private static void drawBarBackground(ExtendedGuiGraphics exGr, int x, int y, int w, int h) {
        exGr.setColor(0x88000000); // 黑色半透明底色
        exGr.fillRoundRect(x, y, x + w, y + h, CORNER_RADIUS);
    }

    /**
     * 绘制通用的半透明白色轻微描边
     */
    private static void drawBarBorder(ExtendedGuiGraphics exGr, int x, int y, int w, int h) {
        exGr.setColor(0x33FFFFFF); // 弱高光描边
        exGr.setStrokeWidth(1.0f);
        exGr.strokeRoundRect(x, y, x + w, y + h, CORNER_RADIUS);
    }
}