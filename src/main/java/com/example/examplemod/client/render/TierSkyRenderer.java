package com.example.examplemod.client.render;

import com.example.examplemod.client.environment.ClientEnvironmentState;
import com.example.examplemod.client.environment.EnvironmentProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class TierSkyRenderer {

    /*
     * 球体半径只影响几何位置，不影响视觉尺寸。
     * 保持在远裁剪面以内即可。
     */
    private static final float SKY_RADIUS = 100.0F;

    /*
     * 经度段数和纬度段数。
     * 64 × 24 只生成 1536 个四边形，对天空渲染开销很低。
     */
    private static final int LONGITUDE_SEGMENTS = 64;
    private static final int LATITUDE_SEGMENTS = 24;

    /*
     * 控制地平线颜色向顶部颜色过渡的形状。
     * 数值越大，地平线色带越窄。
     */
    private static final float HORIZON_FALLOFF = 1.65F;

    private TierSkyRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (ExampleShaders.tierSky == null) {
            return;
        }

        float partialTick =
            event.getPartialTick().getGameTimeDeltaPartialTick(false);

        EnvironmentProfile profile =
            ClientEnvironmentState.getProfile(partialTick);

        PoseStack poseStack = event.getPoseStack();

        /*
         * 直接以相机为原点提交球体顶点。
         *
         * RenderLevelStageEvent 的 PoseStack 已处于世界渲染使用的相机空间，
         * 因此这里不再用相机世界坐标构造顶点，也不再执行
         * translate(-cameraX, -cameraY, -cameraZ)。
         *
         * 这样还能避免玩家位于高坐标时的浮点抖动。
         */
        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        /*
         * 相机位于球体内部，因此必须关闭背面剔除。
         */
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(() -> ExampleShaders.tierSky);

        if (ExampleShaders.tierSky.getUniform("TierTime") != null) {
            float tierTime =
                (minecraft.level.getGameTime() + partialTick) / 20.0F;

            ExampleShaders.tierSky
                .getUniform("TierTime")
                .set(tierTime);
        }

        if (ExampleShaders.tierSky.getUniform("PulseStrength") != null) {
            ExampleShaders.tierSky
                .getUniform("PulseStrength")
                .set(profile.pulseStrength());
        }

        BufferBuilder builder = Tesselator.getInstance().begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR
        );

        /*
         * 纬度从 -90° 到 +90°：
         *
         * -90°：脚下
         *   0°：地平线
         * +90°：头顶
         *
         * 整个表面连续，不再存在单独的水平顶盖。
         */
        for (
            int latitudeIndex = 0;
            latitudeIndex < LATITUDE_SEGMENTS;
            latitudeIndex++
        ) {
            float latitudeProgress0 =
                latitudeIndex / (float) LATITUDE_SEGMENTS;

            float latitudeProgress1 =
                (latitudeIndex + 1) / (float) LATITUDE_SEGMENTS;

            double latitude0 =
                -Math.PI * 0.5 + Math.PI * latitudeProgress0;

            double latitude1 =
                -Math.PI * 0.5 + Math.PI * latitudeProgress1;

            float cosLatitude0 = (float) Math.cos(latitude0);
            float sinLatitude0 = (float) Math.sin(latitude0);

            float cosLatitude1 = (float) Math.cos(latitude1);
            float sinLatitude1 = (float) Math.sin(latitude1);

            SkyVertexColor color0 =
                calculateColor(profile, sinLatitude0);

            SkyVertexColor color1 =
                calculateColor(profile, sinLatitude1);

            for (
                int longitudeIndex = 0;
                longitudeIndex < LONGITUDE_SEGMENTS;
                longitudeIndex++
            ) {
                double longitude0 =
                    Math.PI
                        * 2.0
                        * longitudeIndex
                        / LONGITUDE_SEGMENTS;

                double longitude1 =
                    Math.PI
                        * 2.0
                        * (longitudeIndex + 1)
                        / LONGITUDE_SEGMENTS;

                float cosLongitude0 = (float) Math.cos(longitude0);
                float sinLongitude0 = (float) Math.sin(longitude0);

                float cosLongitude1 = (float) Math.cos(longitude1);
                float sinLongitude1 = (float) Math.sin(longitude1);

                float x00 =
                    cosLongitude0
                        * cosLatitude0
                        * SKY_RADIUS;

                float y00 =
                    sinLatitude0
                        * SKY_RADIUS;

                float z00 =
                    sinLongitude0
                        * cosLatitude0
                        * SKY_RADIUS;

                float x01 =
                    cosLongitude1
                        * cosLatitude0
                        * SKY_RADIUS;

                float y01 = y00;

                float z01 =
                    sinLongitude1
                        * cosLatitude0
                        * SKY_RADIUS;

                float x11 =
                    cosLongitude1
                        * cosLatitude1
                        * SKY_RADIUS;

                float y11 =
                    sinLatitude1
                        * SKY_RADIUS;

                float z11 =
                    sinLongitude1
                        * cosLatitude1
                        * SKY_RADIUS;

                float x10 =
                    cosLongitude0
                        * cosLatitude1
                        * SKY_RADIUS;

                float y10 = y11;

                float z10 =
                    sinLongitude0
                        * cosLatitude1
                        * SKY_RADIUS;

                addVertex(
                    builder,
                    matrix,
                    x00,
                    y00,
                    z00,
                    color0
                );

                addVertex(
                    builder,
                    matrix,
                    x01,
                    y01,
                    z01,
                    color0
                );

                addVertex(
                    builder,
                    matrix,
                    x11,
                    y11,
                    z11,
                    color1
                );

                addVertex(
                    builder,
                    matrix,
                    x10,
                    y10,
                    z10,
                    color1
                );
            }
        }

        MeshData meshData = builder.build();

        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static SkyVertexColor calculateColor(
        EnvironmentProfile profile,
        float normalizedHeight
    ) {
        /*
         * normalizedHeight：
         *
         * -1 = 脚下
         *  0 = 地平线
         *  1 = 头顶
         */

        float upwardHeight =
            Mth.clamp(normalizedHeight, 0.0F, 1.0F);

        float topBlend =
            (float) Math.pow(
                upwardHeight,
                HORIZON_FALLOFF
            );

        /*
         * 地平线到头顶连续插值。
         */
        float red = Mth.lerp(
            topBlend,
            profile.skyHorizonR(),
            profile.skyTopR()
        );

        float green = Mth.lerp(
            topBlend,
            profile.skyHorizonG(),
            profile.skyTopG()
        );

        float blue = Mth.lerp(
            topBlend,
            profile.skyHorizonB(),
            profile.skyTopB()
        );

        /*
         * 地平线以下逐渐向雾色靠拢。
         * 使用连续 smoothstep，不产生新的水平接缝。
         */
        float belowHorizon =
            Mth.clamp(-normalizedHeight, 0.0F, 1.0F);

        float fogBlend = smoothStep(belowHorizon);

        red = Mth.lerp(
            fogBlend,
            red,
            profile.fogR()
        );

        green = Mth.lerp(
            fogBlend,
            green,
            profile.fogG()
        );

        blue = Mth.lerp(
            fogBlend,
            blue,
            profile.fogB()
        );

        /*
         * Alpha 在整个球面连续变化。
         * 地平线效果略强，头顶和脚下略弱。
         */
        float horizonWeight =
            1.0F - Math.abs(normalizedHeight);

        horizonWeight = smoothStep(
            Mth.clamp(horizonWeight, 0.0F, 1.0F)
        );

        float alphaMultiplier =
            Mth.lerp(
                horizonWeight,
                0.62F,
                1.0F
            );

        float alpha = Mth.clamp(
            profile.skyOpacity() * alphaMultiplier,
            0.0F,
            0.82F
        );

        return new SkyVertexColor(
            red,
            green,
            blue,
            alpha
        );
    }

    private static float smoothStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static void addVertex(
        BufferBuilder builder,
        Matrix4f matrix,
        float x,
        float y,
        float z,
        SkyVertexColor color
    ) {
        builder.addVertex(matrix, x, y, z)
            .setColor(
                color.red(),
                color.green(),
                color.blue(),
                color.alpha()
            );
    }

    private record SkyVertexColor(
        float red,
        float green,
        float blue,
        float alpha
    ) {
    }
}
