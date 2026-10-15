package com.nf121.examplemod.client.particle;

import com.nf121.examplemod.client.environment.ClientEnvironmentState;
import com.nf121.examplemod.client.environment.EnvironmentProfile;
import com.nf121.examplemod.client.render.ExampleShaders;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public final class AmbientParticleRenderer {

    private static final int ATLAS_COLUMNS = 4;
    private static final int ATLAS_ROWS = 2;

    private AmbientParticleRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (ExampleShaders.ambientParticle == null) {
            return;
        }

        List<AmbientParticleManager.AmbientParticle> particles =
            AmbientParticleManager.INSTANCE.getParticles();

        if (particles.isEmpty()) {
            return;
        }

        float partialTick =
            event.getPartialTick().getGameTimeDeltaPartialTick(false);

        EnvironmentProfile profile =
            ClientEnvironmentState.getProfile(partialTick);

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();

        Vector3f left = new Vector3f(camera.getLeftVector());
        Vector3f up = new Vector3f(camera.getUpVector());

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(
            -cameraPosition.x,
            -cameraPosition.y,
            -cameraPosition.z
        );

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShaderTexture(
            0,
            AmbientParticleManager.PARTICLE_TEXTURE
        );

        RenderSystem.setShader(() -> ExampleShaders.ambientParticle);

        if (
            ExampleShaders.ambientParticle.getUniform("TierBrightness")
                != null
        ) {
            ExampleShaders.ambientParticle
                .getUniform("TierBrightness")
                .set(profile.brightness());
        }

        drawPass(
            particles,
            matrix,
            left,
            up,
            cameraPosition,
            partialTick,
            false
        );

        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE
        );

        drawPass(
            particles,
            matrix,
            left,
            up,
            cameraPosition,
            partialTick,
            true
        );

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static void drawPass(
            List<AmbientParticleManager.AmbientParticle> particles,
            Matrix4f matrix,
            Vector3f left,
            Vector3f up,
            Vec3 cameraPosition,
            float partialTick,
            boolean emissivePass
    ) {
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR
        );

        for (AmbientParticleManager.AmbientParticle particle : particles) {
            if (particle.emissive != emissivePass) {
                continue;
            }

            double x = Mth.lerp(
                    partialTick,
                    particle.previousX,
                    particle.x
            );

            double y = Mth.lerp(
                    partialTick,
                    particle.previousY,
                    particle.y
            );

            double z = Mth.lerp(
                    partialTick,
                    particle.previousZ,
                    particle.z
            );

            double deltaX = x - cameraPosition.x;
            double deltaY = y - cameraPosition.y;
            double deltaZ = z - cameraPosition.z;

            double distanceSquared =
                    deltaX * deltaX
                            + deltaY * deltaY
                            + deltaZ * deltaZ;

            if (distanceSquared > 72.0 * 72.0) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);

            float nearFade = Mth.clamp(
                    (distance - 1.2F) / 3.0F,
                    0.0F,
                    1.0F
            );

            float farFade = Mth.clamp(
                    (72.0F - distance) / 10.0F,
                    0.0F,
                    1.0F
            );

            float alpha =
                    particle.getAlpha(partialTick)
                            * nearFade
                            * farFade;

            if (alpha <= 0.002F) {
                continue;
            }

            addParticleQuad(
                    builder,
                    matrix,
                    left,
                    up,
                    (float) x,
                    (float) y,
                    (float) z,
                    particle.size,
                    particle.red,
                    particle.green,
                    particle.blue,
                    alpha,
                    particle.sprite
            );
        }

        MeshData meshData = builder.build();

        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
    }


    private static void addParticleQuad(
        BufferBuilder builder,
        Matrix4f matrix,
        Vector3f left,
        Vector3f up,
        float x,
        float y,
        float z,
        float size,
        float red,
        float green,
        float blue,
        float alpha,
        int sprite
    ) {
        float lx = left.x() * size;
        float ly = left.y() * size;
        float lz = left.z() * size;

        float ux = up.x() * size;
        float uy = up.y() * size;
        float uz = up.z() * size;

        int column = sprite % ATLAS_COLUMNS;
        int row = sprite / ATLAS_COLUMNS;

        float u0 = column / (float) ATLAS_COLUMNS;
        float u1 = (column + 1) / (float) ATLAS_COLUMNS;

        float v0 = row / (float) ATLAS_ROWS;
        float v1 = (row + 1) / (float) ATLAS_ROWS;

        builder.addVertex(
                matrix,
                x - lx - ux,
                y - ly - uy,
                z - lz - uz
            )
            .setUv(u0, v1)
            .setColor(red, green, blue, alpha);

        builder.addVertex(
                matrix,
                x + lx - ux,
                y + ly - uy,
                z + lz - uz
            )
            .setUv(u1, v1)
            .setColor(red, green, blue, alpha);

        builder.addVertex(
                matrix,
                x + lx + ux,
                y + ly + uy,
                z + lz + uz
            )
            .setUv(u1, v0)
            .setColor(red, green, blue, alpha);

        builder.addVertex(
                matrix,
                x - lx + ux,
                y - ly + uy,
                z - lz + uz
            )
            .setUv(u0, v0)
            .setColor(red, green, blue, alpha);
    }
}
