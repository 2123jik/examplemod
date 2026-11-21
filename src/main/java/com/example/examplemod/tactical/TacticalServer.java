package com.example.examplemod.tactical;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "examplemod")
public class TacticalServer {

    private static final Map<UUID, BattleSession> PLAYER_SESSIONS = new ConcurrentHashMap<>();
    private static final Set<UUID> FROZEN_ENTITIES = ConcurrentHashMap.newKeySet();
    private static boolean resolvingTacticalDamage = false;

    static class BattleSession {
        final ServerPlayer owner;
        final ServerLevel level;
        final BlockPos center;
        final int radius = 10;
        final List<UUID> turnOrder = new ArrayList<>();
        final Map<UUID, Integer> teams = new HashMap<>(); // 0=友方, 1=敌方
        final Map<UUID, Integer> apMap = new HashMap<>();
        int turnIdx = 0;

        BattleSession(ServerPlayer owner, BlockPos center) {
            this.owner = owner;
            this.level = owner.serverLevel();
            this.center = center;
        }

        UUID currentUnit() {
            return turnOrder.isEmpty() ? null : turnOrder.get(turnIdx % turnOrder.size());
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!event.getEntity().level().isClientSide && FROZEN_ENTITIES.contains(event.getEntity().getUUID())) {
            if (!(event.getEntity() instanceof ServerPlayer)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (resolvingTacticalDamage) return;
        UUID id = event.getEntity().getUUID();
        if (FROZEN_ENTITIES.contains(id) || PLAYER_SESSIONS.containsKey(id)) {
            event.setCanceled(true);
        }
    }

    public static void handleAction(TacticalNet.C2SAction action, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;

        if (action.op() == 0) {
            if (PLAYER_SESSIONS.containsKey(player.getUUID())) {
                endSession(player);
            } else {
                startSession(player);
            }
            return;
        }

        BattleSession session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null) return;
        UUID curId = session.currentUnit();
        if (curId == null || !Objects.equals(curId, action.unitId()) || session.teams.getOrDefault(curId, 1) != 0) return;

        LivingEntity actor = findLiving(session.level, curId);
        if (actor == null) { nextTurn(session); return; }

        int ap = session.apMap.getOrDefault(curId, 0);
        if (action.op() == 1 && ap >= 1) { // 移动（若目标为墙体/深渊返回 null 则不移动不扣AP）
            BlockPos target = findFloorAround(session.level, action.targetPos().getX(), actor.blockPosition().getY(), action.targetPos().getZ(), 2);
            if (target != null) {
                int dist = Math.abs(target.getX() - actor.blockPosition().getX()) + Math.abs(target.getZ() - actor.blockPosition().getZ());
                int moveRange = getMoveRange(actor);
                if (dist > 0 && dist <= moveRange) {
                    actor.teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                    session.apMap.put(curId, ap - 1);
                }
            }
        } else if (action.op() == 2 && ap >= 1) { // 攻击
            if (session.level.getEntity(action.targetEntityId()) instanceof LivingEntity target && target.isAlive()) {
                double range = actor.getAttributes().hasAttribute(Attributes.ENTITY_INTERACTION_RANGE)
                        ? actor.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) : 2.2;
                if (actor.distanceTo(target) <= Math.max(2.0, range) + 0.8) {
                    resolvingTacticalDamage = true;
                    try {
                        actor.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                        if (actor instanceof ServerPlayer sp) {
                            sp.attack(target);
                        } else {
                            actor.doHurtTarget(target);
                        }
                    } finally {
                        resolvingTacticalDamage = false;
                    }
                    session.apMap.put(curId, ap - 1);
                }
            }
        } else if (action.op() == 3) {
            nextTurn(session);
            return;
        }

        if (session.apMap.getOrDefault(curId, 0) <= 0) {
            nextTurn(session);
        } else {
            syncToClient(session, true);
        }
    }

    private static void startSession(ServerPlayer player) {
        BattleSession session = new BattleSession(player, player.blockPosition());
        AABB box = new AABB(session.center).inflate(session.radius, 6, session.radius);
        List<LivingEntity> list = session.level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);

        for (LivingEntity e : list) {
            boolean isAlly = (e == player) || (e instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID()));
            boolean isEnemy = !isAlly && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() == player));
            if (!isAlly && !isEnemy) continue;

            session.teams.put(e.getUUID(), isAlly ? 0 : 1);
            session.turnOrder.add(e.getUUID());
            if (e != player) FROZEN_ENTITIES.add(e.getUUID());
        }

        session.turnOrder.sort((a, b) -> {
            LivingEntity ea = findLiving(session.level, a);
            LivingEntity eb = findLiving(session.level, b);
            double sa = ea != null ? ea.getAttributeValue(Attributes.MOVEMENT_SPEED) : 0;
            double sb = eb != null ? eb.getAttributeValue(Attributes.MOVEMENT_SPEED) : 0;
            return Double.compare(sb, sa);
        });

        PLAYER_SESSIONS.put(player.getUUID(), session);
        if (!session.turnOrder.isEmpty()) {
            session.apMap.put(session.currentUnit(), 2);
            if (session.teams.get(session.currentUnit()) == 1) runEnemyTurns(session);
        }
        syncToClient(session, true);
    }

    private static void nextTurn(BattleSession session) {
        session.turnOrder.removeIf(id -> {
            LivingEntity e = findLiving(session.level, id);
            boolean dead = (e == null || !e.isAlive());
            if (dead) FROZEN_ENTITIES.remove(id);
            return dead;
        });

        boolean hasEnemy = session.turnOrder.stream().anyMatch(id -> session.teams.getOrDefault(id, 1) == 1);
        boolean hasAlly = session.turnOrder.stream().anyMatch(id -> session.teams.getOrDefault(id, 0) == 0);
        if (!hasEnemy || !hasAlly) {
            session.owner.sendSystemMessage(Component.literal(hasEnemy ? "§c战败撤退！" : "§a战斗胜利！"));
            endSession(session.owner);
            return;
        }

        session.turnIdx = (session.turnIdx + 1) % session.turnOrder.size();
        UUID next = session.currentUnit();
        session.apMap.put(next, 2);

        if (session.teams.get(next) == 1) {
            runEnemyTurns(session);
        } else {
            syncToClient(session, true);
        }
    }

    private static void runEnemyTurns(BattleSession session) {
        int guard = 0;
        while (guard++ < 20) {
            UUID cur = session.currentUnit();
            if (cur == null || session.teams.getOrDefault(cur, 0) == 0) break;

            LivingEntity enemy = findLiving(session.level, cur);
            LivingEntity nearestAlly = session.turnOrder.stream()
                    .filter(id -> session.teams.getOrDefault(id, 1) == 0)
                    .map(id -> findLiving(session.level, id))
                    .filter(Objects::nonNull)
                    .min(Comparator.comparingDouble(a -> enemy != null ? a.distanceToSqr(enemy) : Double.MAX_VALUE))
                    .orElse(null);

            if (enemy != null && nearestAlly != null) {
                int move = getMoveRange(enemy);
                BlockPos ep = enemy.blockPosition();
                BlockPos ap = nearestAlly.blockPosition();
                for (int step = 0; step < move; step++) {
                    if (enemy.distanceTo(nearestAlly) <= 2.2) break;
                    int dx = Integer.compare(ap.getX(), ep.getX());
                    int dz = Integer.compare(ap.getZ(), ep.getZ());

                    // 优先斜向/直向逼近，遇到地牢柱子自动尝试单轴绕行
                    BlockPos nextPos = findFloorAround(session.level, ep.getX() + dx, ep.getY(), ep.getZ() + dz, 2);
                    if (nextPos == null && dx != 0) nextPos = findFloorAround(session.level, ep.getX() + dx, ep.getY(), ep.getZ(), 2);
                    if (nextPos == null && dz != 0) nextPos = findFloorAround(session.level, ep.getX(), ep.getY(), ep.getZ() + dz, 2);
                    if (nextPos == null) break;

                    ep = nextPos;
                    enemy.teleportTo(ep.getX() + 0.5, ep.getY(), ep.getZ() + 0.5);
                }
                if (enemy.distanceTo(nearestAlly) <= 2.8) {
                    resolvingTacticalDamage = true;
                    try { enemy.doHurtTarget(nearestAlly); } finally { resolvingTacticalDamage = false; }
                }
            }

            session.turnIdx = (session.turnIdx + 1) % session.turnOrder.size();
            UUID next = session.currentUnit();
            if (next != null) session.apMap.put(next, 2);
        }
        syncToClient(session, true);
    }

    private static void endSession(ServerPlayer player) {
        BattleSession session = PLAYER_SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.turnOrder.forEach(FROZEN_ENTITIES::remove);
            PacketDistributor.sendToPlayer(player, new TacticalNet.S2CSyncBoard(false, session.center, session.radius, null, List.of()));
        }
    }

    private static void syncToClient(BattleSession session, boolean open) {
        List<TacticalNet.UnitView> views = new ArrayList<>();
        UUID cur = session.currentUnit();
        for (UUID id : session.turnOrder) {
            LivingEntity e = findLiving(session.level, id);
            if (e == null || !e.isAlive()) continue;
            views.add(new TacticalNet.UnitView(
                    id, e.getId(), e.getName().getString(), e.blockPosition(),
                    e.getHealth(), e.getMaxHealth(), session.teams.get(id),
                    getMoveRange(e), session.apMap.getOrDefault(id, 0), id.equals(cur)
            ));
        }
        PacketDistributor.sendToPlayer(session.owner, new TacticalNet.S2CSyncBoard(open, session.center, session.radius, cur, views));
    }

    private static int getMoveRange(LivingEntity e) {
        return Math.max(3, Math.min(8, (int) Math.round(e.getAttributeValue(Attributes.MOVEMENT_SPEED) * 40)));
    }

    /** 从 refY 向外按距离优先搜索可站立地面（0, +1, -1, +2, -2...），墙壁或深坑返回 null */
    @Nullable
    public static BlockPos findFloorAround(Level level, int x, int refY, int z, int maxDelta) {
        for (int d = 0; d <= maxDelta; d++) {
            if (isStandable(level, x, refY + d, z)) return new BlockPos(x, refY + d, z);
            if (d > 0 && isStandable(level, x, refY - d, z)) return new BlockPos(x, refY - d, z);
        }
        return null;
    }

    private static boolean isStandable(Level level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private static LivingEntity findLiving(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof LivingEntity le ? le : null;
    }
}