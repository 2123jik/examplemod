package com.example.examplemod.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

public final class PinnacleAdaptiveHud {

    private PinnacleAdaptiveHud() {}

    // 核心几何常数 (严格贴合原版快捷栏 91px 边界)
    private static final int BAR_W  = 81;
    private static final int MAIN_H = 5;
    private static final int SUB_H  = 2;
    private static final int XP_W   = 60;
    private static final int XP_H   = 2;

    // ARGB8888 颜色常量
    private static final int COL_BG         = 0xCC101010;
    private static final int COL_BORDER     = 0xFF000000;
    private static final int COL_HEALTH_OK  = 0xFF2ECC71;
    private static final int COL_HEALTH_LOW = 0xFFE74C3C;
    private static final int COL_WITHER     = 0xFF2C3E50;
    private static final int COL_POISON     = 0xFF8E44AD;
    private static final int COL_FROZEN     = 0xFF3498DB;
    private static final int COL_ABSORB     = 0xFFFFD700;
    private static final int COL_ARMOR      = 0xFFBDC3C7;
    private static final int COL_FOOD       = 0xFFE67E22;
    private static final int COL_HUNGER     = 0xFF27AE60;
    private static final int COL_MOUNT      = 0xFFE91E63;
    private static final int COL_AIR        = 0xFF00BCD4;
    private static final int COL_EXP        = 0xFF55FF55;
    private static final int COL_JUMP       = 0xFFFFAA00;

    public static void render(GuiGraphics g, Player player) {
        Minecraft mc = Minecraft.getInstance();

        // =====================================================================
        // 门禁系统：杜绝一切未就绪、死亡、睡觉及创造模式下的幽灵提前渲染
        // =====================================================================
        // 1. 世界未就绪或未完成连接握手，坚决不渲染
        if (mc.level == null || mc.gameMode == null) return;

        // 2. 旁观者、或既不能受伤也没有经验的模式（纯创造飞天），直接 0 运算退出
        if (player.isSpectator() || (!mc.gameMode.canHurtPlayer() && !mc.gameMode.hasExperience())) {
            return;
        }

        // 3. 死亡中（避免在“你死了”界面下穿模）或睡觉中，坚决不渲染
        if (player.isDeadOrDying() || player.isSleeping()) return;

        // 4. 用户按 F1 隐藏界面时不渲染
        if (mc.options.hideGui) return;

        // =====================================================================
        // 坐标与缓冲区准备
        // =====================================================================
        int midX = g.guiWidth() >> 1;
        int baseY = g.guiHeight() - 29;

        int leftX = midX - 91;
        int rightX = midX + 91;

        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());

        Entity vehicle = player.getVehicle();
        boolean isSurvivalLike = mc.gameMode.canHurtPlayer();

        // =====================================================================
        // 1. 生存状态集群：生命、护甲、饱食、坐骑、氧气
        // =====================================================================
        if (isSurvivalLike) {
            // --- 左侧：生命值与护甲 ---
            float maxHp = player.getMaxHealth();
            if (maxHp > 0.0F) {
                float health = player.getHealth();
                float hpRatio = Mth.clamp(health / maxHp, 0.0F, 1.0F);
                int fillW = (int) (BAR_W * hpRatio);
                if (health > 0.0F && fillW == 0) fillW = 1;

                int hpCol;
                if (player.hasEffect(MobEffects.WITHER))       hpCol = COL_WITHER;
                else if (player.hasEffect(MobEffects.POISON))  hpCol = COL_POISON;
                else if (player.isFullyFrozen())               hpCol = COL_FROZEN;
                else hpCol = (hpRatio <= 0.25F) ? COL_HEALTH_LOW : COL_HEALTH_OK;

                // 主生命条 (左对齐)
                drawBar(vc, m, leftX, baseY, BAR_W, MAIN_H, fillW, hpCol, false);

                // 伤害吸收金心 (若存在，覆盖于生命条顶部 1px 细线)
                float abs = player.getAbsorptionAmount();
                if (abs > 0.0F) {
                    int absW = Math.min(BAR_W, (int) (BAR_W * (abs / maxHp)));
                    if (absW > 0) {
                        drawSolid(vc, m, leftX, baseY, leftX + absW, baseY + 1, COL_ABSORB);
                    }
                }

                // 护甲微条
                int armor = player.getArmorValue();
                if (armor > 0) {
                    int armorY = baseY - SUB_H - 2;
                    int armorW = (armor >= 20) ? BAR_W : Math.max(1, (armor * BAR_W) / 20);
                    drawBar(vc, m, leftX, armorY, BAR_W, SUB_H, armorW, COL_ARMOR, false);
                }
            }

            // --- 右侧：饱食度 / 坐骑生命 / 氧气 ---
            LivingEntity mount = (vehicle instanceof LivingEntity living && living.showVehicleHealth()) ? living : null;
            if (mount != null) {
                float mMax = mount.getMaxHealth();
                if (mMax > 0.0F) {
                    float mHp = mount.getHealth();
                    int mFillW = (int) (BAR_W * Mth.clamp(mHp / mMax, 0.0F, 1.0F));
                    if (mHp > 0.0F && mFillW == 0) mFillW = 1;
                    drawBar(vc, m, rightX - BAR_W, baseY, BAR_W, MAIN_H, mFillW, COL_MOUNT, true);
                }
            } else {
                int food = player.getFoodData().getFoodLevel();
                int foodCol = player.hasEffect(MobEffects.HUNGER) ? COL_HUNGER : COL_FOOD;
                int foodW = (food <= 0) ? 0 : (food >= 20 ? BAR_W : Math.max(1, (food * BAR_W) / 20));
                drawBar(vc, m, rightX - BAR_W, baseY, BAR_W, MAIN_H, foodW, foodCol, true);
            }

            // 缺氧微条（强制防御 maxAir <= 0 导致的除零异常）
            int maxAir = player.getMaxAirSupply();
            int air = player.getAirSupply();
            if (maxAir > 0 && (air < maxAir || player.isEyeInFluid(FluidTags.WATER))) {
                int airY = baseY - SUB_H - 2;
                int airW = (air <= 0) ? 0 : (air >= maxAir ? BAR_W : Math.max(1, (air * BAR_W) / maxAir));
                drawBar(vc, m, rightX - BAR_W, airY, BAR_W, SUB_H, airW, COL_AIR, true);
            }
        }

        // =====================================================================
        // 2. 中置集群：极简经验条 / 战马蓄力条（仅在允许获取经验的模式下绘制）
        // =====================================================================
        boolean hasExp = mc.gameMode.hasExperience();
        boolean isJumpCharging = vehicle instanceof PlayerRideableJumping jumper && jumper.canJump();

        if (hasExp || isJumpCharging) {
            float xpRatio = isJumpCharging ? mc.player.getJumpRidingScale() : player.experienceProgress;
            int xpColor = isJumpCharging ? COL_JUMP : COL_EXP;

            int xpStartX = midX - (XP_W >> 1);
            int xpY = baseY + MAIN_H + 2;
            int xpFillW = (int) (XP_W * Mth.clamp(xpRatio, 0.0F, 1.0F));

            drawBar(vc, m, xpStartX, xpY, XP_W, XP_H, xpFillW, xpColor, false);
        }

        // 统一完成所有无纹理几何图元的显卡管线提交
        g.bufferSource().endBatch(RenderType.gui());
    }

    /**
     * 标准工业级无接缝进度条绘制：
     * 1. 边框 1 个 Quad
     * 2. 底色 1 个 Quad
     * 3. 填充 1 个 Quad
     * 严格恒定 3 个 Quad (12 个顶点)，消灭虚假优化，杜绝非整数缩放下的 1 像素透明接缝
     */
    private static void drawBar(VertexConsumer vc, Matrix4f m, int x, int y, int w, int h, int fillW, int col, boolean fromRight) {
        // 1. 黑色外框
        drawSolid(vc, m, x - 1, y - 1, x + w + 1, y + h + 1, COL_BORDER);

        // 2. 全量底色
        drawSolid(vc, m, x, y, x + w, y + h, COL_BG);

        // 3. 进度填充
        if (fillW > 0) {
            int clampedW = Math.min(fillW, w);
            if (fromRight) {
                drawSolid(vc, m, x + w - clampedW, y, x + w, y + h, col);
            } else {
                drawSolid(vc, m, x, y, x + clampedW, y + h, col);
            }
        }
    }

    private static void drawSolid(VertexConsumer vc, Matrix4f m, int x1, int y1, int x2, int y2, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b = argb          & 0xFF;

        vc.addVertex(m, x1, y2, 0).setColor(r, g, b, a);
        vc.addVertex(m, x2, y2, 0).setColor(r, g, b, a);
        vc.addVertex(m, x2, y1, 0).setColor(r, g, b, a);
        vc.addVertex(m, x1, y1, 0).setColor(r, g, b, a);
    }
}