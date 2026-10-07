package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.XpFlightPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public final class XpFlights {
    private static final double RANGE = 32.0;
    private static final double RANGE_SQUARED = RANGE * RANGE;

    // 抵达后最多等待 30 秒。
    // 若其他模组持续阻止拾取，释放经验球，不销毁经验。
    private static final int MAX_SETTLE_TICKS = 20 * 30;

    private static final Map<ExperienceOrb, Flight> FLIGHTS =
            new IdentityHashMap<>();

    // 只有这里主动执行原版拾取时，才允许结算被管理的经验球。
    private static ExperienceOrb settlingOrb;

    private XpFlights() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ExperienceOrb orb)
                || !(orb.level() instanceof ServerLevel level)
                || orb.isRemoved()) {
            return;
        }

        Flight flight = FLIGHTS.get(orb);

        if (flight == null) {
            ServerPlayer target = findNearestPlayer(level, orb.position());
            if (target == null) {
                return;
            }

            double distance = orb.position().distanceTo(target.position());

            int duration = Math.max(
                    8,
                    Math.min(24, (int) Math.ceil(distance / 1.4))
            );

            flight = new Flight(
                    target,
                    orb.position(),
                    level.getGameTime(),
                    duration
            );

            FLIGHTS.put(orb, flight);
            broadcast(level, startPacket(orb, flight));
        }

        if (!isValidTarget(level, flight.target)) {
            FLIGHTS.remove(orb);
            broadcast(level, XpFlightPayload.stop(orb.getId()));
            return;
        }

        // 不执行原版经验球的重力、吸引、移动及合并逻辑。
        event.setCanceled(true);

        long elapsed = level.getGameTime() - flight.startTick;

// 原版实体保持在起点。
// 客户端独立绘制轨迹，服务端不用逐 tick 移动实体。
        orb.setDeltaMovement(Vec3.ZERO);

        if (elapsed < flight.duration) {
            return;
        }

        if (elapsed > flight.duration + MAX_SETTLE_TICKS) {
            FLIGHTS.remove(orb);
            broadcast(level, XpFlightPayload.stop(orb.getId()));
            return;
        }

// 原版 playerTouch 不负责校验接触距离。
// 这里由服务端的飞行时间与目标有效性决定何时允许结算。
        settlingOrb = orb;
        try {
            orb.playerTouch(flight.target);
        } finally {
            settlingOrb = null;
        }

    }

    @SubscribeEvent
    public static void onPickup(PlayerXpEvent.PickupXp event) {
        ExperienceOrb orb = event.getOrb();

        if (!FLIGHTS.containsKey(orb)) {
            return;
        }

        Flight flight = FLIGHTS.get(orb);

        if (settlingOrb != orb || event.getEntity() != flight.target) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<ExperienceOrb, Flight>> iterator =
                FLIGHTS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ExperienceOrb, Flight> entry = iterator.next();

            ExperienceOrb orb = entry.getKey();
            Flight flight = entry.getValue();

            if (!(orb.level() instanceof ServerLevel level)) {
                iterator.remove();
                continue;
            }

            if (orb.isRemoved() || !isValidTarget(level, flight.target)) {
                iterator.remove();
                broadcast(level, XpFlightPayload.stop(orb.getId()));
            }
        }
    }

    // 玩家在轨迹已经开始后才追踪到经验球时，补发当前轨迹。
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof ExperienceOrb orb)) {
            return;
        }

        Flight flight = FLIGHTS.get(orb);

        if (flight != null) {
            PacketDistributor.sendToPlayer(
                    player, startPacket(orb, flight)
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FLIGHTS.clear();
        settlingOrb = null;
    }

    private static ServerPlayer findNearestPlayer(
            ServerLevel level,
            Vec3 position
    ) {
        ServerPlayer nearest = null;
        double nearestDistance = RANGE_SQUARED;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            double distance = player.position().distanceToSqr(position);

            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    private static boolean isValidTarget(
            ServerLevel level,
            ServerPlayer player
    ) {
        return player.isAlive()
                && !player.isSpectator()
                && player.level() == level
                && level.getServer().getPlayerList()
                        .getPlayer(player.getUUID()) == player;
    }

    private static XpFlightPayload startPacket(
            ExperienceOrb orb,
            Flight flight
    ) {
        return new XpFlightPayload(
                orb.getId(),
                flight.target.getId(),
                flight.start.x,
                flight.start.y,
                flight.start.z,
                flight.startTick,
                flight.duration
        );
    }

    private static void broadcast(
            ServerLevel level,
            XpFlightPayload payload
    ) {
        PacketDistributor.sendToPlayersInDimension(level, payload);
    }

    // 服务端位置与客户端显示共用同一条二次贝塞尔曲线。
    public static Vec3 curve(Vec3 start, Vec3 end, double t) {
        double lift = Math.min(3.0, 0.8 + start.distanceTo(end) * 0.12);

        Vec3 control = start.lerp(end, 0.45).add(0.0, lift, 0.0);

        double u = 1.0 - t;

        return start.scale(u * u)
                .add(control.scale(2.0 * u * t))
                .add(end.scale(t * t));
    }

    private static final class Flight {
        final ServerPlayer target;
        final Vec3 start;
        final long startTick;
        final int duration;

        Flight(
                ServerPlayer target,
                Vec3 start,
                long startTick,
                int duration
        ) {
            this.target = target;
            this.start = start;
            this.startTick = startTick;
            this.duration = duration;
        }
    }
}
