package com.example.examplemod.event.explosion;

import com.example.examplemod.menu.enderinterface.EnderInterface;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = EnderInterface.MODID, value = Dist.CLIENT)
public class ExplosionClientRenderer {

    public static class ShockwaveInstance {
        final Vec3 center;
        final float maxRadius;
        final int rgbColor;
        int age = 0;
        final int maxAge = 9;

        public ShockwaveInstance(Vec3 center, float maxRadius, int rgbColor) {
            this.center = center;
            this.maxRadius = maxRadius;
            this.rgbColor = rgbColor;
        }
    }

    private static final List<ShockwaveInstance> ACTIVE_SHOCKWAVES = new ArrayList<>();

    public static void handlePacket(ExplosionServerHandler.ExplosionShockwavePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ACTIVE_SHOCKWAVES.add(new ShockwaveInstance(
                new Vec3(payload.x(), payload.y(), payload.z()),
                payload.maxRadius(),
                payload.rgbColor()
        )));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Iterator<ShockwaveInstance> it = ACTIVE_SHOCKWAVES.iterator();
        while (it.hasNext()) {
            ShockwaveInstance sw = it.next();
            sw.age++;
            if (sw.age > sw.maxAge) it.remove();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_SHOCKWAVES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lightning());
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        for (ShockwaveInstance sw : ACTIVE_SHOCKWAVES) {
            float progress = (sw.age + partialTicks) / (float) sw.maxAge;
            if (progress > 1.0F) continue;

            float easeRadius = sw.maxRadius * (float) (1.0 - Math.pow(1.0 - progress, 3));
            float energy = 1.0F - progress;

            // 1. 激波外壳
            renderFresnelSphere(mc.level, poseStack, builder, sw.center, camPos, easeRadius, energy, sw.rgbColor, 1.0F);

            // 2. 内层极核火球
            if (progress < 0.6F) {
                float coreProgress = progress / 0.6F;
                float coreRadius = (sw.maxRadius * 0.45F) * (float) Math.sin(coreProgress * Math.PI / 2.0);
                float coreEnergy = 1.0F - coreProgress;
                renderFresnelSphere(mc.level, poseStack, builder, sw.center, camPos, coreRadius, coreEnergy, sw.rgbColor, 1.8F);
            }

            // 3. 地表平切冲击盘
            renderEquatorialShockRing(poseStack, builder, sw.center, camPos, easeRadius, energy, sw.rgbColor);
        }
    }

    private static void renderFresnelSphere(net.minecraft.world.level.Level level, PoseStack poseStack, VertexConsumer builder,
                                            Vec3 center, Vec3 camPos, float currentRadius,
                                            float energy, int rgbColor, float intensityMultiplier) {
        poseStack.pushPose();
        poseStack.translate(center.x - camPos.x, center.y - camPos.y, center.z - camPos.z);
        var matrix = poseStack.last().pose();

        int lats = 16;
        int lons = 32;

        for (int i = 0; i < lats; i++) {
            float lat0 = (float) Math.PI * (-0.5F + (float) i / lats);
            float z0 = Mth.sin(lat0); float zr0 = Mth.cos(lat0);

            float lat1 = (float) Math.PI * (-0.5F + (float) (i + 1) / lats);
            float z1 = Mth.sin(lat1); float zr1 = Mth.cos(lat1);

            for (int j = 0; j < lons; j++) {
                float lon0 = 2.0F * (float) Math.PI * (float) j / lons;
                float x0 = Mth.cos(lon0); float y0 = Mth.sin(lon0);

                float lon1 = 2.0F * (float) Math.PI * (float) (j + 1) / lons;
                float x1 = Mth.cos(lon1); float y1 = Mth.sin(lon1);

                Vec3 d1 = new Vec3(x0 * zr0, z0, y0 * zr0);
                Vec3 d2 = new Vec3(x1 * zr0, z0, y1 * zr0);
                Vec3 d3 = new Vec3(x1 * zr1, z1, y1 * zr1);
                Vec3 d4 = new Vec3(x0 * zr1, z1, y0 * zr1);

                Vec3 p1 = getOccludedVertex(level, center, d1, currentRadius);
                Vec3 p2 = getOccludedVertex(level, center, d2, currentRadius);
                Vec3 p3 = getOccludedVertex(level, center, d3, currentRadius);
                Vec3 p4 = getOccludedVertex(level, center, d4, currentRadius);

                addFresnelVertex(builder, matrix, p1, d1, center, camPos, energy, rgbColor, intensityMultiplier);
                addFresnelVertex(builder, matrix, p2, d2, center, camPos, energy, rgbColor, intensityMultiplier);
                addFresnelVertex(builder, matrix, p3, d3, center, camPos, energy, rgbColor, intensityMultiplier);
                addFresnelVertex(builder, matrix, p4, d4, center, camPos, energy, rgbColor, intensityMultiplier);
            }
        }
        poseStack.popPose();
    }

    private static void addFresnelVertex(VertexConsumer builder, org.joml.Matrix4f matrix,
                                         Vec3 localPos, Vec3 normal, Vec3 center, Vec3 camPos,
                                         float energy, int rgbColor, float intensityMultiplier) {
        Vec3 worldPos = center.add(localPos);
        Vec3 viewDir = camPos.subtract(worldPos).normalize();

        double dot = Math.abs(viewDir.dot(normal));
        float fresnel = (float) Math.pow(1.0 - dot, 2.5);

        int r, g, b, alpha;

        if (rgbColor != -1) {
            // --- 动态学派色彩光谱插值 (根据官方学派 Style 自动变幻) ---
            int rawR = (rgbColor >> 16) & 0xFF;
            int rawG = (rgbColor >> 8) & 0xFF;
            int rawB = rgbColor & 0xFF;

            if (energy > 0.65F) {
                // 超临界阶段：向白光过曝光 Bloom 融合
                r = (int) Mth.lerp(0.5F, rawR, 255);
                g = (int) Mth.lerp(0.5F, rawG, 255);
                b = (int) Mth.lerp(0.5F, rawB, 255);
                alpha = (int) ((30 + fresnel * 210) * energy * intensityMultiplier);
            } else {
                // 常规激波与边缘：呈现饱和纯净的官方学派原色
                r = (int) Mth.lerp(fresnel, rawR * 0.7F, rawR);
                g = (int) Mth.lerp(fresnel, rawG * 0.7F, rawG);
                b = (int) Mth.lerp(fresnel, rawB * 0.7F, rawB);
                alpha = (int) ((15 + fresnel * 170) * energy * intensityMultiplier);
            }
        } else {
            // --- 默认经典烈焰爆炸色温 ---
            if (energy > 0.65F) {
                r = 255; g = (int) Mth.lerp(fresnel, 240, 200); b = (int) Mth.lerp(fresnel, 220, 80);
                alpha = (int) ((20 + fresnel * 200) * energy * intensityMultiplier);
            } else if (energy > 0.3F) {
                r = 255; g = (int) Mth.lerp(fresnel, 60, 160); b = (int) Mth.lerp(fresnel, 10, 40);
                alpha = (int) ((15 + fresnel * 180) * energy * intensityMultiplier);
            } else {
                r = (int) Mth.lerp(fresnel, 40, 180); g = (int) Mth.lerp(fresnel, 160, 220); b = 255;
                alpha = (int) ((10 + fresnel * 120) * (energy / 0.3F) * intensityMultiplier);
            }
        }

        alpha = Mth.clamp(alpha, 0, 255);
        builder.addVertex(matrix, (float) localPos.x, (float) localPos.y, (float) localPos.z).setColor(r, g, b, alpha);
    }

    private static void renderEquatorialShockRing(PoseStack poseStack, VertexConsumer builder,
                                                  Vec3 center, Vec3 camPos, float currentRadius, float energy, int rgbColor) {
        poseStack.pushPose();
        poseStack.translate(center.x - camPos.x, center.y - camPos.y, center.z - camPos.z);
        var matrix = poseStack.last().pose();

        int segments = 36;
        float innerRadius = currentRadius * 0.82F;
        float outerRadius = currentRadius * 1.05F;

        int ringAlpha = (int) (180 * energy);
        int r = rgbColor != -1 ? (rgbColor >> 16) & 0xFF : (energy > 0.5F ? 255 : 80);
        int g = rgbColor != -1 ? (rgbColor >> 8) & 0xFF  : (energy > 0.5F ? 200 : 180);
        int b = rgbColor != -1 ? rgbColor & 0xFF         : (energy > 0.5F ? 100 : 255);

        for (int i = 0; i < segments; i++) {
            float angle0 = 2.0F * (float) Math.PI * (float) i / segments;
            float angle1 = 2.0F * (float) Math.PI * (float) (i + 1) / segments;

            float x0_in = Mth.cos(angle0) * innerRadius; float z0_in = Mth.sin(angle0) * innerRadius;
            float x0_out = Mth.cos(angle0) * outerRadius; float z0_out = Mth.sin(angle0) * outerRadius;

            float x1_in = Mth.cos(angle1) * innerRadius; float z1_in = Mth.sin(angle1) * innerRadius;
            float x1_out = Mth.cos(angle1) * outerRadius; float z1_out = Mth.sin(angle1) * outerRadius;

            builder.addVertex(matrix, x0_in, 0.05F, z0_in).setColor(r, g, b, 0);
            builder.addVertex(matrix, x1_in, 0.05F, z1_in).setColor(r, g, b, 0);
            builder.addVertex(matrix, x1_out, 0.05F, z1_out).setColor(r, g, b, ringAlpha);
            builder.addVertex(matrix, x0_out, 0.05F, z0_out).setColor(r, g, b, ringAlpha);
        }
        poseStack.popPose();
    }

    private static Vec3 getOccludedVertex(net.minecraft.world.level.Level level, Vec3 center, Vec3 dir, float radius) {
        Vec3 end = center.add(dir.scale(radius));
        BlockHitResult hit = level.clip(new ClipContext(
                center, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ));

        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation().subtract(center);
        }
        return dir.scale(radius);
    }
}