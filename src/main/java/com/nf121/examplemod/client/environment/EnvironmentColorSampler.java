package com.nf121.examplemod.client.environment;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.Random;

/**
 * 实时采样玩家周围方块的特征颜色，动态拟合当前结构/环境的基调色与高光色
 */
public final class EnvironmentColorSampler {

    public static final EnvironmentColorSampler INSTANCE = new EnvironmentColorSampler();

    private static final int SAMPLES_PER_TICK = 12;
    private static final int HORIZONTAL_RADIUS = 14;
    private static final int VERTICAL_RADIUS = 7;

    private final Random random = new Random();

    // 平滑后的结构主基调颜色 (RGB 0.0 ~ 1.0)
    private float structureR = 0.5F;
    private float structureG = 0.5F;
    private float structureB = 0.5F;

    // 平滑后的结构发光/高光颜色
    private float accentR = 1.0F;
    private float accentG = 1.0F;
    private float accentB = 1.0F;

    // 当前环境结构特征强度（在虚空或大片空气中降低，在密集人造/自然结构中升高）
    private float structureInfluence = 0.0F;

    private EnvironmentColorSampler() {
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            reset();
            return;
        }

        Level level = minecraft.level;
        BlockPos playerPos = minecraft.player.blockPosition();

        float tickAccumR = 0.0F;
        float tickAccumG = 0.0F;
        float tickAccumB = 0.0F;
        float totalWeight = 0.0F;

        float bestLightWeight = 0.0F;
        float bestLightR = structureR;
        float bestLightG = structureG;
        float bestLightB = structureB;

        for (int i = 0; i < SAMPLES_PER_TICK; i++) {
            int dx = random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int dy = random.nextInt(VERTICAL_RADIUS * 2 + 1) - VERTICAL_RADIUS;
            int dz = random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;

            BlockPos samplePos = playerPos.offset(dx, dy, dz);
            BlockState state = level.getBlockState(samplePos);

            if (state.isAir() || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR)) {
                continue;
            }

            MapColor mapColor = state.getMapColor(level, samplePos);
            if (mapColor == MapColor.NONE) {
                continue;
            }

            int rgb = mapColor.col;
            float r = ((rgb >> 16) & 0xFF) / 255.0F;
            float g = ((rgb >> 8) & 0xFF) / 255.0F;
            float b = (rgb & 0xFF) / 255.0F;

            // 权重计算：发光方块、特殊方块赋予极高的特征比重
            int lightEmission = state.getLightEmission(level, samplePos);
            float weight = 1.0F + lightEmission * 0.8F;

            // 距离衰减（离玩家越近，对当前体感影响越重）
            double distSq = dx * dx + dy * dy + dz * dz;
            weight *= (float) (1.0 - (distSq / (HORIZONTAL_RADIUS * HORIZONTAL_RADIUS * 2.0)));
            weight = Math.max(weight, 0.1F);

            tickAccumR += r * weight;
            tickAccumG += g * weight;
            tickAccumB += b * weight;
            totalWeight += weight;

            if (lightEmission > 0 && weight > bestLightWeight) {
                bestLightWeight = weight;
                bestLightR = r;
                bestLightG = g;
                bestLightB = b;
            }
        }

        if (totalWeight > 0.0F) {
            float targetR = tickAccumR / totalWeight;
            float targetG = tickAccumG / totalWeight;
            float targetB = tickAccumB / totalWeight;

            // 指数滑动滤波平滑 (EMA)，平滑时间常数约 30~40 tick，防止突变
            float alpha = 0.05F;
            structureR = Mth.lerp(alpha, structureR, targetR);
            structureG = Mth.lerp(alpha, structureG, targetG);
            structureB = Mth.lerp(alpha, structureB, targetB);

            accentR = Mth.lerp(alpha * 1.5F, accentR, bestLightR);
            accentG = Mth.lerp(alpha * 1.5F, accentG, bestLightG);
            accentB = Mth.lerp(alpha * 1.5F, accentB, bestLightB);

            structureInfluence = Mth.lerp(0.04F, structureInfluence, 1.0F);
        } else {
            structureInfluence = Mth.lerp(0.04F, structureInfluence, 0.0F);
        }
    }

    public float getStructureR() { return structureR; }
    public float getStructureG() { return structureG; }
    public float getStructureB() { return structureB; }

    public float getAccentR() { return accentR; }
    public float getAccentG() { return accentG; }
    public float getAccentB() { return accentB; }

    public float getStructureInfluence() { return structureInfluence; }

    public void reset() {
        structureR = 0.5F;
        structureG = 0.5F;
        structureB = 0.5F;
        accentR = 1.0F;
        accentG = 1.0F;
        accentB = 1.0F;
        structureInfluence = 0.0F;
    }
}