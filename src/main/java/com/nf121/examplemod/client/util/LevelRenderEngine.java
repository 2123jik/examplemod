package com.nf121.examplemod.client.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 工业级世界渲染引擎超集工具 (Industrial Level Render Engine)
 * 适配 NeoForge 1.20.6 / 1.21+
 */
public final class LevelRenderEngine {

    private LevelRenderEngine() {}

    // =========================================================================
    // 1. 核心四大管线与享元状态缓存池 (Pipelines Matrix)
    // =========================================================================
    public static class Pipelines extends RenderType {
        private Pipelines(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        private static final Map<String, RenderType> CACHE = new ConcurrentHashMap<>();

        public static RenderType lines(double lineWidth, boolean depthTest, boolean polygonOffset) {
            String key = "lines_" + lineWidth + "_" + depthTest + "_" + polygonOffset;
            return CACHE.computeIfAbsent(key, k -> create("engine_lines_" + System.nanoTime(),
                    DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_LINES_SHADER)
                            .setLineState(new LineStateShard(OptionalDouble.of(lineWidth)))
                            .setLayeringState(polygonOffset ? VIEW_OFFSET_Z_LAYERING : NO_LAYERING)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setOutputState(ITEM_ENTITY_TARGET)
                            .setWriteMaskState(depthTest ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
                            .createCompositeState(false)));
        }
        public static RenderType texturedAdditive(ResourceLocation texture) {
            return RenderType.create("void_textured_additive",
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR,
                    com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(
                                    net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                            .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE) // 禁用深度写入，防止半透穿帮
                            .createCompositeState(false));
        }
        public static RenderType unlitColor(boolean additive, boolean depthTest, boolean doubleSided) {
            String key = "unlit_col_" + additive + "_" + depthTest + "_" + doubleSided;
            return CACHE.computeIfAbsent(key, k -> create("engine_unlit_color_" + System.nanoTime(),
                    DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
                    CompositeState.builder()
                            .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                            .setTransparencyState(additive ? ADDITIVE_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
                            .setWriteMaskState(depthTest && !additive ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                            .setCullState(doubleSided ? NO_CULL : CULL)
                            .createCompositeState(false)));
        }

        public static RenderType unlitTexture(ResourceLocation texture, boolean additive, boolean depthTest, boolean doubleSided) {
            String key = "unlit_tex_" + texture + "_" + additive + "_" + depthTest + "_" + doubleSided;
            return CACHE.computeIfAbsent(key, k -> create("engine_unlit_tex_" + System.nanoTime(),
                    DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
                    CompositeState.builder()
                            .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(additive ? ADDITIVE_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
                            .setWriteMaskState(depthTest && !additive ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                            .setCullState(doubleSided ? NO_CULL : CULL)
                            .createCompositeState(false)));
        }

        public static RenderType litTexture(ResourceLocation texture, boolean depthTest, boolean doubleSided) {
            String key = "lit_tex_" + texture + "_" + depthTest + "_" + doubleSided;
            return CACHE.computeIfAbsent(key, k -> create("engine_lit_tex_" + System.nanoTime(),
                    DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS, 256, false, false,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
                            .setCullState(doubleSided ? NO_CULL : CULL)
                            .createCompositeState(false)));
        }
    }

    // =========================================================================
    // 2. 渲染事件作用域与生命周期上下文 (Render Context)
    // =========================================================================
    public static class Context {
        private final RenderLevelStageEvent event;
        private final PoseStack poseStack;
        private final Vec3 cameraPos;
        private final Frustum frustum;

        public Context(RenderLevelStageEvent event) {
            this.event = event;
            this.poseStack = event.getPoseStack();
            this.cameraPos = event.getCamera().getPosition();
            this.frustum = event.getFrustum();
        }

        public RenderLevelStageEvent getEvent() { return event; }
        public PoseStack getPoseStack() { return poseStack; }
        public Vec3 getCameraPos() { return cameraPos; }
        public Frustum getFrustum() { return frustum; }
        public float getPartialTick() { return event.getPartialTick().getGameTimeDeltaPartialTick(true); }

        public boolean isStage(RenderLevelStageEvent.Stage expectedStage) {
            return this.event.getStage() == expectedStage;
        }

        public void inWorldSpace(Consumer<PoseStack> action) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            try {
                action.accept(poseStack);
            } finally {
                poseStack.popPose();
            }
        }

        public void inLocalSpace(Vec3 worldOrigin, Consumer<PoseStack> action) {
            poseStack.pushPose();
            poseStack.translate(worldOrigin.x - cameraPos.x, worldOrigin.y - cameraPos.y, worldOrigin.z - cameraPos.z);
            try {
                action.accept(poseStack);
            } finally {
                poseStack.popPose();
            }
        }

        public boolean isVisible(AABB aabb) {
            return frustum != null && frustum.isVisible(aabb);
        }
    }

    // =========================================================================
    // 3. 严格遵循规范的顶点提交契约 (Strict Vertex Builders)
    // =========================================================================
    public static class VertexWriter {

        public static void vertexLine(VertexConsumer consumer, Matrix4f pose, PoseStack.Pose normalPose,
                                      float x, float y, float z,
                                      float r, float g, float b, float a,
                                      float nx, float ny, float nz) {
            consumer.addVertex(pose, x, y, z)
                    .setColor(r, g, b, a)
                    .setNormal(normalPose, nx, ny, nz);
        }

        public static void vertexColor(VertexConsumer consumer, Matrix4f pose,
                                       float x, float y, float z,
                                       float r, float g, float b, float a) {
            consumer.addVertex(pose, x, y, z)
                    .setColor(r, g, b, a);
        }

        public static void quadColor(VertexConsumer c, Matrix4f p,
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float x3, float y3, float z3,
                                     float r, float g, float b, float a) {
            c.addVertex(p, x0, y0, z0).setColor(r, g, b, a);
            c.addVertex(p, x1, y1, z1).setColor(r, g, b, a);
            c.addVertex(p, x2, y2, z2).setColor(r, g, b, a);
            c.addVertex(p, x3, y3, z3).setColor(r, g, b, a);
        }

        public static void vertexTexColor(VertexConsumer consumer, Matrix4f pose,
                                          float x, float y, float z,
                                          float u, float v,
                                          float r, float g, float b, float a) {
            consumer.addVertex(pose, x, y, z)
                    .setUv(u, v)
                    .setColor(r, g, b, a);
        }

        public static void vertexLit(VertexConsumer consumer, Matrix4f pose, PoseStack.Pose normalPose,
                                     float x, float y, float z,
                                     float u, float v,
                                     float r, float g, float b, float a,
                                     float nx, float ny, float nz) {
            consumer.addVertex(pose, x, y, z)
                    .setUv(u, v)
                    .setColor(r, g, b, a)
                    .setNormal(normalPose, nx, ny, nz);
        }
    }

    // =========================================================================
    // 4. 超集拓扑几何母体 (Superset Topology Primitives)
    // =========================================================================
    public static class Topology {

        // ---------------------------------------------------------------------
        // 4.1 线框与实体包围盒 (Bounding Boxes)
        // ---------------------------------------------------------------------

        public static void wireframeBox(PoseStack poseStack, VertexConsumer consumer, AABB aabb, float r, float g, float b, float a) {
            Matrix4f pose = poseStack.last().pose();
            PoseStack.Pose last = poseStack.last();
            float x0 = (float) aabb.minX, y0 = (float) aabb.minY, z0 = (float) aabb.minZ;
            float x1 = (float) aabb.maxX, y1 = (float) aabb.maxY, z1 = (float) aabb.maxZ;

            lineEdge(consumer, pose, last, x0, y0, z0, x1, y0, z0, r, g, b, a);
            lineEdge(consumer, pose, last, x1, y0, z0, x1, y0, z1, r, g, b, a);
            lineEdge(consumer, pose, last, x1, y0, z1, x0, y0, z1, r, g, b, a);
            lineEdge(consumer, pose, last, x0, y0, z1, x0, y0, z0, r, g, b, a);
            lineEdge(consumer, pose, last, x0, y1, z0, x1, y1, z0, r, g, b, a);
            lineEdge(consumer, pose, last, x1, y1, z0, x1, y1, z1, r, g, b, a);
            lineEdge(consumer, pose, last, x1, y1, z1, x0, y1, z1, r, g, b, a);
            lineEdge(consumer, pose, last, x0, y1, z1, x0, y1, z0, r, g, b, a);
            lineEdge(consumer, pose, last, x0, y0, z0, x0, y1, z0, r, g, b, a);
            lineEdge(consumer, pose, last, x1, y0, z0, x1, y1, z0, r, g, b, a);
            lineEdge(consumer, pose, last, x1, y0, z1, x1, y1, z1, r, g, b, a);
            lineEdge(consumer, pose, last, x0, y0, z1, x0, y1, z1, r, g, b, a);
        }

        private static void lineEdge(VertexConsumer c, Matrix4f p, PoseStack.Pose n, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a) {
            float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
            VertexWriter.vertexLine(c, p, n, x0, y0, z0, r, g, b, a, dx, dy, dz);
            VertexWriter.vertexLine(c, p, n, x1, y1, z1, r, g, b, a, dx, dy, dz);
        }

        public static void solidBox(PoseStack ps, VertexConsumer c, AABB bb, float r, float g, float b, float a) {
            solidBox(ps, c, (float) bb.minX, (float) bb.minY, (float) bb.minZ, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, r, g, b, a);
        }

        public static void solidBox(PoseStack ps, VertexConsumer c,
                                    float x0, float y0, float z0,
                                    float x1, float y1, float z1,
                                    float r, float g, float b, float a) {
            Matrix4f pose = ps.last().pose();
            VertexWriter.quadColor(c, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a); // -Y
            VertexWriter.quadColor(c, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, a); // +Y
            VertexWriter.quadColor(c, pose, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, r, g, b, a); // -Z
            VertexWriter.quadColor(c, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a); // +Z
            VertexWriter.quadColor(c, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a); // -X
            VertexWriter.quadColor(c, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a); // +X
        }

        public static void taperedBox(PoseStack ps, VertexConsumer c,
                                      float x0, float y0, float z0, float x1, float y1, float z1,
                                      float shrinkX, float shrinkZ,
                                      float r, float g, float b, float a) {
            Matrix4f pose = ps.last().pose();
            float tx0 = x0 + shrinkX, tx1 = x1 - shrinkX;
            float tz0 = z0 + shrinkZ, tz1 = z1 - shrinkZ;

            VertexWriter.quadColor(c, pose, x0, y0, z0, x1, y0, z0, tx1, y1, tz0, tx0, y1, tz0, r, g, b, a);
            VertexWriter.quadColor(c, pose, x1, y0, z1, x0, y0, z1, tx0, y1, tz1, tx1, y1, tz1, r, g, b, a);
            VertexWriter.quadColor(c, pose, x0, y0, z1, x0, y0, z0, tx0, y1, tz0, tx0, y1, tz1, r, g, b, a);
            VertexWriter.quadColor(c, pose, x1, y0, z0, x1, y0, z1, tx1, y1, tz1, tx1, y1, tz0, r, g, b, a);
            VertexWriter.quadColor(c, pose, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, r, g, b, a);
            VertexWriter.quadColor(c, pose, tx0, y1, tz0, tx1, y1, tz0, tx1, y1, tz1, tx0, y1, tz1, r, g, b, a);
        }

        // ---------------------------------------------------------------------
        // 4.2 柱体与晶体几何 (Prisms & Octahedrons)
        // ---------------------------------------------------------------------

        public static void prism(PoseStack ps, VertexConsumer c, int sides,
                                 float y0, float y1, float r0, float r1,
                                 float r, float g, float b, float a) {
            Matrix4f pose = ps.last().pose();
            float step = (float) (Math.PI * 2.0 / sides);

            for (int i = 0; i < sides; i++) {
                float a0 = i * step;
                float a1 = (i + 1) * step;

                float x0A = Mth.cos(a0) * r0, z0A = Mth.sin(a0) * r0;
                float x1A = Mth.cos(a1) * r0, z1A = Mth.sin(a1) * r0;
                float x0B = Mth.cos(a0) * r1, z0B = Mth.sin(a0) * r1;
                float x1B = Mth.cos(a1) * r1, z1B = Mth.sin(a1) * r1;

                VertexWriter.quadColor(c, pose, x0A, y0, z0A, x1A, y0, z1A, x1B, y1, z1B, x0B, y1, z0B, r, g, b, a);
            }
        }

        public static void octahedron(PoseStack ps, VertexConsumer c, float size, float r, float g, float b, float a) {
            Matrix4f pose = ps.last().pose();
            float s = size;
            VertexWriter.quadColor(c, pose, 0, s, 0, s, 0, 0, 0, 0, s, 0, s, 0, r, g, b, a);
            VertexWriter.quadColor(c, pose, 0, s, 0, 0, 0, s, -s, 0, 0, 0, s, 0, r, g, b, a);
            VertexWriter.quadColor(c, pose, 0, s, 0, -s, 0, 0, 0, 0, -s, 0, s, 0, r, g, b, a);
            VertexWriter.quadColor(c, pose, 0, s, 0, 0, 0, -s, s, 0, 0, 0, s, 0, r, g, b, a);

            VertexWriter.quadColor(c, pose, 0, -s, 0, 0, 0, s, s, 0, 0, 0, -s, 0, r, g, b, a);
            VertexWriter.quadColor(c, pose, 0, -s, 0, -s, 0, 0, 0, 0, s, 0, -s, 0, r, g, b, a);
            VertexWriter.quadColor(c, pose, 0, -s, 0, 0, 0, -s, -s, 0, 0, 0, -s, 0, r, g, b, a);
            VertexWriter.quadColor(c, pose, 0, -s, 0, s, 0, 0, 0, 0, -s, 0, -s, 0, r, g, b, a);
        }

        public static void diamondBlade(PoseStack ps, VertexConsumer c,
                                        float y0, float yMid, float y1,
                                        float wBase, float wMid,
                                        float thickBase, float thickMid,
                                        float r, float g, float b, float a) {
            Matrix4f pose = ps.last().pose();

            float xR0 = wBase, xL0 = -wBase, zF0 = thickBase, zB0 = -thickBase;
            float xR1 = wMid, xL1 = -wMid, zF1 = thickMid, zB1 = -thickMid;
            float xTip = 0.0f, zTip = 0.0f;

            VertexWriter.quadColor(c, pose, 0, y0, zF0, xR0, y0, 0, xR1, yMid, 0, 0, yMid, zF1, r, g, b, a);
            VertexWriter.quadColor(c, pose, xL0, y0, 0, 0, y0, zF0, 0, yMid, zF1, xL1, yMid, 0, r, g, b, a);
            VertexWriter.quadColor(c, pose, xR0, y0, 0, 0, y0, zB0, 0, yMid, zB1, xR1, yMid, 0, r, g, b, a);
            VertexWriter.quadColor(c, pose, 0, y0, zB0, xL0, y0, 0, xL1, yMid, 0, 0, yMid, zB1, r, g, b, a);

            VertexWriter.quadColor(c, pose, 0, yMid, zF1, xR1, yMid, 0, xTip, y1, zTip, 0, y1, zTip, r, g, b, a);
            VertexWriter.quadColor(c, pose, xL1, yMid, 0, 0, yMid, zF1, 0, y1, zTip, xTip, y1, zTip, r, g, b, a);
            VertexWriter.quadColor(c, pose, xR1, yMid, 0, 0, yMid, zB1, 0, y1, zTip, xTip, y1, zTip, r, g, b, a);
            VertexWriter.quadColor(c, pose, 0, yMid, zB1, xL1, yMid, 0, xTip, y1, zTip, 0, y1, zTip, r, g, b, a);
        }

        // ---------------------------------------------------------------------
        // 4.3 动态全息线元 (Holo Rings & Helix Arcs)
        // ---------------------------------------------------------------------

        public static void wireRing(PoseStack ps, VertexConsumer c, float radius, int segments,
                                    float r, float g, float b, float a) {
            Matrix4f pose = ps.last().pose();
            PoseStack.Pose normal = ps.last();
            float step = (float) (Math.PI * 2.0 / segments);

            for (int i = 0; i < segments; i++) {
                float a0 = i * step;
                float a1 = (i + 1) * step;
                float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
                float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;

                VertexWriter.vertexLine(c, pose, normal, x0, 0, z0, r, g, b, a, 0.0f, 1.0f, 0.0f);
                VertexWriter.vertexLine(c, pose, normal, x1, 0, z1, r, g, b, a, 0.0f, 1.0f, 0.0f);
            }
        }

        public static void doubleHelixLines(PoseStack ps, VertexConsumer c,
                                            float yMin, float yMax, float baseRadius,
                                            float time, int segments,
                                            float r1, float g1, float b1, float a1,
                                            float r2, float g2, float b2, float a2) {
            Matrix4f pose = ps.last().pose();
            PoseStack.Pose normal = ps.last();

            float ySpan = yMax - yMin;
            float step = ySpan / segments;

            float prevX1 = 0, prevZ1 = 0, prevY1 = yMin;
            float prevX2 = 0, prevZ2 = 0, prevY2 = yMin;

            for (int i = 0; i <= segments; i++) {
                float currY = yMin + i * step;
                float progress = (currY - yMin) / ySpan;
                float radius = baseRadius * (1.0f - progress * 0.5f);

                float angle1 = currY * 18.0f - time * 9.0f;
                float angle2 = angle1 + (float) Math.PI;

                float currX1 = Mth.cos(angle1) * radius;
                float currZ1 = Mth.sin(angle1) * (radius * 0.5f);

                float currX2 = Mth.cos(angle2) * radius;
                float currZ2 = Mth.sin(angle2) * (radius * 0.5f);

                if (i > 0) {
                    VertexWriter.vertexLine(c, pose, normal, prevX1, prevY1, prevZ1, r1, g1, b1, a1, 0, 1, 0);
                    VertexWriter.vertexLine(c, pose, normal, currX1, currY, currZ1, r1, g1, b1, a1, 0, 1, 0);

                    VertexWriter.vertexLine(c, pose, normal, prevX2, prevY2, prevZ2, r2, g2, b2, a2, 0, 1, 0);
                    VertexWriter.vertexLine(c, pose, normal, currX2, currY, currZ2, r2, g2, b2, a2, 0, 1, 0);
                }

                prevX1 = currX1; prevZ1 = currZ1; prevY1 = currY;
                prevX2 = currX2; prevZ2 = currZ2; prevY2 = currY;
            }
        }

        // ---------------------------------------------------------------------
        // 4.4 纹理曲面 (Discs, Torus, Laser & Tubes)
        // ---------------------------------------------------------------------

        public static void disc(PoseStack poseStack, VertexConsumer consumer,
                                float innerRadius, float outerRadius,
                                float startAngle, float endAngle,
                                int segments,
                                float uMin, float vMin, float uMax, float vMax,
                                float r, float g, float b, float a) {
            Matrix4f pose = poseStack.last().pose();
            float step = (endAngle - startAngle) / segments;

            for (int i = 0; i < segments; i++) {
                float a0 = startAngle + i * step;
                float a1 = a0 + step;

                float cos0 = Mth.cos(a0), sin0 = Mth.sin(a0);
                float cos1 = Mth.cos(a1), sin1 = Mth.sin(a1);

                float inX0 = cos0 * innerRadius, inZ0 = sin0 * innerRadius;
                float outX0 = cos0 * outerRadius, outZ0 = sin0 * outerRadius;
                float inX1 = cos1 * innerRadius, inZ1 = sin1 * innerRadius;
                float outX1 = cos1 * outerRadius, outZ1 = sin1 * outerRadius;

                float u0 = Mth.lerp((float) i / segments, uMin, uMax);
                float u1 = Mth.lerp((float) (i + 1) / segments, uMin, uMax);

                VertexWriter.vertexTexColor(consumer, pose, inX0, 0, inZ0, u0, vMin, r, g, b, a);
                VertexWriter.vertexTexColor(consumer, pose, outX0, 0, outZ0, u0, vMax, r, g, b, a);
                VertexWriter.vertexTexColor(consumer, pose, outX1, 0, outZ1, u1, vMax, r, g, b, a);
                VertexWriter.vertexTexColor(consumer, pose, inX1, 0, inZ1, u1, vMin, r, g, b, a);
            }
        }

        public static void torus(PoseStack poseStack, VertexConsumer consumer,
                                 float mainRadius, float tubeRadius,
                                 int mainSegments, int tubeSegments,
                                 float vOffset, float r, float g, float b, float a) {
            Matrix4f pose = poseStack.last().pose();
            float mainStep = (float) (Math.PI * 2.0 / mainSegments);
            float tubeStep = (float) (Math.PI * 2.0 / tubeSegments);

            for (int i = 0; i < mainSegments; i++) {
                float u0 = (float) i / mainSegments;
                float u1 = (float) (i + 1) / mainSegments;
                float a0 = i * mainStep;
                float a1 = (i + 1) * mainStep;

                for (int j = 0; j < tubeSegments; j++) {
                    float v0 = (float) j / tubeSegments + vOffset;
                    float v1 = (float) (j + 1) / tubeSegments + vOffset;
                    float t0 = j * tubeStep;
                    float t1 = (j + 1) * tubeStep;

                    Vector3f p0 = sampleTorus(mainRadius, tubeRadius, a0, t0);
                    Vector3f p1 = sampleTorus(mainRadius, tubeRadius, a1, t0);
                    Vector3f p2 = sampleTorus(mainRadius, tubeRadius, a1, t1);
                    Vector3f p3 = sampleTorus(mainRadius, tubeRadius, a0, t1);

                    VertexWriter.vertexTexColor(consumer, pose, p0.x, p0.y, p0.z, u0, v0, r, g, b, a);
                    VertexWriter.vertexTexColor(consumer, pose, p1.x, p1.y, p1.z, u1, v0, r, g, b, a);
                    VertexWriter.vertexTexColor(consumer, pose, p2.x, p2.y, p2.z, u1, v1, r, g, b, a);
                    VertexWriter.vertexTexColor(consumer, pose, p3.x, p3.y, p3.z, u0, v1, r, g, b, a);
                }
            }
        }

        private static Vector3f sampleTorus(float R, float r, float theta, float phi) {
            float x = (float) ((R + r * Math.cos(phi)) * Math.cos(theta));
            float y = (float) (r * Math.sin(phi));
            float z = (float) ((R + r * Math.cos(phi)) * Math.sin(theta));
            return new Vector3f(x, y, z);
        }

        public static void laserBeam(PoseStack poseStack, VertexConsumer consumer,
                                     Vec3 start, Vec3 end,
                                     float radius, int facets,
                                     float vScroll, float uLengthScale,
                                     float r, float g, float b, float a) {
            Vec3 dir = end.subtract(start);
            double length = dir.length();
            if (length < 1e-4) return;

            poseStack.pushPose();
            poseStack.translate(start.x, start.y, start.z);

            Vec3 up = Math.abs(dir.y / length) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 forward = dir.normalize();
            Vec3 right = forward.cross(up).normalize();
            Vec3 realUp = right.cross(forward).normalize();

            Matrix4f pose = poseStack.last().pose();
            float facetStep = (float) (Math.PI * 2.0 / facets);
            float totalV = (float) (length * uLengthScale) + vScroll;

            for (int i = 0; i < facets; i++) {
                float a0 = i * facetStep;
                float a1 = (i + 1) * facetStep;
                float u0 = (float) i / facets;
                float u1 = (float) (i + 1) / facets;

                Vector3f offset0 = evaluateRadialOffset(right, realUp, a0, radius);
                Vector3f offset1 = evaluateRadialOffset(right, realUp, a1, radius);

                float x0 = offset0.x, y0 = offset0.y, z0 = offset0.z;
                float x1 = offset1.x, y1 = offset1.y, z1 = offset1.z;

                float ex0 = (float) (x0 + dir.x), ey0 = (float) (y0 + dir.y), ez0 = (float) (z0 + dir.z);
                float ex1 = (float) (x1 + dir.x), ey1 = (float) (y1 + dir.y), ez1 = (float) (z1 + dir.z);

                VertexWriter.vertexTexColor(consumer, pose, x0, y0, z0, u0, vScroll, r, g, b, a);
                VertexWriter.vertexTexColor(consumer, pose, x1, y1, z1, u1, vScroll, r, g, b, a);
                VertexWriter.vertexTexColor(consumer, pose, ex1, ey1, ez1, u1, totalV, r, g, b, a);
                VertexWriter.vertexTexColor(consumer, pose, ex0, ey0, ez0, u0, totalV, r, g, b, a);
            }

            poseStack.popPose();
        }

        private static Vector3f evaluateRadialOffset(Vec3 r, Vec3 u, float angle, float radius) {
            float cos = Mth.cos(angle) * radius;
            float sin = Mth.sin(angle) * radius;
            return new Vector3f((float) (r.x * cos + u.x * sin), (float) (r.y * cos + u.y * sin), (float) (r.z * cos + u.z * sin));
        }

        public static void extrudedTube(PoseStack poseStack, VertexConsumer consumer,
                                        List<Vec3> path, float radius, int segments,
                                        BiConsumer<Float, Vector4f> colorGrad,
                                        float vTextureScale) {
            int n = path.size();
            if (n < 2) return;
            Matrix4f pose = poseStack.last().pose();

            List<Vector3f> normalList = new ArrayList<>();
            List<Vector3f> binormalList = new ArrayList<>();

            Vec3 t0 = path.get(1).subtract(path.get(0)).normalize();
            Vec3 initialUp = Math.abs(t0.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 r = t0.cross(initialUp).normalize();
            Vec3 u = r.cross(t0).normalize();

            normalList.add(new Vector3f((float) r.x, (float) r.y, (float) r.z));
            binormalList.add(new Vector3f((float) u.x, (float) u.y, (float) u.z));

            for (int i = 1; i < n; i++) {
                Vec3 pPrev = path.get(i - 1);
                Vec3 pCurr = path.get(i);
                Vec3 v = pCurr.subtract(pPrev);
                float c1 = (float) v.dot(v);
                if (c1 > 1e-6) {
                    Vec3 rPrev = new Vec3(normalList.get(i - 1).x, normalList.get(i - 1).y, normalList.get(i - 1).z);
                    Vec3 riL = rPrev.subtract(v.scale(2.0 / c1 * v.dot(rPrev)));
                    Vec3 tCurr = (i < n - 1) ? path.get(i + 1).subtract(pCurr).normalize() : v.normalize();
                    Vec3 v2 = tCurr.subtract(v.scale(1.0 / Math.sqrt(c1)));
                    float c2 = (float) v2.dot(v2);
                    if (c2 > 1e-6) {
                        r = riL.subtract(v2.scale(2.0 / c2 * v2.dot(riL))).normalize();
                    }
                }
                u = path.get(Math.min(i + 1, n - 1)).subtract(pCurr).normalize().cross(r).normalize();
                normalList.add(new Vector3f((float) r.x, (float) r.y, (float) r.z));
                binormalList.add(new Vector3f((float) u.x, (float) u.y, (float) u.z));
            }

            Vector4f c0 = new Vector4f(), c1 = new Vector4f();
            float angleStep = (float) (Math.PI * 2.0 / segments);

            for (int i = 0; i < n - 1; i++) {
                float pStep0 = (float) i / (n - 1);
                float pStep1 = (float) (i + 1) / (n - 1);
                colorGrad.accept(pStep0, c0);
                colorGrad.accept(pStep1, c1);

                Vec3 pA = path.get(i);
                Vec3 pB = path.get(i + 1);

                Vector3f rA = normalList.get(i), uA = binormalList.get(i);
                Vector3f rB = normalList.get(i + 1), uB = binormalList.get(i + 1);

                float v0 = pStep0 * vTextureScale;
                float v1 = pStep1 * vTextureScale;

                for (int s = 0; s < segments; s++) {
                    float aA = s * angleStep;
                    float aB = (s + 1) * angleStep;
                    float uCoord0 = (float) s / segments;
                    float uCoord1 = (float) (s + 1) / segments;

                    Vector3f vAA = getTubeOffset(pA, rA, uA, aA, radius);
                    Vector3f vAB = getTubeOffset(pA, rA, uA, aB, radius);
                    Vector3f vBA = getTubeOffset(pB, rB, uB, aA, radius);
                    Vector3f vBB = getTubeOffset(pB, rB, uB, aB, radius);

                    VertexWriter.vertexTexColor(consumer, pose, vAA.x, vAA.y, vAA.z, uCoord0, v0, c0.x, c0.y, c0.z, c0.w);
                    VertexWriter.vertexTexColor(consumer, pose, vAB.x, vAB.y, vAB.z, uCoord1, v0, c0.x, c0.y, c0.z, c0.w);
                    VertexWriter.vertexTexColor(consumer, pose, vBB.x, vBB.y, vBB.z, uCoord1, v1, c1.x, c1.y, c1.z, c1.w);
                    VertexWriter.vertexTexColor(consumer, pose, vBA.x, vBA.y, vBA.z, uCoord0, v1, c1.x, c1.y, c1.z, c1.w);
                }
            }
        }

        private static Vector3f getTubeOffset(Vec3 origin, Vector3f r, Vector3f u, float angle, float radius) {
            float cos = Mth.cos(angle) * radius;
            float sin = Mth.sin(angle) * radius;
            return new Vector3f((float) origin.x + r.x * cos + u.x * sin,
                    (float) origin.y + r.y * cos + u.y * sin,
                    (float) origin.z + r.z * cos + u.z * sin);
        }
    }

    // =========================================================================
    // 5. 投影解算与视线追踪算子 (Projection & Ray Unprojector)
    // =========================================================================
    public static class Raycast {
        public static Vec3 screenToWorldRay(double mouseX, double mouseY, int windowWidth, int windowHeight,
                                            Matrix4f viewMatrix, Matrix4f projMatrix, Vec3 cameraPos) {
            float ndcX = (float) ((2.0 * mouseX) / windowWidth - 1.0);
            float ndcY = (float) (1.0 - (2.0 * mouseY) / windowHeight);

            Matrix4f invMat = new Matrix4f(projMatrix).mul(viewMatrix).invert();

            Vector4f nearPoint = new Vector4f(ndcX, ndcY, -1.0f, 1.0f).mul(invMat);
            Vector4f farPoint = new Vector4f(ndcX, ndcY, 1.0f, 1.0f).mul(invMat);

            nearPoint.div(nearPoint.w);
            farPoint.div(farPoint.w);

            Vec3 pNear = new Vec3(nearPoint.x, nearPoint.y, nearPoint.z);
            Vec3 pFar = new Vec3(farPoint.x, farPoint.y, farPoint.z);

            return pFar.subtract(pNear).normalize();
        }
    }
}