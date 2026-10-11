package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(
        modid = ExampleMod.MOD_ID,
        value = Dist.CLIENT
)
public final class LivingHudRenderer {
    private static final Minecraft MC = Minecraft.getInstance();

    // 48 格内显示。距离判断使用平方距离，避免开方。
    private static final double RANGE = 48.0;
    private static final double RANGE_SQR = RANGE * RANGE;

    private static final float BAR_SCALE = 0.025F;
    private static final float HALF_WIDTH = 24.0F;
    private static final float BAR_HEIGHT = 3.0F;

    private static final int DAMAGE_LIFETIME = 24;
    private static final int MAX_DAMAGE_NUMBERS = 1024;

    // 建议保持关闭：大量生物时，文字通常比几个矩形更费。
    private static final boolean SHOW_HEALTH_TEXT = true;

    private static final Map<Integer, HealthState> STATES = new HashMap<>();
    private static final ArrayDeque<DamageNumber> DAMAGES = new ArrayDeque<>();

    // 使用独立缓冲，不主动刷新原版正在使用的 BufferSource。
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(64 * 1024));

    private static final RenderType BAR_RENDER_TYPE = RenderType.debugQuads();

    private static ClientLevel trackedLevel;
    private static int tick;

    private LivingHudRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = MC.level;

        if (level != trackedLevel) {
            STATES.clear();
            DAMAGES.clear();
            trackedLevel = level;
            tick = 0;
        }

        if (level == null || MC.player == null || MC.isPaused()) {
            return;
        }

        ++tick;

        Entity viewer = MC.getCameraEntity();
        if (viewer == null) {
            viewer = MC.player;
        }

        // 每 tick 扫描一次。此处不进行射线遮挡检测。
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            if (living == viewer
                    || living.isRemoved()
                    || living.isInvisible()
                    || living.isSpectator()
                    || living.distanceToSqr(viewer) > RANGE_SQR) {
                continue;
            }

            float health = Math.max(0.0F, living.getHealth());
            float maxHealth = Math.max(1.0F, living.getMaxHealth());

            HealthState state = STATES.get(living.getId());

            // 身份检查防止实体 ID 被重复使用后继承旧血量。
            if (state == null || state.entity != living) {
                STATES.put(
                        living.getId(),
                        new HealthState(living, health, maxHealth, tick)
                );
                continue;
            }

            state.lastSeenTick = tick;
            state.previousTrail = state.trail;

            float lostHealth = state.health - health;
            if (lostHealth > 0.01F) {
                addDamage(living, lostHealth);
            }

            state.health = health;
            state.maxHealth = maxHealth;

            // 回血时立即抬升残影，掉血时缓慢追赶真实血量。
            if (state.trail < health) {
                state.trail = health;
                state.previousTrail = health;
            } else {
                state.trail = Mth.lerp(0.18F, state.trail, health);
                if (state.trail - health < 0.01F) {
                    state.trail = health;
                }
            }

            if (SHOW_HEALTH_TEXT) {
                state.updateLabel();
            }
        }

        // 离开范围、卸载、隐形的实体立即移出渲染缓存。
        Iterator<HealthState> states = STATES.values().iterator();
        while (states.hasNext()) {
            if (states.next().lastSeenTick != tick) {
                states.remove();
            }
        }

        // 按生成时间入队，因此只需要检查队首。
        while (!DAMAGES.isEmpty()
                && tick - DAMAGES.peekFirst().bornTick >= DAMAGE_LIFETIME) {
            DAMAGES.removeFirst();
        }
    }

    private static void addDamage(LivingEntity entity, float amount) {
        while (DAMAGES.size() >= MAX_DAMAGE_NUMBERS) {
            DAMAGES.removeFirst();
        }

        // 用整数混合生成方向，无需 Random 对象或三角函数。
        int hash = entity.getId() * 73428767 ^ tick * 912931;
        float drift = (((hash >>> 8) & 255) / 255.0F - 0.5F) * 18.0F;

        DAMAGES.addLast(new DamageNumber(
                entity.getX(),
                entity.getY() + entity.getBbHeight() + 0.65,
                entity.getZ(),
                formatDamage(amount),
                tick,
                drift
        ));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || MC.level == null
                || MC.level != trackedLevel
                || MC.player == null
                || MC.options.hideGui
                || (STATES.isEmpty() && DAMAGES.isEmpty())) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack pose = event.getPoseStack();

        // 所有血条共用同一种 RenderType 和一次提交。
        VertexConsumer vertices = BUFFERS.getBuffer(BAR_RENDER_TYPE);

        for (HealthState state : STATES.values()) {
            LivingEntity entity = state.entity;

            if (!entity.isAlive()
                    || entity.isRemoved()
                    || entity.isInvisible()
                    || entity.isSpectator()
                    || entity.distanceToSqr(cameraPos) > RANGE_SQR) {
                continue;
            }

            // 实体包围盒向上扩展，涵盖头顶血条。
            AABB bounds = entity.getBoundingBox().inflate(0.8, 1.0, 0.8);
            if (!event.getFrustum().isVisible(bounds)) {
                continue;
            }

            pushBillboard(pose, camera, cameraPos, entity, partialTick);

            Matrix4f matrix = pose.last().pose();

            float ratio = Mth.clamp(
                    state.health / state.maxHealth,
                    0.0F,
                    1.0F
            );

            float trail = Mth.lerp(
                    partialTick,
                    state.previousTrail,
                    state.trail
            );

            float trailRatio = Mth.clamp(
                    trail / state.maxHealth,
                    ratio,
                    1.0F
            );

            drawHealthBar(vertices, matrix, ratio, trailRatio);

            pose.popPose();

        }

        BUFFERS.endBatch(BAR_RENDER_TYPE);

        // 文字另作一批，避免在矩形和文字之间反复切换缓冲。
        if (SHOW_HEALTH_TEXT) {
            for (HealthState state : STATES.values()) {
                LivingEntity entity = state.entity;

                if (!entity.isAlive()
                        || entity.isRemoved()
                        || entity.isInvisible()
                        || entity.isSpectator()
                        || entity.distanceToSqr(cameraPos) > RANGE_SQR
                        || !event.getFrustum().isVisible(
                                entity.getBoundingBox().inflate(0.8, 1.0, 0.8))) {
                    continue;
                }

                pushBillboard(pose, camera, cameraPos, entity, partialTick);

                float textX = -HALF_WIDTH - 1.0F - 4.0F - state.labelWidth;
                float textY = (BAR_HEIGHT - MC.font.lineHeight) * 0.5F;

                MC.font.drawInBatch(
                        state.label,
                        textX,
                        textY,
                        0xFFFFFFFF,
                        true,
                        pose.last().pose(),
                        BUFFERS,
                        Font.DisplayMode.NORMAL,
                        0,
                        LightTexture.FULL_BRIGHT
                );

                pose.popPose();
            }
        }

        for (DamageNumber damage : DAMAGES) {
            float age = tick - damage.bornTick + partialTick;
            float progress = age / DAMAGE_LIFETIME;

            if (progress < 0.0F || progress >= 1.0F) {
                continue;
            }

            double y = damage.y + progress * 0.85;

            double dx = damage.x - cameraPos.x;
            double dy = y - cameraPos.y;
            double dz = damage.z - cameraPos.z;

            if (dx * dx + dy * dy + dz * dz > RANGE_SQR) {
                continue;
            }

            if (!event.getFrustum().isVisible(new AABB(
                    damage.x - 0.75, y - 0.5, damage.z - 0.75,
                    damage.x + 0.75, y + 0.5, damage.z + 0.75
            ))) {
                continue;
            }

            // 前半段保持清晰，后半段淡出。
            float opacity = progress <= 0.45F
                    ? 1.0F
                    : (1.0F - progress) / 0.55F;

            int alpha = Mth.clamp((int) (opacity * 255.0F), 0, 255);

            // Font 对非常小的 alpha 有特殊处理，直接跳过。
            if (alpha < 8) {
                continue;
            }

            // 出现时稍大，随后回落。
            float pop = 1.0F + 0.3F * Math.max(0.0F, 1.0F - age / 5.0F);
            float scale = 0.03F * pop;

            pose.pushPose();
            pose.translate(dx, dy, dz);
            pose.mulPose(camera.rotation());
            pose.scale(scale, -scale, scale);

            MC.font.drawInBatch(
                    damage.text,
                    -damage.width * 0.5F + damage.drift * progress,
                    0.0F,
                    (alpha << 24) | 0x00FF6655,
                    false,
                    pose.last().pose(),
                    BUFFERS,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    LightTexture.FULL_BRIGHT
            );


            pose.popPose();
        }

        BUFFERS.endBatch();
    }
    private static void drawHealthBar(
            VertexConsumer vertices,
            Matrix4f matrix,
            float ratio,
            float trailRatio
    ) {
        final float left = -HALF_WIDTH;
        final float right = HALF_WIDTH;
        final float top = 0.0F;
        final float bottom = BAR_HEIGHT;
        final float border = 1.0F;
        final float z = 0.0F;

        final int borderColor = 0xFF11151B;
        final int emptyColor = 0xFF30363F;
        final int trailColor = 0xFFFFC078;

        ratio = Mth.clamp(ratio, 0.0F, 1.0F);
        trailRatio = Mth.clamp(trailRatio, ratio, 1.0F);

        float fillEnd = left + (right - left) * ratio;
        float trailEnd = left + (right - left) * trailRatio;

        // 边框只画四周，不再用一整块底板覆盖内部区域。
        // 上、下边框包含四个角，左、右边框只覆盖中段。
        quad(vertices, matrix,
                left - border, top - border,
                right + border, top,
                z, borderColor);

        quad(vertices, matrix,
                left - border, bottom,
                right + border, bottom + border,
                z, borderColor);

        quad(vertices, matrix,
                left - border, top,
                left, bottom,
                z, borderColor);

        quad(vertices, matrix,
                right, top,
                right + border, bottom,
                z, borderColor);

        // 内部三个区域首尾相接，彼此不覆盖。
        if (fillEnd > left) {
            quad(vertices, matrix,
                    left, top,
                    fillEnd, bottom,
                    z, healthColor(ratio));
        }

        if (trailEnd > fillEnd) {
            quad(vertices, matrix,
                    fillEnd, top,
                    trailEnd, bottom,
                    z, trailColor);
        }

        if (right > trailEnd) {
            quad(vertices, matrix,
                    trailEnd, top,
                    right, bottom,
                    z, emptyColor);
        }
    }

    private static void pushBillboard(
            PoseStack pose,
            Camera camera,
            Vec3 cameraPos,
            LivingEntity entity,
            float partialTick
    ) {
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY());
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());

        pose.pushPose();
        pose.translate(
                x - cameraPos.x,
                y + entity.getBbHeight()*1.42f - cameraPos.y,
                z - cameraPos.z
        );
        pose.mulPose(camera.rotation());
        pose.scale(BAR_SCALE, -BAR_SCALE, BAR_SCALE);
    }

    private static void quad(
            VertexConsumer vertices,
            Matrix4f matrix,
            float left,
            float top,
            float right,
            float bottom,
            float z,
            int color
    ) {
        vertices.addVertex(matrix, left, top, z).setColor(color);
        vertices.addVertex(matrix, left, bottom, z).setColor(color);
        vertices.addVertex(matrix, right, bottom, z).setColor(color);
        vertices.addVertex(matrix, right, top, z).setColor(color);
    }

    private static int healthColor(float ratio) {
        int red;
        int green;

        if (ratio >= 0.5F) {
            float t = (ratio - 0.5F) * 2.0F;
            red = (int) Mth.lerp(t, 245.0F, 65.0F);
            green = (int) Mth.lerp(t, 195.0F, 220.0F);
        } else {
            float t = ratio * 2.0F;
            red = 245;
            green = (int) Mth.lerp(t, 65.0F, 195.0F);
        }

        return 0xFF000000 | (red << 16) | (green << 8) | 65;
    }

    private static String formatDamage(float amount) {
        int tenths = Math.round(amount * 10.0F);

        // 极小但可见的血量变化，至少显示 0.1。
        if (tenths <= 0) {
            tenths = 1;
        }

        if (tenths % 10 == 0) {
            return Integer.toString(tenths / 10);
        }

        return (tenths / 10) + "." + (tenths % 10);
    }

    private static final class HealthState {
        private final LivingEntity entity;

        private float health;
        private float maxHealth;
        private float trail;
        private float previousTrail;

        private int lastSeenTick;

        private String label = "";
        private int labelWidth;
        private int labelHealth = Integer.MIN_VALUE;
        private int labelMaxHealth = Integer.MIN_VALUE;

        private HealthState(
                LivingEntity entity,
                float health,
                float maxHealth,
                int tick
        ) {
            this.entity = entity;
            this.health = health;
            this.maxHealth = maxHealth;
            this.trail = health;
            this.previousTrail = health;
            this.lastSeenTick = tick;

            if (SHOW_HEALTH_TEXT) {
                updateLabel();
            }
        }

        private void updateLabel() {
            int current = Mth.ceil(health);

            if (current == labelHealth ) {
                return;
            }

            labelHealth = current;
            label = ""+current ;
            labelWidth = MC.font.width(label);
        }
    }

    private static final class DamageNumber {
        private final double x;
        private final double y;
        private final double z;

        private final String text;
        private final int width;
        private final int bornTick;
        private final float drift;

        private DamageNumber(
                double x,
                double y,
                double z,
                String text,
                int bornTick,
                float drift
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
            this.width = MC.font.width(text);
            this.bornTick = bornTick;
            this.drift = drift;
        }
    }

}
