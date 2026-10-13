package com.example.examplemod.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.data.LHTagGen;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MobOverheadRenderer {

    private static final double MAX_DISTANCE = 20.0D;
    private static final double TRAIT_DISTANCE = 20.0D;

    // 视线最多允许穿过三个有碰撞形状的方块。
    private static final int MAX_OCCLUDING_BLOCKS = 2;

    private static final boolean TRAITS_ONLY_WHEN_HOVERED = false;
    private static final boolean SHOW_LEVEL = true;
    private static final boolean SHOW_TRAITS = true;

    private static final float SCALE = 0.022F;
    private static final double HEIGHT_OFFSET = 0.25D;

    private static final int MIN_WIDTH = 100;
    private static final int MAX_WIDTH = 150;

    private static final int TRAIT_WRAP_WIDTH = MAX_WIDTH - 8;
    private static final int MAX_TRAIT_LINES = 3;

    private static final float BAR_HEIGHT = 4.0F;
    private static final float BORDER = 1.0F;

    private static final int ROW_GAP = 8;
    private static final int TRAIT_LINE_GAP = 2;

    // 视觉与配色优化：纯白文字 + 同色系面板与血条槽
    private static final int HP_TEXT_COLOR = 0xFFFFFFFF;
    private static final int TRAIT_TEXT_COLOR = 0xFFCCD3DE;

    private static final int PANEL_COLOR = 0x500C1017;         // 较通透的大底板 (31% 不透明)
    private static final int BORDER_COLOR = 0xFF4A5568;       // 清晰的亮金属边框
    private static final int BAR_BACKGROUND_COLOR = 0xE00C1017; // 较浓郁的同色系血条槽 (88% 不透明，确保血条高对比)

    private MobOverheadRenderer() {
    }

    /*
     * 由 L2ClientEventsMixin 调用。
     * 不要再为本类注册 RenderLivingEvent.Post 监听。
     */
    public static void renderOverhead(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Mob entity)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || !entity.isAlive()
                || entity.isInvisibleTo(minecraft.player)) {
            return;
        }

        float partialTick = event.getPartialTick();

        Vec3 cameraPosition = minecraft.gameRenderer
                .getMainCamera()
                .getPosition();

        Vec3 displayPosition = entity instanceof EnderDragon dragon
                ? dragon.head.getPosition(partialTick)
                : entity.getPosition(partialTick);

        double distanceSquared = cameraPosition.distanceToSqr(
                displayPosition
        );

        if (distanceSquared > MAX_DISTANCE * MAX_DISTANCE) {
            return;
        }

        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();

        if (!Float.isFinite(health)
                || !Float.isFinite(maxHealth)
                || maxHealth <= 0.0F) {
            return;
        }

        Vec3 anchor = getAnchor(entity, partialTick);

        // 遮挡检测终点与实际绘制位置一致。
        Vec3 panelPosition = entity.getPosition(partialTick).add(
                anchor.x,
                anchor.y + HEIGHT_OFFSET,
                anchor.z
        );

        if (!canSeeThroughBlocks(
                entity.level(),
                cameraPosition,
                panelPosition,
                entity
        )) {
            return;
        }

        Font font = minecraft.font;
        MobTraitCap cap = getCapability(entity);

        List<Component> levelTitles = List.of();
        List<Component> traitTitles = List.of();

        if (cap != null) {
            if (SHOW_LEVEL
                    && !entity.getType().is(LHTagGen.HIDE_LEVEL)) {
                levelTitles = cap.getTitle(true, false);
            }

            boolean showTraits = SHOW_TRAITS
                    && !entity.getType().is(LHTagGen.HIDE_TRAITS)
                    && distanceSquared <= TRAIT_DISTANCE * TRAIT_DISTANCE
                    && (!TRAITS_ONLY_WHEN_HOVERED
                    || minecraft.crosshairPickEntity == entity);

            if (showTraits) {
                traitTitles = cap.getTitle(false, true);
            }
        }

        Component levelText = joinComponents(levelTitles, " ");

        String healthText = formatHealth(health)
                + " / "
                + formatHealth(maxHealth);

        List<FormattedCharSequence> traitLines = wrapTraits(
                font,
                traitTitles
        );

        int levelWidth = font.width(levelText);
        int healthWidth = font.width(healthText);

        int infoWidth = levelWidth
                + healthWidth
                + (levelWidth > 0 ? ROW_GAP : 0);

        int width = Math.max(MIN_WIDTH, infoWidth + 8);

        for (FormattedCharSequence line : traitLines) {
            width = Math.max(width, font.width(line) + 8);
        }

        width = Math.min(width, MAX_WIDTH);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();

        try {
            // 事件矩阵已经位于实体渲染原点，只添加局部偏移。
            pose.translate(
                    anchor.x,
                    anchor.y + HEIGHT_OFFSET,
                    anchor.z
            );

            pose.mulPose(
                    minecraft.getEntityRenderDispatcher()
                            .cameraOrientation()
            );

            pose.scale(SCALE, -SCALE, SCALE);

            renderPanel(
                    pose,
                    event.getMultiBufferSource(),
                    font,
                    width,
                    levelText,
                    healthText,
                    traitLines,
                    Mth.clamp(health / maxHealth, 0.0F, 1.0F),
                    isHostile(entity)
            );
        } finally {
            pose.popPose();
        }
    }

    private static Vec3 getAnchor(
            LivingEntity entity,
            float partialTick
    ) {
        if (entity instanceof EnderDragon dragon) {
            Vec3 bodyPosition = dragon.getPosition(partialTick);
            Vec3 headPosition = dragon.head.getPosition(partialTick);

            return headPosition.subtract(bodyPosition).add(
                    0.0D,
                    dragon.head.getBbHeight() + 0.5D,
                    0.0D
            );
        }

        Vec3 anchor = entity.getAttachments().getNullable(
                EntityAttachment.NAME_TAG,
                0,
                entity.getViewYRot(partialTick)
        );

        if (anchor != null) {
            return anchor;
        }

        return new Vec3(
                0.0D,
                entity.getBoundingBox().getYsize(),
                0.0D
        );
    }

    /*
     * 只检查方块碰撞形状，不检查实体。
     * 沿视线逐格遍历，每个相交方块只计数一次。
     */
    private static boolean canSeeThroughBlocks(
            Level level,
            Vec3 from,
            Vec3 to,
            LivingEntity entity
    ) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        if (dx * dx + dy * dy + dz * dz < 1.0E-12D) {
            return true;
        }

        int x = Mth.floor(from.x);
        int y = Mth.floor(from.y);
        int z = Mth.floor(from.z);

        int endX = Mth.floor(to.x);
        int endY = Mth.floor(to.y);
        int endZ = Mth.floor(to.z);

        int stepX = dx > 0.0D ? 1 : dx < 0.0D ? -1 : 0;
        int stepY = dy > 0.0D ? 1 : dy < 0.0D ? -1 : 0;
        int stepZ = dz > 0.0D ? 1 : dz < 0.0D ? -1 : 0;

        double deltaX = stepX == 0
                ? Double.POSITIVE_INFINITY
                : 1.0D / Math.abs(dx);

        double deltaY = stepY == 0
                ? Double.POSITIVE_INFINITY
                : 1.0D / Math.abs(dy);

        double deltaZ = stepZ == 0
                ? Double.POSITIVE_INFINITY
                : 1.0D / Math.abs(dz);

        double nextX = firstBoundary(from.x, x, dx, stepX);
        double nextY = firstBoundary(from.y, y, dy, stepY);
        double nextZ = firstBoundary(from.z, z, dz, stepZ);

        CollisionContext context = CollisionContext.of(entity);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int occludingBlocks = 0;

        while (true) {
            pos.set(x, y, z);

            if (!level.hasChunkAt(pos)) {
                return false;
            }

            VoxelShape shape = level.getBlockState(pos)
                    .getCollisionShape(level, pos, context);

            if (!shape.isEmpty()
                    && shape.clip(from, to, pos) != null) {
                occludingBlocks++;

                if (occludingBlocks > MAX_OCCLUDING_BLOCKS) {
                    return false;
                }
            }

            if (x == endX && y == endY && z == endZ) {
                return true;
            }

            double next = Math.min(
                    nextX,
                    Math.min(nextY, nextZ)
            );

            if (!Double.isFinite(next) || next > 1.0D) {
                return true;
            }

            // 同时经过格子边或角时，同时推进对应坐标。
            if (nextX <= next) {
                x += stepX;
                nextX += deltaX;
            }

            if (nextY <= next) {
                y += stepY;
                nextY += deltaY;
            }

            if (nextZ <= next) {
                z += stepZ;
                nextZ += deltaZ;
            }
        }
    }

    private static double firstBoundary(
            double origin,
            int block,
            double direction,
            int step
    ) {
        if (step == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double boundary = step > 0
                ? block + 1.0D
                : block;

        return (boundary - origin) / direction;
    }

    private static boolean isHostile(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return false;
        }

        LivingEntity target = mob.getTarget();

        return target != null
                && target.isAlive()
                && !target.isRemoved();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MobTraitCap getCapability(LivingEntity entity) {
        // 只读取已有数据，没有 L2 数据仍显示血条。
        Optional<?> optional =
                ((GeneralCapabilityHolder) LHMiscs.MOB.type())
                        .getExisting(entity);

        if (optional.isPresent()
                && optional.get() instanceof MobTraitCap cap) {
            return cap;
        }

        return null;
    }

    private static Component joinComponents(
            List<Component> components,
            String separator
    ) {
        var result = Component.empty();

        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(separator));
            }

            result.append(components.get(i));
        }

        return result;
    }

    private static List<FormattedCharSequence> wrapTraits(
            Font font,
            List<Component> titles
    ) {
        if (titles.isEmpty()) {
            return List.of();
        }

        Component text = joinComponents(titles, "  ·  ");

        List<FormattedCharSequence> wrapped = font.split(
                text,
                TRAIT_WRAP_WIDTH
        );

        if (wrapped.size() <= MAX_TRAIT_LINES) {
            return wrapped;
        }

        List<FormattedCharSequence> result = new ArrayList<>(
                wrapped.subList(0, MAX_TRAIT_LINES - 1)
        );

        result.add(
                Component.literal("…").getVisualOrderText()
        );

        return result;
    }

    private static void renderPanel(
            PoseStack pose,
            MultiBufferSource buffers,
            Font font,
            int width,
            Component levelText,
            String healthText,
            List<FormattedCharSequence> traitLines,
            float ratio,
            boolean hostile
    ) {
        float left = -width * 0.5F;
        float right = width * 0.5F;

        float infoY = -font.lineHeight - 3.0F;

        float traitStartY = infoY
                - 3.0F
                - traitLines.size()
                * (font.lineHeight + TRAIT_LINE_GAP);

        float panelTop = traitLines.isEmpty()
                ? infoY - 3.0F
                : traitStartY - 3.0F;

        Matrix4f matrix = pose.last().pose();

        VertexConsumer vertices = buffers.getBuffer(
                RenderType.GUI
        );

        rect(
                vertices,
                matrix,
                left - 4.0F,
                panelTop,
                right + 4.0F,
                BAR_HEIGHT + 3.0F,
                -0.02F,
                PANEL_COLOR
        );

        rect(
                vertices,
                matrix,
                left - BORDER,
                -BORDER,
                right + BORDER,
                BAR_HEIGHT + BORDER,
                0.0F,
                BORDER_COLOR
        );

        rect(
                vertices,
                matrix,
                left,
                0.0F,
                right,
                BAR_HEIGHT,
                0.01F,
                BAR_BACKGROUND_COLOR
        );

        if (ratio > 0.0F) {
            rect(
                    vertices,
                    matrix,
                    left,
                    0.0F,
                    left + width * ratio,
                    BAR_HEIGHT,
                    0.02F,
                    healthColor(ratio, hostile)
            );
        }

        int levelWidth = font.width(levelText);
        int healthWidth = font.width(healthText);

        int totalWidth = levelWidth
                + healthWidth
                + (levelWidth > 0 ? ROW_GAP : 0);

        float infoScale = totalWidth > width
                ? (float) width / totalWidth
                : 1.0F;

        pose.pushPose();

        try {
            pose.translate(0.0F, infoY, 0.03F);
            pose.scale(infoScale, infoScale, 1.0F);

            float scaledLeft = left / infoScale;
            float scaledRight = right / infoScale;

            if (levelWidth > 0) {
                font.drawInBatch(
                        levelText,
                        scaledLeft,
                        0.0F,
                        HP_TEXT_COLOR,
                        false,
                        pose.last().pose(),
                        buffers,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        LightTexture.FULL_BRIGHT
                );
            }

            float healthX = levelWidth > 0
                    ? scaledRight - healthWidth
                    : -healthWidth * 0.5F;

            font.drawInBatch(
                    healthText,
                    healthX,
                    0.0F,
                    HP_TEXT_COLOR,
                    false,
                    pose.last().pose(),
                    buffers,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    LightTexture.FULL_BRIGHT
            );
        } finally {
            pose.popPose();
        }

        pose.pushPose();

        try {
            pose.translate(0.0F, 0.0F, 0.03F);

            for (int i = 0; i < traitLines.size(); i++) {
                FormattedCharSequence line = traitLines.get(i);

                font.drawInBatch(
                        line,
                        -font.width(line) * 0.5F,
                        traitStartY
                                + i * (font.lineHeight + TRAIT_LINE_GAP),
                        TRAIT_TEXT_COLOR,
                        false,
                        pose.last().pose(),
                        buffers,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        LightTexture.FULL_BRIGHT
                );
            }
        } finally {
            pose.popPose();
        }
    }

    private static void rect(
            VertexConsumer vertices,
            Matrix4f matrix,
            float left,
            float top,
            float right,
            float bottom,
            float z,
            int color
    ) {
        vertices.addVertex(matrix, left, top, z)
                .setColor(color)
                .setLight(LightTexture.FULL_BRIGHT);

        vertices.addVertex(matrix, left, bottom, z)
                .setColor(color)
                .setLight(LightTexture.FULL_BRIGHT);

        vertices.addVertex(matrix, right, bottom, z)
                .setColor(color)
                .setLight(LightTexture.FULL_BRIGHT);

        vertices.addVertex(matrix, right, top, z)
                .setColor(color)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private static int healthColor(
            float ratio,
            boolean hostile
    ) {
        return hostile
                ? 0xFFFF3344   // 更醒目的亮红
                : 0xFF22DD55;  // 更醒目的亮绿
    }

    private static int mixColor(
            int from,
            int to,
            float progress
    ) {
        int red = Math.round(Mth.lerp(
                progress,
                (float) ((from >> 16) & 0xFF),
                (float) ((to >> 16) & 0xFF)
        ));

        int green = Math.round(Mth.lerp(
                progress,
                (float) ((from >> 8) & 0xFF),
                (float) ((to >> 8) & 0xFF)
        ));

        int blue = Math.round(Mth.lerp(
                progress,
                (float) (from & 0xFF),
                (float) (to & 0xFF)
        ));

        return (red << 16) | (green << 8) | blue;
    }

    private static String formatHealth(double value) {
        value = Math.max(0.0D, value);

        if (value < 10_000.0D) {
            return Long.toString(Math.round(value));
        }

        if (value < 100_000_000.0D) {
            return formatUnit(value / 10_000.0D, "万");
        }

        if (value < 1_000_000_000_000.0D) {
            return formatUnit(value / 100_000_000.0D, "亿");
        }

        return formatUnit(
                value / 1_000_000_000_000.0D,
                "万亿"
        );
    }

    private static String formatUnit(
            double value,
            String unit
    ) {
        String text = String.format(
                Locale.ROOT,
                "%.1f",
                value
        );

        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }

        return text + unit;
    }
}