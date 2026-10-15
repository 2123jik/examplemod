package com.nf121.examplemod.client;

import com.nf121.examplemod.ExampleMod;
import com.nf121.examplemod.event.XpFlights;
import com.nf121.examplemod.registry.ModPayloads;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(
        modid = ExampleMod.MOD_ID,
        value = Dist.CLIENT
)
public final class XpFlightClient {
    private static final Map<Integer, Flight> FLIGHTS = new HashMap<>();
    private static final Map<GroupKey, Group> GROUPS = new HashMap<>();
    private static final ArrayList<Group> DRAW_GROUPS = new ArrayList<>();

    private static final double CELL_SIZE = 2.0;
    private static final int PROGRESS_BUCKETS = 8;

    private static final int SEGMENTS = 6;
    private static final int MAX_DRAW_GROUPS = 192;

    private static final double TRAIL_LENGTH = 0.28;
    private static final double MAX_DISTANCE_SQR = 96.0 * 96.0;

    private static ClientLevel activeLevel;

    private XpFlightClient() {
    }

    public static void receive(ModPayloads.XpFlightPayload packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        ensureLevel(level);

        if (packet.duration() <= 0) {
            FLIGHTS.remove(packet.orbId());
            return;
        }

        FLIGHTS.put(packet.orbId(), new Flight(
                packet.playerId(),
                packet.pos(),
                packet.startTick(),
                packet.duration()
        ));
    }

    @SubscribeEvent
    public static void onLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        clear();
        activeLevel = null;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null) {
            return;
        }

        ensureLevel(level);

        float partialTick =
                event.getPartialTick().getGameTimeDeltaPartialTick(false);

        double now = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();

        GROUPS.clear();
        DRAW_GROUPS.clear();

        var iterator = FLIGHTS.values().iterator();

        while (iterator.hasNext()) {
            Flight flight = iterator.next();

            double progress =
                    (now - flight.startTick) / flight.duration;

            // 视觉飞行结束就移除记录。
            // 服务端仍可继续处理合并经验球的原版拾取冷却。
            if (progress >= 1.0) {
                iterator.remove();
                continue;
            }

            if (progress < 0.0) {
                continue;
            }

            Entity target = level.getEntity(flight.playerId);
            if (target == null) {
                continue;
            }

            Vec3 end = target.getPosition(partialTick)
                    .add(0.0, 0.6, 0.0);

            Vec3 head = XpFlights.curve(
                    flight.start, end, progress
            );

            if (head.distanceToSqr(camera) > MAX_DISTANCE_SQR) {
                continue;
            }

            GroupKey key = new GroupKey(
                    flight.playerId,
                    Mth.floor(flight.start.x / CELL_SIZE),
                    Mth.floor(flight.start.y / CELL_SIZE),
                    Mth.floor(flight.start.z / CELL_SIZE),
                    Math.min(
                            PROGRESS_BUCKETS - 1,
                            (int) (progress * PROGRESS_BUCKETS)
                    )
            );

            Group group = GROUPS.get(key);

            if (group == null) {
                group = new Group(end);
                GROUPS.put(key, group);
            }

            group.add(flight.start, progress);
        }

        if (GROUPS.isEmpty()) {
            return;
        }

        for (Group group : GROUPS.values()) {
            group.finish(camera);
            DRAW_GROUPS.add(group);
        }

        // 超出预算时优先显示附近经验流。
        DRAW_GROUPS.sort(
                Comparator.comparingDouble(group -> group.distanceSqr)
        );

        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();

        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();

        RenderType type = RenderType.lightning();
        VertexConsumer vertices = buffers.getBuffer(type);

        int count = Math.min(MAX_DRAW_GROUPS, DRAW_GROUPS.size());

        for (int i = 0; i < count; i++) {
            drawGroup(
                    DRAW_GROUPS.get(i),
                    camera,
                    matrix,
                    vertices
            );
        }

        buffers.endBatch(type);
    }

    private static void drawGroup(
            Group group,
            Vec3 camera,
            Matrix4f matrix,
            VertexConsumer vertices
    ) {
        double headT = group.progress;
        double tailT = Math.max(0.0, headT - TRAIL_LENGTH);

        // 球越集中，经验流越宽；设上限防止遮挡过大。
        double width = Math.min(
                0.14,
                0.035 + Math.sqrt(group.count) * 0.012
        );

        double fade = 1.0 - smoothstep(0.85, 1.0, headT);

        for (int i = 0; i < SEGMENTS; i++) {
            double f0 = (double) i / SEGMENTS;
            double f1 = (double) (i + 1) / SEGMENTS;

            Vec3 a = XpFlights.curve(
                    group.start,
                    group.end,
                    lerp(tailT, headT, f0)
            );

            Vec3 b = XpFlights.curve(
                    group.start,
                    group.end,
                    lerp(tailT, headT, f1)
            );

            Vec3 direction = b.subtract(a);
            if (direction.lengthSqr() < 1.0E-10) {
                continue;
            }

            Vec3 middle = a.add(b).scale(0.5);
            Vec3 side = direction.cross(camera.subtract(middle));

            if (side.lengthSqr() < 1.0E-10) {
                side = direction.cross(new Vec3(0.0, 1.0, 0.0));
            }

            if (side.lengthSqr() < 1.0E-10) {
                side = direction.cross(new Vec3(1.0, 0.0, 0.0));
            }

            side = side.normalize();

            Vec3 side0 = side.scale(width * f0);
            Vec3 side1 = side.scale(width * f1);

            int alpha0 = (int) (220.0 * f0 * fade);
            int alpha1 = (int) (220.0 * f1 * fade);

            vertex(
                    vertices, matrix,
                    a.subtract(side0).subtract(camera), alpha0
            );
            vertex(
                    vertices, matrix,
                    a.add(side0).subtract(camera), alpha0
            );
            vertex(
                    vertices, matrix,
                    b.add(side1).subtract(camera), alpha1
            );
            vertex(
                    vertices, matrix,
                    b.subtract(side1).subtract(camera), alpha1
            );
        }
    }

    private static void vertex(
            VertexConsumer vertices,
            Matrix4f matrix,
            Vec3 position,
            int alpha
    ) {
        vertices.addVertex(
                matrix,
                (float) position.x,
                (float) position.y,
                (float) position.z
        ).setColor(170, 255, 90, alpha);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double a, double b, double value) {
        double t = Math.clamp((value - a) / (b - a), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static void ensureLevel(ClientLevel level) {
        if (activeLevel != level) {
            clear();
            activeLevel = level;
        }
    }

    private static void clear() {
        FLIGHTS.clear();
        GROUPS.clear();
        DRAW_GROUPS.clear();
    }

    private record Flight(
            int playerId,
            Vec3 start,
            long startTick,
            int duration
    ) {
    }

    private record GroupKey(
            int playerId,
            int cellX,
            int cellY,
            int cellZ,
            int progressBucket
    ) {
    }

    private static final class Group {
        private final Vec3 end;

        private double sumX;
        private double sumY;
        private double sumZ;
        private double sumProgress;
        private int count;

        private Vec3 start;
        private double progress;
        private double distanceSqr;

        private Group(Vec3 end) {
            this.end = end;
        }

        private void add(Vec3 position, double progress) {
            sumX += position.x;
            sumY += position.y;
            sumZ += position.z;
            sumProgress += progress;
            count++;
        }

        private void finish(Vec3 camera) {
            start = new Vec3(
                    sumX / count,
                    sumY / count,
                    sumZ / count
            );

            progress = sumProgress / count;

            distanceSqr = XpFlights.curve(
                    start, end, progress
            ).distanceToSqr(camera);
        }
    }
}
