package com.example.examplemod.client.render;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.environment.ClientEnvironmentState;
import com.example.examplemod.client.environment.EnvironmentProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class TierSkyRenderer {

    /*
     * 天空以相机为中心。
     *
     * 半径不用于解决天空跟随视角旋转的问题，
     * 只需保证几何体位于适当的裁剪范围内。
     */
    private static final float SKY_RADIUS = 100.0F;

    private static final int LONGITUDE_SEGMENTS = 64;
    private static final int LATITUDE_SEGMENTS = 24;

    private static final float HORIZON_FALLOFF = 1.65F;

    private static final ResourceLocation ENVIRONMENT_NOISE =
            ResourceLocation.fromNamespaceAndPath(
                    ExampleMod.MOD_ID,
                    "textures/environment/environment_noise.png"
            );

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

        var shader = ExampleShaders.tierSky;

        if (shader == null) {
            return;
        }

        float partialTick =
                event.getPartialTick().getGameTimeDeltaPartialTick(false);

        EnvironmentProfile profile =
                ClientEnvironmentState.getProfile(partialTick);

        /*
         * 天空顶点首先定义在世界方向坐标系中：
         *
         * +Y 始终是世界上方。
         * X/Z 始终对应固定的世界水平方向。
         *
         * 渲染时保留世界到视图的旋转，去除平移。
         * 这样天空不会因为玩家移动产生视差，
         * 但玩家转头时会看到不同的天空方向。
         *
         * 必须复制矩阵，不能修改事件提供的原始矩阵。
         */
        Matrix4f skyViewMatrix =
                new Matrix4f(event.getModelViewMatrix());

        skyViewMatrix.m30(0.0F);
        skyViewMatrix.m31(0.0F);
        skyViewMatrix.m32(0.0F);

        /*
         * 顶点保持原始世界方向，视图变换交给 shader 的
         * ModelViewMat 执行。
         *
         * 不在 CPU 上用 skyViewMatrix 预先变换顶点，
         * 避免 shader 中的 Position 已经变成视图空间坐标。
         * 这也允许 shader 用 normalize(Position) 获取世界方向。
         */
        var modelViewStack = RenderSystem.getModelViewStack();

        modelViewStack.pushMatrix();

        try {
            modelViewStack.set(skyViewMatrix);
            RenderSystem.applyModelViewMatrix();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 从球体内部观看天空。
            RenderSystem.disableCull();

            // AFTER_SKY 阶段绘制背景，不写入世界深度。
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            RenderSystem.setShaderColor(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );

            RenderSystem.setShaderTexture(0, ENVIRONMENT_NOISE);
            RenderSystem.setShader(() -> shader);

            var timeUniform = shader.getUniform("TierTime");

            if (timeUniform != null) {
                float tierTime =
                        (minecraft.level.getGameTime() + partialTick) / 20.0F;

                timeUniform.set(tierTime);
            }

            var pulseUniform = shader.getUniform("PulseStrength");

            if (pulseUniform != null) {
                pulseUniform.set(profile.pulseStrength());
            }

            BufferBuilder builder = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_COLOR
            );

            buildSkySphere(builder, profile);

            MeshData meshData = builder.build();

            if (meshData != null) {
                BufferUploader.drawWithShader(meshData);
            }
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();

            // 恢复后续世界渲染所需的常规状态。
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();

            RenderSystem.setShaderColor(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }
    }

    private static void buildSkySphere(
            BufferBuilder builder,
            EnvironmentProfile profile
    ) {
        /*
         * 纬度范围：
         *
         * -90°：世界下方
         *   0°：世界地平线
         * +90°：世界上方
         *
         * 保留完整球体，避免向下看时出现背景缺口。
         */
        for (
                int latitudeIndex = 0;
                latitudeIndex < LATITUDE_SEGMENTS;
                latitudeIndex++
        ) {
            double latitude0 =
                    -Math.PI * 0.5
                            + Math.PI * latitudeIndex / LATITUDE_SEGMENTS;

            double latitude1 =
                    -Math.PI * 0.5
                            + Math.PI * (latitudeIndex + 1) / LATITUDE_SEGMENTS;

            float cosLatitude0 = (float) Math.cos(latitude0);
            float sinLatitude0 = (float) Math.sin(latitude0);

            float cosLatitude1 = (float) Math.cos(latitude1);
            float sinLatitude1 = (float) Math.sin(latitude1);

            float ringRadius0 = cosLatitude0 * SKY_RADIUS;
            float ringRadius1 = cosLatitude1 * SKY_RADIUS;

            float y0 = sinLatitude0 * SKY_RADIUS;
            float y1 = sinLatitude1 * SKY_RADIUS;

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
                        Math.PI * 2.0
                                * longitudeIndex / LONGITUDE_SEGMENTS;

                double longitude1 =
                        Math.PI * 2.0
                                * (longitudeIndex + 1) / LONGITUDE_SEGMENTS;

                float cosLongitude0 = (float) Math.cos(longitude0);
                float sinLongitude0 = (float) Math.sin(longitude0);

                float cosLongitude1 = (float) Math.cos(longitude1);
                float sinLongitude1 = (float) Math.sin(longitude1);

                addVertex(
                        builder,
                        cosLongitude0 * ringRadius0,
                        y0,
                        sinLongitude0 * ringRadius0,
                        color0
                );

                addVertex(
                        builder,
                        cosLongitude1 * ringRadius0,
                        y0,
                        sinLongitude1 * ringRadius0,
                        color0
                );

                addVertex(
                        builder,
                        cosLongitude1 * ringRadius1,
                        y1,
                        sinLongitude1 * ringRadius1,
                        color1
                );

                addVertex(
                        builder,
                        cosLongitude0 * ringRadius1,
                        y1,
                        sinLongitude0 * ringRadius1,
                        color1
                );
            }
        }
    }

    private static SkyVertexColor calculateColor(
            EnvironmentProfile profile,
            float normalizedHeight
    ) {
        float upwardHeight =
                Mth.clamp(normalizedHeight, 0.0F, 1.0F);

        float topBlend =
                (float) Math.pow(upwardHeight, HORIZON_FALLOFF);

        // 从世界地平线向世界顶部渐变。
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

        // 世界地平线以下逐渐过渡到雾色。
        float belowHorizon =
                Mth.clamp(-normalizedHeight, 0.0F, 1.0F);

        float fogBlend = smoothStep(belowHorizon);

        red = Mth.lerp(fogBlend, red, profile.fogR());
        green = Mth.lerp(fogBlend, green, profile.fogG());
        blue = Mth.lerp(fogBlend, blue, profile.fogB());

        // 地平线附近更明显，顶部和底部略弱。
        float horizonWeight = smoothStep(
                1.0F - Math.abs(normalizedHeight)
        );

        float alphaMultiplier =
                Mth.lerp(horizonWeight, 0.62F, 1.0F);

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
            float x,
            float y,
            float z,
            SkyVertexColor color
    ) {
        /*
         * 不传入 PoseStack 矩阵。
         * 保留原始世界方向，供顶点着色器使用。
         */
        builder.addVertex(x, y, z)
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
