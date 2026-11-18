package com.example.examplemod.engine;

import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@EventBusSubscriber(modid = "examplemod")
public class SpellTransformEngine {

    private static final ResourceLocation TRANSFORM_ID =
            ResourceLocation.fromNamespaceAndPath("examplemod", "spell_transform");

    private static final Map<UUID, Vec3> LAST_SPELL_POS = new ConcurrentHashMap<>();

    public static class TransformState {
        public UUID ownerUuid;
        @Nullable public UUID followEntityUuid;
        public WandCompiler.CastNode sourceNode;

        public boolean orbit, freeOrbit, cursorHoming, endTp, midTp, midTpDone;
        public float orbitRadius = 3.5F, angleRad = 0.0F, angularSpeed = 0.12F;
        public float entityHomingDeg, selfHomingDeg;
        public int splitLeft, refractLeft;
        public float hoverSec;
        public boolean hoveringNow = false;
        public int hoverTicksLeft = 0;

        public int tickTriggerCount = 0;
        public long lastHitTriggerGameTime = 0L;
        public boolean expireTriggered = false;
    }

    public record CaptureContext(
            UUID ownerUuid,
            @Nullable Vec3 overrideSpawnPos,
            @Nullable UUID followEntityUuid,
            WandCompiler.CastNode node
    ) {}

    private static final ThreadLocal<Deque<CaptureContext>> CAPTURE_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    public static final Supplier<AttachmentType<TransformState>> TRANSFORM_DATA =
            DeferredHolder.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TRANSFORM_ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, h ->
                h.register(TRANSFORM_ID, AttachmentType.builder(TransformState::new).build()));
    }

    public static void pushCapture(CaptureContext ctx) { CAPTURE_STACK.get().push(ctx); }
    public static void popCapture() { if (!CAPTURE_STACK.get().isEmpty()) CAPTURE_STACK.get().pop(); }

    @Nullable
    private static ServerPlayer getServerPlayer(ServerLevel sl, @Nullable UUID uuid) {
        if (uuid == null) return null;
        return sl.getPlayerByUUID(uuid) instanceof ServerPlayer sp ? sp : null;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (CAPTURE_STACK.get().isEmpty()) return;

        CaptureContext ctx = CAPTURE_STACK.get().peek();
        Entity entity = event.getEntity();
        var mods = ctx.node().modifiers();
        ServerPlayer owner = getServerPlayer(sl, ctx.ownerUuid());

        if (ctx.overrideSpawnPos() != null) {
            Vec3 p = ctx.overrideSpawnPos();
            entity.setPos(p.x, p.y, p.z);
        } else if (owner != null && mods.remoteCast) {
            Vec3 hit = owner.pick(24.0D, 0.0F, false).getLocation();
            entity.setPos(hit.x, hit.y + 0.5, hit.z);
        }

        if (mods.fallDown) {
            Vec3 target = entity.position();
            entity.setPos(target.x, target.y + 10.0, target.z);
            entity.setDeltaMovement(0, -1.4, 0);
        } else if (mods.reverseCast) {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(-1.0));
        }

        if (mods.speedMult != 1.0F || mods.speedAdd != 0.0F) {
            Vec3 v = entity.getDeltaMovement();
            double len = v.length();
            if (len > 1e-4) {
                double newLen = Math.max(0.05, len * mods.speedMult + mods.speedAdd * 0.15);
                entity.setDeltaMovement(v.normalize().scale(newLen));
            }
        }

        if (entity instanceof AoeEntity aoe && (mods.radiusMult != 1.0F || mods.scaleMult != 1.0F)) {
            aoe.setRadius(aoe.getRadius() * mods.radiusMult * mods.scaleMult);
        }
        if (entity instanceof AbstractMagicProjectile magicProj) {
            if (mods.pierceAdd > 0) magicProj.setPierceLevel(mods.pierceAdd);
        }

        if (mods.hitTags.contains("LINK_PREV") && owner != null) {
            Vec3 prev = LAST_SPELL_POS.get(owner.getUUID());
            if (prev != null && prev.distanceToSqr(entity.position()) < 900) {
                damageEntitiesAlongSegment(sl, owner, prev, entity.position(), 24.0F * mods.damageMult);
            }
        }
        if (owner != null) {
            LAST_SPELL_POS.put(owner.getUUID(), entity.position());
        }

        TransformState st = entity.getData(TRANSFORM_DATA);
        st.ownerUuid = ctx.ownerUuid();
        st.followEntityUuid = ctx.followEntityUuid();
        st.sourceNode = ctx.node();
        st.orbit = mods.orbit || (ctx.followEntityUuid() != null);
        st.freeOrbit = mods.freeOrbit;
        st.orbitRadius = ctx.followEntityUuid() != null ? 1.8F : mods.orbitRadius;
        st.angleRad = mods.orbitPhaseRad;
        st.cursorHoming = mods.cursorHoming;
        st.entityHomingDeg = mods.entityHomingDeg;
        st.selfHomingDeg = mods.selfHomingDeg;
        st.splitLeft = mods.splitCount > 0 ? 1 : 0;
        st.refractLeft = mods.refractAdd;
        st.hoverSec = mods.hoverSec;
        st.endTp = mods.endTeleport;
        st.midTp = mods.midTeleport;

        for (var trig : ctx.node().triggers()) {
            if ("ENTANGLE".equals(trig.timing()) && owner != null) {
                int count = Math.max(1, trig.maxTriggers());
                var baseChild = trig.childNode();
                for (int i = 0; i < count; i++) {
                    var childModsCopy = baseChild.modifiers().copy();
                    childModsCopy.orbit = true;
                    childModsCopy.orbitPhaseRad = (float) (2.0 * Math.PI * i / count);
                    var childNodeCopy = new WandCompiler.CastNode(
                            baseChild.spellId(), baseChild.effectiveBaseLevel(),
                            childModsCopy, baseChild.triggers(), baseChild.depth()
                    );
                    if (!WandExecutionEngine.executeSingleNode(owner, childNodeCopy, entity.position(), entity.getUUID())) {
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl) || !entity.hasData(TRANSFORM_DATA)) return;

        TransformState st = entity.getData(TRANSFORM_DATA);
        if (st.ownerUuid == null || st.sourceNode == null) return;
        ServerPlayer owner = getServerPlayer(sl, st.ownerUuid);
        if (owner == null || !owner.isAlive()) return;
        if (st.hoverSec > 0 && !st.hoveringNow && entity.tickCount == 12) {
            st.hoveringNow = true;
            st.hoverTicksLeft = Math.round(st.hoverSec * 20.0F);
            entity.setNoGravity(true);
        }
        if (st.hoveringNow) {
            entity.setDeltaMovement(Vec3.ZERO);
            if (--st.hoverTicksLeft <= 0) entity.discard();
            return;
        }

        if (st.midTp && !st.midTpDone && entity.tickCount == 25) {
            st.midTpDone = true;
            double rx = (sl.random.nextDouble() - 0.5) * 10.0;
            double rz = (sl.random.nextDouble() - 0.5) * 10.0;
            entity.setPos(entity.getX() + rx, entity.getY() + 1.0, entity.getZ() + rz);
        }

        if (st.orbit) {
            Entity center = (st.followEntityUuid != null) ? sl.getEntity(st.followEntityUuid) : owner;
            if (center != null) {
                st.angleRad += st.angularSpeed;
                float r = st.freeOrbit ? (st.orbitRadius + (float) Math.sin(entity.tickCount * 0.15) * 1.5F) : st.orbitRadius;
                double tx = center.getX() + r * Math.cos(st.angleRad);
                double ty = center.getY() + (center == owner ? 0.8 : 0.0);
                double tz = center.getZ() + r * Math.sin(st.angleRad);
                if (entity instanceof Projectile) {
                    entity.setDeltaMovement(tx - entity.getX(), ty - entity.getY(), tz - entity.getZ());
                    entity.hasImpulse = true;
                } else {
                    entity.setPos(tx, ty, tz);
                }
            }
        } else if (st.cursorHoming) {
            Vec3 aim = owner.pick(32.0, 0.0F, false).getLocation();
            steerVelocityTowards(entity, aim, 25.0F);
        } else if (st.selfHomingDeg > 0) {
            steerVelocityTowards(entity, owner.getEyePosition(), st.selfHomingDeg);
        } else if (st.entityHomingDeg > 0) {
            LivingEntity nearest = findNearestHostile(sl, owner, entity.position(), 12.0, null);
            if (nearest != null) steerVelocityTowards(entity, nearest.getEyePosition(), st.entityHomingDeg);
        }

        for (var trig : st.sourceNode.triggers()) {
            if ("TICK".equals(trig.timing()) && st.tickTriggerCount < trig.maxTriggers()) {
                int intervalTicks = Math.max(2, Math.round(trig.intervalSec() * 20.0F));
                if (entity.tickCount > 0 && entity.tickCount % intervalTicks == 0) {
                    st.tickTriggerCount++;
                    WandExecutionEngine.executeSingleNode(owner, trig.childNode(), entity.position(), null);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Entity direct) || !direct.hasData(TRANSFORM_DATA)) return;
        if (!(direct.level() instanceof ServerLevel sl)) return;

        TransformState st = direct.getData(TRANSFORM_DATA);
        if (st.sourceNode == null) return;
        var mods = st.sourceNode.modifiers();
        LivingEntity victim = event.getEntity();
        ServerPlayer owner = getServerPlayer(sl, st.ownerUuid);
        if (owner == null) return;

        float dmg = event.getAmount() * mods.damageMult;
        boolean isCrit = mods.critAdd > 0 && sl.random.nextFloat() < mods.critAdd;
        if (isCrit) dmg *= 1.5F;
        event.setAmount(dmg);

        if (mods.hitTags.contains("HIT_SLOW")) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        if (mods.hitTags.contains("HIT_POISON")) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        }
        if (mods.hitTags.contains("HIT_FREEZE")) {
            victim.setTicksFrozen(Math.min(300, victim.getTicksFrozen() + 140));
        }
        if (mods.hitTags.contains("HIT_MAX_HP_BURN")) {
            float pct = victim.getMaxHealth() > 200 ? 0.005F : 0.05F;
            victim.hurt(sl.damageSources().magic(), victim.getMaxHealth() * pct);
        }
        if (mods.hitTags.contains("HIT_COLLAPSE")) {
            float thresh = victim.getMaxHealth() > 200 ? 0.05F : 0.15F;
            if (victim.getHealth() - dmg <= victim.getMaxHealth() * thresh) {
                event.setAmount(victim.getHealth() + 999.0F);
                List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class,
                        new AABB(victim.blockPosition()).inflate(5.0), e -> e != owner && e != victim && e.isAlive());
                if (!nearby.isEmpty()) {
                    float share = (victim.getMaxHealth() * 0.40F) / nearby.size();
                    for (LivingEntity n : nearby) n.hurt(sl.damageSources().magic(), share);
                }
            }
        }
        if (isCrit && mods.hitTags.contains("CRIT_PULL")) {
            List<LivingEntity> others = sl.getEntitiesOfClass(LivingEntity.class,
                    new AABB(victim.blockPosition()).inflate(8.0), e -> e != owner && e != victim && e.isAlive());
            for (int i = 0; i < Math.min(2, others.size()); i++) {
                LivingEntity other = others.get(i);
                Vec3 pull = victim.position().subtract(other.position()).normalize().scale(0.8);
                other.setDeltaMovement(pull);
                other.hurt(sl.damageSources().magic(), dmg);
            }
        }

        if (st.refractLeft > 0 && direct instanceof Projectile) {
            LivingEntity nextTarget = findNearestHostile(sl, owner, direct.position(), 10.0, victim);
            if (nextTarget != null) {
                st.refractLeft--;
                Vec3 dir = nextTarget.getEyePosition().subtract(direct.position()).normalize();
                direct.setDeltaMovement(dir.scale(Math.max(0.8, direct.getDeltaMovement().length())));
            }
        }

        long now = sl.getGameTime();
        for (var trig : st.sourceNode.triggers()) {
            if ("HIT".equals(trig.timing())) {
                long cdTicks = Math.max(2L, Math.round(trig.intervalSec() * 20.0F));
                if (now - st.lastHitTriggerGameTime >= cdTicks) {
                    st.lastHitTriggerGameTime = now;
                    WandExecutionEngine.executeSingleNode(owner, trig.childNode(), victim.getEyePosition(), null);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        Entity entity = event.getEntity();
        if (!entity.hasData(TRANSFORM_DATA)) return;

        TransformState st = entity.getData(TRANSFORM_DATA);
        if (st.expireTriggered || st.sourceNode == null || st.ownerUuid == null) return;
        if (entity.getRemovalReason() != Entity.RemovalReason.DISCARDED
                && entity.getRemovalReason() != Entity.RemovalReason.KILLED) return;

        st.expireTriggered = true;
        ServerPlayer owner = getServerPlayer(sl, st.ownerUuid);
        if (owner == null || !owner.isAlive()) return;
        Vec3 pos = entity.position();

        if (st.endTp) {
            owner.teleportTo(sl, pos.x, pos.y, pos.z, owner.getYRot(), owner.getXRot());
        }

        if (st.sourceNode.modifiers().hitTags.contains("END_THUNDER") && sl.random.nextFloat() < 0.25F) {
            var bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(pos.x, pos.y, pos.z);
                sl.addFreshEntity(bolt);
            }
        }

        for (var trig : st.sourceNode.triggers()) {
            if ("EXPIRE".equals(trig.timing())) {
                int times = Math.max(1, trig.maxTriggers());
                var baseChild = trig.childNode();
                for (int i = 0; i < times; i++) {
                    var modsCopy = baseChild.modifiers().copy();
                    if (times > 1) {
                        modsCopy.yawOffsetDeg = (360.0F / times) * i;
                    }
                    var childCopy = new WandCompiler.CastNode(
                            baseChild.spellId(), baseChild.effectiveBaseLevel(),
                            modsCopy, baseChild.triggers(), baseChild.depth()
                    );
                    if (!WandExecutionEngine.executeSingleNode(owner, childCopy, pos, null)) break;
                }
            }
        }

        if (st.splitLeft > 0) {
            var splitNode = new WandCompiler.CastNode(
                    st.sourceNode.spellId(), st.sourceNode.effectiveBaseLevel(),
                    st.sourceNode.modifiers().copy(), List.of(), st.sourceNode.depth() + 1
            );
            splitNode.modifiers().splitCount = 0;
            splitNode.modifiers().damageMult *= 0.33F;
            for (int i = 0; i < 3; i++) {
                splitNode.modifiers().yawOffsetDeg = (i - 1) * 35.0F;
                if (!WandExecutionEngine.executeSingleNode(owner, splitNode, pos, null)) break;
            }
        }
    }

    private static void steerVelocityTowards(Entity e, Vec3 target, float maxTurnDeg) {
        Vec3 vel = e.getDeltaMovement();
        double speed = vel.length();
        if (speed < 1e-4) return;
        Vec3 desired = target.subtract(e.position()).normalize();
        Vec3 current = vel.normalize();
        double maxRad = Math.toRadians(maxTurnDeg) * 0.15;
        Vec3 blended = current.lerp(desired, Math.min(1.0, maxRad)).normalize();
        e.setDeltaMovement(blended.scale(speed));
    }

    @Nullable
    private static LivingEntity findNearestHostile(ServerLevel sl, ServerPlayer owner, Vec3 center, double r, @Nullable Entity exclude) {
        return sl.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(center)).inflate(r),
                e -> e != owner && e != exclude && e.isAlive() && !e.isAlliedTo(owner))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(center))).orElse(null);
    }

    private static void damageEntitiesAlongSegment(ServerLevel sl, ServerPlayer owner, Vec3 a, Vec3 b, float dmg) {
        AABB box = new AABB(a, b).inflate(1.2);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, en -> en != owner && en.isAlive())) {
            e.hurt(sl.damageSources().lightningBolt(), dmg);
        }
    }
}