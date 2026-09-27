package com.example.examplemod.content.spell;

import com.example.examplemod.registry.ModEntities;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class SummonedSupportDragon extends EnderDragon implements IMagicSummon {

    private static final double HOVER_HEIGHT = 10.0;
    private static final int FIREBALL_INTERVAL = 60;
    private static final double TARGET_RANGE = 32.0;

    private int fireballCooldown = 0;
    private float spellDamage = 8.0F;
    private int spellLevel = 1;

    public SummonedSupportDragon(EntityType<? extends EnderDragon> type, Level level) {
        super(type, level);   // 参数会被原版忽略，用 getType() 修正
        this.xpReward = 0;
        this.setNoGravity(true);
    }

    // ★ 坑 1 修复：原版 EnderDragon 硬编码了 EntityType.ENDER_DRAGON
    @Override
    public EntityType<?> getType() {
        return ModEntities.SUMMONED_SUPPORT_DRAGON.get();
    }

    // ★ 已修复：不再覆写 defineSynchedData

    public void setSpellPower(float damage, int level) {
        this.spellDamage = damage;
        this.spellLevel = level;
    }

    @Override
    protected void registerGoals() { /* 清空 */ }

    @Override
    public void aiStep() {
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.baseTick();

        // ★ 坑 2 修复：hurt 会把相位切到 TAKEOFF，拉回来
        if (!this.level().isClientSide
                && this.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.HOVERING) {
            this.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
        }

        LivingEntity owner = getSummoner() instanceof LivingEntity le ? le : null;
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) onUnSummon();
            return;
        }

        // 悬浮跟随
        Vec3 target = new Vec3(owner.getX(), owner.getY() + HOVER_HEIGHT, owner.getZ());
        Vec3 delta = target.subtract(position());
        Vec3 motion = delta.scale(0.08);
        if (motion.lengthSqr() > 0.25) motion = motion.normalize().scale(0.5);
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, getDeltaMovement());

        // 敌人搜索 + 攻击
        LivingEntity enemy = findNearestEnemy(owner);
        if (enemy != null) {
            faceEntity(enemy);
            tryShootFireball(enemy);
        } else {
            faceEntity(owner);
        }

        // 翅膀动画 + 粒子
        this.flapTime += 0.1F;
        if (this.flapTime > 1.0F) this.flapTime = 0.0F;

        if (this.level().isClientSide && this.tickCount % 4 == 0) {
            this.level().addParticle(ParticleTypes.DRAGON_BREATH,
                    this.getX() + (this.random.nextDouble() - 0.5) * 4,
                    this.getY() + this.random.nextDouble() * 2,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 4,
                    0, 0, 0);
        }
    }

    @Nullable
    private LivingEntity findNearestEnemy(LivingEntity owner) {
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(TARGET_RANGE),
                e -> e != owner
                        && e.isAlive()
                        && !this.isAlliedHelper(e)
                        && !(e instanceof Player p && p.isCreative())
        );
        return candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this)))
                .orElse(null);
    }

    private void faceEntity(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        this.setYRot(yaw);
        this.yHeadRot = yaw;
        this.yBodyRot = yaw;
    }

    private void tryShootFireball(LivingEntity target) {
        if (this.level().isClientSide) return;
        if (fireballCooldown > 0) { fireballCooldown--; return; }
        fireballCooldown = FIREBALL_INTERVAL;

        Vec3 look = this.getViewVector(1.0F);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5) - this.getY(0.5);
        double dz = target.getZ() - this.getZ();
        int explosionPower = Math.max(1, spellLevel);

        LargeFireball fireball = new LargeFireball(
                this.level(), this, new Vec3(dx, dy, dz), explosionPower);
        fireball.setPos(
                this.getX() + look.x * 4.0,
                this.getY(0.5),
                this.getZ() + look.z * 4.0
        );
        this.level().addFreshEntity(fireball);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_SHOOT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    @Override
    public void onUnSummon() {
        if (!this.level().isClientSide) {
            MagicManager.spawnParticles(this.level(), ParticleTypes.POOF,
                    this.getX(), this.getY() + 2.0, this.getZ(),
                    40, 2.0, 1.0, 2.0, 0.05, false);
            this.setRemoved(RemovalReason.DISCARDED);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide) {
            this.onDeathHelper();
            MagicManager.spawnParticles(this.level(), ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 4.0, this.getZ(),
                    10, 4.0, 2.0, 4.0, 0.0, false);
        }
        this.setHealth(0.0F);
        this.remove(RemovalReason.KILLED);
        this.gameEvent(GameEvent.ENTITY_DIE);   // 补上，通知其他模组
    }

    @Override
    public void onRemovedFromLevel() {
        this.onRemovedHelper(this);
        super.onRemovedFromLevel();
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || this.isAlliedHelper(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.shouldIgnoreDamage(source) ? false : super.hurt(source, amount);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EnderDragon.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }
}