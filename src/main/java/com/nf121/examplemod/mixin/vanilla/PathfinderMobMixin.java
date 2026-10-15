package com.nf121.examplemod.mixin.vanilla;

import com.nf121.examplemod.compat.l2hostility.SpellAffixPolicy;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import io.redspace.ironsspellbooks.spells.fire.BurningDashSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(PathfinderMob.class)
public abstract class PathfinderMobMixin extends Mob implements IMagicEntity {

    @Unique
    private static final EntityDataAccessor<Boolean> EXAMPLEMOD_CANCEL_CAST =
            SynchedEntityData.defineId(
                    PathfinderMob.class,
                    EntityDataSerializers.BOOLEAN
            );

    @Unique
    private static final EntityDataAccessor<Boolean> EXAMPLEMOD_DRINKING =
            SynchedEntityData.defineId(
                    PathfinderMob.class,
                    EntityDataSerializers.BOOLEAN
            );

    @Unique
    private static final AttributeModifier EXAMPLEMOD_DRINK_SLOWDOWN =
            new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(
                            "examplemodid",
                            "spell_affix_potion_slowdown"
                    ),
                    -0.15D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

    /*
     * 懒初始化：
     * 防止父类构造期间调用 override 时字段尚未初始化。
     */
    @Unique
    private MagicData examplemod$magicData;

    @Unique
    private SpellData examplemod$castingSpell;

    @Unique
    private UUID examplemod$castTarget;

    @Unique
    private boolean examplemod$finishing;

    @Unique
    private boolean examplemod$usedSingleAttack;

    @Unique
    private int examplemod$drinkTime;

    @Unique
    private long examplemod$nextCastTime;

    protected PathfinderMobMixin(
            EntityType<? extends Mob> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    @Unique
    private PathfinderMob examplemod$self() {
        return (PathfinderMob) (Object) this;
    }

    @Unique
    private boolean examplemod$isNativeCaster() {
        return examplemod$self() instanceof AbstractSpellCastingMob;
    }

    @Unique
    private MagicData examplemod$data() {
        if (examplemod$magicData == null) {
            examplemod$magicData = new MagicData(true);
            examplemod$magicData.setSyncedData(
                    new SyncedSpellData(examplemod$self())
            );
        }

        return examplemod$magicData;
    }

    /*
     * PathfinderMob 本身没有声明这些继承方法。
     * Mixin 将这些 override 加入 PathfinderMob。
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EXAMPLEMOD_CANCEL_CAST, false);
        builder.define(EXAMPLEMOD_DRINKING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (examplemod$isNativeCaster() || !level().isClientSide()) {
            return;
        }

        if (EXAMPLEMOD_CANCEL_CAST.equals(key)) {
            examplemod$clearClientCast();
        }
    }

    @Override
    public MagicData getMagicData() {
        return examplemod$data();
    }

    @Override
    public boolean isCasting() {
        return examplemod$magicData != null
                && examplemod$magicData.isCasting();
    }

    @Override
    public void setSyncedSpellData(SyncedSpellData synced) {
        if (!level().isClientSide()) {
            return;
        }

        MagicData data = examplemod$data();
        boolean wasCasting = data.isCasting();
        SpellData previous = examplemod$castingSpell;

        data.setSyncedData(synced);
        examplemod$castingSpell = data.getCastingSpell();

        if (!data.isCasting() || examplemod$castingSpell == null) {
            examplemod$clearClientCast();
            return;
        }

        SpellData current = examplemod$castingSpell;

        boolean newCast = !wasCasting
                || previous == null
                || previous.getSpell() != current.getSpell()
                || previous.getLevel() != current.getLevel();

        if (!newCast) {
            return;
        }

        /*
         * 客户端沿用本体的 initiateCast 初始化方式。
         * 客户端不进行服务端目标/视线判断。
         */
        initiateCastSpell(current.getSpell(), current.getLevel());

        SpellData active = examplemod$castingSpell;
        if (active != null
                && active.getSpell().getCastType() == CastType.INSTANT) {
            active.getSpell().onClientPreCast(
                    level(),
                    active.getLevel(),
                    examplemod$self(),
                    InteractionHand.MAIN_HAND,
                    data
            );

            examplemod$clearClientCast();
        }
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        if (spell == null || spell == SpellRegistry.none()) {
            return;
        }

        MagicData data = examplemod$data();
        int actualLevel = Mth.clamp(spellLevel, 1, spell.getMaxLevel());

        if (level().isClientSide()) {
            examplemod$castingSpell = new SpellData(spell, actualLevel);

            data.initiateCast(
                    spell,
                    actualLevel,
                    spell.getEffectiveCastTime(actualLevel, examplemod$self()),
                    CastSource.MOB,
                    SpellSelectionManager.MAINHAND
            );
            return;
        }

        if (examplemod$finishing
                || isCasting()
                || examplemod$castingSpell != null
                || isDrinkingPotion()
                || level().getGameTime() < examplemod$nextCastTime) {
            return;
        }

        LivingEntity target = getTarget();

        if (!SpellAffixPolicy.canStart(this, target, spell, actualLevel)) {
            return;
        }

        examplemod$faceTarget(target);

        if (!spell.checkPreCastConditions(
                level(),
                actualLevel,
                examplemod$self(),
                data
        )) {
            examplemod$nextCastTime = level().getGameTime()
                    + SpellAffixPolicy.FAILED_ATTEMPT_DELAY_TICKS;
            return;
        }

        /*
         * checkPreCastConditions 可能由附属模组实现。
         * 调用后重新检查目标和视线。
         */
        if (getTarget() != target
                || !SpellAffixPolicy.canStart(
                this, target, spell, actualLevel
        )) {
            return;
        }

        if (spell == SpellRegistry.TELEPORT_SPELL.get()
                || spell == SpellRegistry.FROST_STEP_SPELL.get()) {
            if (!setTeleportLocationBehindTarget(10)) {
                examplemod$nextCastTime = level().getGameTime()
                        + SpellAffixPolicy.FAILED_ATTEMPT_DELAY_TICKS;
                return;
            }
        } else if (spell == SpellRegistry.BLOOD_STEP_SPELL.get()) {
            if (!setTeleportLocationBehindTarget(3)) {
                examplemod$nextCastTime = level().getGameTime()
                        + SpellAffixPolicy.FAILED_ATTEMPT_DELAY_TICKS;
                return;
            }
        } else if (spell == SpellRegistry.BURNING_DASH_SPELL.get()) {
            setBurningDashDirectionData();
        }

        SpellData started = new SpellData(spell, actualLevel);
        examplemod$castingSpell = started;
        examplemod$castTarget = target.getUUID();

        data.initiateCast(
                spell,
                actualLevel,
                spell.getEffectiveCastTime(actualLevel, examplemod$self()),
                CastSource.MOB,
                SpellSelectionManager.MAINHAND
        );

        spell.onServerPreCast(
                level(),
                actualLevel,
                examplemod$self(),
                data
        );
    }

    @Override
    public void cancelCast() {
        if (level().isClientSide()) {
            examplemod$clearClientCast();
            return;
        }

        if (examplemod$finishing
                || (examplemod$castingSpell == null && !isCasting())) {
            return;
        }

        entityData.set(
                EXAMPLEMOD_CANCEL_CAST,
                !entityData.get(EXAMPLEMOD_CANCEL_CAST)
        );

        examplemod$finishCast();
    }

    @Override
    public void castComplete() {
        if (level().isClientSide()) {
            examplemod$clearClientCast();
        } else {
            examplemod$finishCast();
        }
    }

    @Unique
    private void examplemod$finishCast() {
        if (examplemod$finishing) {
            return;
        }

        SpellData finished = examplemod$castingSpell;

        if (finished == null && !isCasting()) {
            return;
        }

        examplemod$finishing = true;

        try {
            if (finished != null) {
                /*
                 * 与提供的 AbstractSpellCastingMob 一致，末参数使用 false。
                 * 不在未核实签名语义时把它假定成“是否被取消”。
                 */
                finished.getSpell().onServerCastComplete(
                        level(),
                        finished.getLevel(),
                        examplemod$self(),
                        examplemod$data(),
                        false
                );
            }
        } finally {
            examplemod$data().resetCastingState();
            examplemod$castingSpell = null;
            examplemod$castTarget = null;
            examplemod$nextCastTime = level().getGameTime()
                    + SpellAffixPolicy.GLOBAL_RECOVERY_TICKS;
            examplemod$finishing = false;
        }
    }

    @Unique
    private void examplemod$clearClientCast() {
        if (examplemod$magicData != null) {
            examplemod$magicData.resetCastingState();
        }

        examplemod$castingSpell = null;
        examplemod$castTarget = null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        /*
         * 原生 AbstractSpellCastingMob 调用 super 时也会经过这里。
         * 必须提前退出，避免双重驱动原生法术与喝药。
         */
        if (examplemod$isNativeCaster()) {
            return;
        }

        examplemod$tickPotion();

        SpellData active = examplemod$castingSpell;
        if (active == null) {
            return;
        }

        if (!isCasting() || !examplemod$canContinue(active)) {
            cancelCast();
            return;
        }

        LivingEntity target = getTarget();

        if (SpellAffixPolicy.isValidEnemy(this, target)
                && SpellAffixPolicy.hasClearSight(this, target)) {
            examplemod$faceTarget(target);
        }

        MagicData data = examplemod$data();
        data.handleCastDuration();

        /*
         * 检查在 onServerCastTick 之前。
         * 部分法术的 tick 回调本身就会产生攻击效果。
         */
        active.getSpell().onServerCastTick(
                level(),
                active.getLevel(),
                examplemod$self(),
                data
        );

        /*
         * 法术回调可能取消施法或改变状态，不能继续使用旧字段。
         */
        if (examplemod$castingSpell != active) {
            return;
        }

        if (!isCasting() || !examplemod$canContinue(active)) {
            cancelCast();
            return;
        }

        int remaining = data.getCastDurationRemaining();
        CastType type = active.getSpell().getCastType();

        if (remaining <= 0) {
            if (type == CastType.LONG || type == CastType.INSTANT) {
                active.getSpell().onCast(
                        level(),
                        active.getLevel(),
                        examplemod$self(),
                        CastSource.MOB,
                        data
                );
            }

            if (examplemod$castingSpell == active) {
                castComplete();
            }
        } else if (type == CastType.CONTINUOUS
                && (remaining + 1) % 10 == 0) {

            active.getSpell().onCast(
                    level(),
                    active.getLevel(),
                    examplemod$self(),
                    CastSource.MOB,
                    data
            );
        }
    }

    @Unique
    private boolean examplemod$canContinue(SpellData active) {
        LivingEntity target = getTarget();
        AbstractSpell spell = active.getSpell();

        /*
         * 攻击法术锁定起手时的目标。
         * 目标改变时取消，避免把预先计算的数据用于另一个目标。
         */
        if (SpellAffixPolicy.requiresTargetThroughout(spell)) {
            if (target == null
                    || examplemod$castTarget == null
                    || !examplemod$castTarget.equals(target.getUUID())) {
                return false;
            }
        }

        return SpellAffixPolicy.canContinue(
                this,
                target,
                spell,
                active.getLevel()
        );
    }

    @Unique
    private void examplemod$faceTarget(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getEyeY() - getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);

        setYRot(yaw);
        setYHeadRot(yaw);
        setXRot(pitch);

        getLookControl().setLookAt(target, 90.0F, 90.0F);
    }

    @Override
    public boolean setTeleportLocationBehindTarget(int distance) {
        MagicData data = examplemod$data();

        data.setAdditionalCastData(
                new TeleportSpell.TeleportData(position())
        );

        if (level().isClientSide()) {
            return false;
        }

        LivingEntity target = getTarget();

        if (!SpellAffixPolicy.isValidEnemy(this, target)
                || !SpellAffixPolicy.hasClearSight(this, target)) {
            return false;
        }

        for (int i = 0; i < 24; i++) {
            Vec3 randomness = Utils.getRandomVec3(0.15F * i)
                    .multiply(1.0, 0.0, 1.0);

            Vec3 destination = target.position()
                    .subtract(new Vec3(
                            0.0,
                            0.0,
                            distance / (double) (i / 7 + 1)
                    ).yRot(
                            -(target.getYRot() + i * 45.0F) * Mth.DEG_TO_RAD
                    ))
                    .add(randomness);

            BlockPos candidate = BlockPos.containing(destination);

            if (!level().hasChunkAt(candidate)) {
                continue;
            }

            destination = Utils.moveToRelativeGroundLevel(
                    level(), destination, 5
            ).add(0.0, 0.1, 0.0);

            BlockPos pos = BlockPos.containing(destination);

            AABB box = getBoundingBox().move(
                    destination.subtract(position())
            );

            if (!level().getWorldBorder().isWithinBounds(pos)
                    || !level().noCollision(this, box)
                    || level().containsAnyLiquid(box)) {
                continue;
            }

            /*
             * 不选择悬空落点。
             * 这是通用地面怪物策略，不为飞行怪物定制传送。
             */
            if (level().noCollision(this, box.move(0.0, -0.2, 0.0))) {
                continue;
            }

            /*
             * 限制传送目的地也处于施法者可见空间内。
             * 避免目标可见，但计算出的“背后位置”位于另一堵墙后。
             */
            Vec3 destinationEye = destination.add(0.0, getEyeHeight(), 0.0);

            if (level().clip(new net.minecraft.world.level.ClipContext(
                    getEyePosition(),
                    destinationEye,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    this
            )).getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                continue;
            }

            data.setAdditionalCastData(
                    new TeleportSpell.TeleportData(destination)
            );
            return true;
        }

        return false;
    }

    @Override
    public void setBurningDashDirectionData() {
        examplemod$data().setAdditionalCastData(
                new BurningDashSpell.BurningDashDirectionOverrideCastData()
        );
    }

    @Override
    public void notifyDangerousProjectile(Projectile projectile) {
        /*
         * 不给所有普通怪物自动增加闪避/反制行为。
         */
    }

    @Override
    public boolean getHasUsedSingleAttack() {
        return examplemod$usedSingleAttack;
    }

    @Override
    public void setHasUsedSingleAttack(boolean value) {
        examplemod$usedSingleAttack = value;
    }

    @Override
    public boolean isDrinkingPotion() {
        return entityData.get(EXAMPLEMOD_DRINKING);
    }

    @Override
    public void startDrinkingPotion() {
        if (level().isClientSide()
                || !isAlive()
                || isNoAi()
                || isCasting()
                || isDrinkingPotion()) {
            return;
        }

        entityData.set(EXAMPLEMOD_DRINKING, true);
        examplemod$drinkTime = 35;

        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(EXAMPLEMOD_DRINK_SLOWDOWN);
            speed.addTransientModifier(EXAMPLEMOD_DRINK_SLOWDOWN);
        }
    }

    @Unique
    private void examplemod$tickPotion() {
        if (!isDrinkingPotion()) {
            return;
        }

        if (!isAlive() || isNoAi()) {
            examplemod$stopPotion();
            return;
        }

        if (--examplemod$drinkTime <= 0) {
            examplemod$stopPotion();

            heal(Math.min(
                    Math.max(10.0F, getMaxHealth() / 10.0F),
                    getMaxHealth() / 4.0F
            ));

            if (!isSilent()) {
                level().playSound(
                        null,
                        getX(), getY(), getZ(),
                        SoundEvents.WITCH_DRINK,
                        getSoundSource(),
                        1.0F,
                        0.8F + random.nextFloat() * 0.4F
                );
            }

            examplemod$nextCastTime = level().getGameTime()
                    + SpellAffixPolicy.GLOBAL_RECOVERY_TICKS;
        } else if (examplemod$drinkTime % 4 == 0 && !isSilent()) {
            level().playSound(
                    null,
                    getX(), getY(), getZ(),
                    SoundEvents.GENERIC_DRINK,
                    getSoundSource(),
                    1.0F,
                    0.9F + random.nextFloat() * 0.1F
            );
        }
    }

    @Unique
    private void examplemod$stopPotion() {
        entityData.set(EXAMPLEMOD_DRINKING, false);
        examplemod$drinkTime = 0;

        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(EXAMPLEMOD_DRINK_SLOWDOWN);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (examplemod$isNativeCaster()) {
            return;
        }

        /*
         * 不保存正在执行的吟唱。
         * 只保存可以安全恢复的长期标志。
         */
        CompoundTag magicTag = new CompoundTag();
        magicTag.putBoolean(
                "UsedSingleAttack",
                examplemod$usedSingleAttack
        );
        tag.put("examplemod_spell_affix", magicTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (examplemod$isNativeCaster()) {
            return;
        }

        if (examplemod$magicData != null) {
            examplemod$magicData.resetCastingState();
        }

        examplemod$castingSpell = null;
        examplemod$castTarget = null;
        examplemod$finishing = false;

        CompoundTag magicTag = tag.getCompound("examplemod_spell_affix");

        examplemod$usedSingleAttack = magicTag.contains("UsedSingleAttack")
                ? magicTag.getBoolean("UsedSingleAttack")
                : tag.getBoolean("usedSpecial");

        examplemod$stopPotion();

        examplemod$nextCastTime = level().getGameTime()
                + SpellAffixPolicy.INITIAL_DELAY_TICKS;
    }
}
