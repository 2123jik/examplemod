package com.example.examplemod.mixin.villna;

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
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathfinderMob.class)
public class PathfinderMobMixin extends Mob implements IMagicEntity {

    protected PathfinderMobMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    private static final ResourceLocation SLOWDOWN_ID =
            ResourceLocation.fromNamespaceAndPath("examplemodid", "potion_slowdown");

    @Unique private static final EntityDataAccessor<Boolean> DATA_CANCEL_CAST =
            SynchedEntityData.defineId(PathfinderMob.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Boolean> DATA_DRINKING_POTION =
            SynchedEntityData.defineId(PathfinderMob.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final AttributeModifier SPEED_MODIFIER_DRINKING =
            new AttributeModifier(SLOWDOWN_ID, -0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    @Unique private final MagicData playerMagicData = new MagicData(true);
    @Unique private boolean recreateSpell;
    @Unique private int drinkTime;
    @Unique private boolean hasUsedSingleAttack;

    @Unique public @Nullable SpellData castingSpell;

    @Unique
    private PathfinderMob examplemod$self() {
        return (PathfinderMob) (Object) this;
    }


    @Inject(method = "<init>", at = @At("RETURN"))
    private void examplemod$init(EntityType<? extends PathfinderMob> type, Level level, CallbackInfo ci) {
        if (!(examplemod$self() instanceof AbstractSpellCastingMob)) {
            playerMagicData.setSyncedData(new SyncedSpellData(examplemod$self()));
            this.lookControl = new LookControl(this) {
                @Override
                protected boolean resetXRotOnTick() {
                    return getTarget() == null;
                }
            };
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CANCEL_CAST, false);
        builder.define(DATA_DRINKING_POTION, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!this.level().isClientSide()) return;
        if (key.id() == DATA_CANCEL_CAST.id()) {
            cancelCast();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!(examplemod$self() instanceof AbstractSpellCastingMob)) {
            playerMagicData.getSyncedData().saveNBTData(tag, level().registryAccess());
            tag.putBoolean("usedSpecial", hasUsedSingleAttack);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!(examplemod$self() instanceof AbstractSpellCastingMob)) {
            var synced = new SyncedSpellData(this);
            synced.loadNBTData(tag, this.level().registryAccess());
            if (synced.isCasting()) this.recreateSpell = true;
            playerMagicData.setSyncedData(synced);
            hasUsedSingleAttack = tag.getBoolean("usedSpecial");
        }
    }


    @Override public MagicData getMagicData() { return playerMagicData; }
    @Override public boolean isCasting()    { return playerMagicData.isCasting(); }

    @Override
    public void setSyncedSpellData(SyncedSpellData syncedSpellData) {
        if (!this.level().isClientSide) return;
        boolean wasCasting = playerMagicData.isCasting();
        playerMagicData.setSyncedData(syncedSpellData);
        this.castingSpell = playerMagicData.getCastingSpell();

        if (this.castingSpell != null) {
            if (!playerMagicData.isCasting() && wasCasting) {
                castComplete();
            } else if (playerMagicData.isCasting() && !wasCasting) {
                AbstractSpell spell = playerMagicData.getCastingSpell().getSpell();
                this.initiateCastSpell(spell, playerMagicData.getCastingSpellLevel());
                if (this.castingSpell.getSpell().getCastType() == CastType.INSTANT) {
                    this.castingSpell.getSpell().onClientPreCast(
                            this.level(), this.castingSpell.getLevel(),
                            examplemod$self(), InteractionHand.MAIN_HAND, playerMagicData);
                    this.castComplete();
                }
            }
        }
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        if (spell == SpellRegistry.none()) {
            this.castingSpell = null;
            return;
        }
        this.castingSpell = new SpellData(spell, spellLevel);
        if (this.getTarget() != null) forceLookAtTarget(this.getTarget());

        if (!this.level().isClientSide
                && !this.castingSpell.getSpell().checkPreCastConditions(
                        this.level(), spellLevel, examplemod$self(), playerMagicData)) {
            this.castingSpell = null;
            return;
        }

        if (spell == SpellRegistry.TELEPORT_SPELL.get()
                || spell == SpellRegistry.FROST_STEP_SPELL.get()) {
            setTeleportLocationBehindTarget(10);
        } else if (spell == SpellRegistry.BLOOD_STEP_SPELL.get()) {
            setTeleportLocationBehindTarget(3);
        } else if (spell == SpellRegistry.BURNING_DASH_SPELL.get()) {
            setBurningDashDirectionData();
        }

        playerMagicData.initiateCast(
                this.castingSpell.getSpell(),
                this.castingSpell.getLevel(),
                this.castingSpell.getSpell().getEffectiveCastTime(this.castingSpell.getLevel(), examplemod$self()),
                CastSource.MOB,
                SpellSelectionManager.MAINHAND);

        if (!this.level().isClientSide) {
            this.castingSpell.getSpell().onServerPreCast(
                    this.level(), this.castingSpell.getLevel(), examplemod$self(), playerMagicData);
        }
    }

    @Override
    public void cancelCast() {
        if (this.isCasting()) {
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_CANCEL_CAST, !this.entityData.get(DATA_CANCEL_CAST));
            }
            this.castComplete();
        }
    }

    @Override
    public void castComplete() {
        if (!this.level().isClientSide) {
            if (this.castingSpell != null) {
                this.castingSpell.getSpell().onServerCastComplete(
                        this.level(), this.castingSpell.getLevel(),
                        examplemod$self(), playerMagicData, false);
            }
            playerMagicData.resetCastingState();
        } else {
            playerMagicData.resetCastingState();
        }
        this.castingSpell = null;
    }

    @Override public void notifyDangerousProjectile(Projectile projectile) { }
    @Override public boolean getHasUsedSingleAttack() { return hasUsedSingleAttack; }
    @Override public void setHasUsedSingleAttack(boolean v) { hasUsedSingleAttack = v; }


    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (recreateSpell) {
            recreateSpell = false;
            var synced = playerMagicData.getSyncedData();
            var spell = SpellRegistry.getSpell(synced.getCastingSpellId());
            this.initiateCastSpell(spell, synced.getCastingSpellLevel());
        }

        if (isDrinkingPotion()) {
            if (drinkTime-- <= 0) {
                finishDrinkingPotion();
            } else if (drinkTime % 4 == 0 && !this.isSilent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_DRINK, this.getSoundSource(),
                        1.0F, Utils.random.nextFloat() * 0.1F + 0.9F);
            }
        }

        if (castingSpell == null) return;

        playerMagicData.handleCastDuration();
        castingSpell.getSpell().onServerCastTick(
                level(), castingSpell.getLevel(), this, playerMagicData);

        this.forceLookAtTarget(getTarget());

        if (playerMagicData.getCastDurationRemaining() <= 0) {
            CastType type = castingSpell.getSpell().getCastType();
            if (type == CastType.LONG || type == CastType.INSTANT) {
                castingSpell.getSpell().onCast(
                        level(), castingSpell.getLevel(), this, CastSource.MOB, playerMagicData);
            }
            castComplete();
        } else if (castingSpell.getSpell().getCastType() == CastType.CONTINUOUS) {
            if ((playerMagicData.getCastDurationRemaining() + 1) % 10 == 0) {
                castingSpell.getSpell().onCast(
                        level(), castingSpell.getLevel(), this, CastSource.MOB, playerMagicData);
            }
        }
    }


    @Unique
    private void forceLookAtTarget(LivingEntity target) {
        if (target == null) return;
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double dy = target.getEyeY() - this.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) (Mth.atan2(dz, dx) * 57.2957763671875) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horiz) * 57.2957763671875));
        this.setXRot(pitch % 360.0F);
        this.setYRot(yaw   % 360.0F);
    }

    @Override
    public boolean setTeleportLocationBehindTarget(int distance) {
        LivingEntity target = this.getTarget();
        boolean valid = false;
        if (target != null) {
            Vec3 pos = target.position();
            Vec3 teleportPos = pos;
            for (int i = 0; i < 24; ++i) {
                Vec3 randomness = Utils.getRandomVec3(0.15F * i).multiply(1.0, 0.0, 1.0);
                teleportPos = Utils.moveToRelativeGroundLevel(this.level(),
                        target.position().subtract(new Vec3(0.0, 0.0, (double) distance / (i / 7 + 1))
                                .yRot(-(target.getYRot() + (float) (i * 45)) * 0.017453292F))
                                .add(randomness), 5);
                teleportPos = new Vec3(teleportPos.x, teleportPos.y + 0.1, teleportPos.z);
                AABB bb = this.getBoundingBox().move(teleportPos.subtract(this.position()));
                if (!this.level().collidesWithSuffocatingBlock(examplemod$self(), bb.inflate(-0.05))) {
                    valid = true;
                    break;
                }
            }
            playerMagicData.setAdditionalCastData(new TeleportSpell.TeleportData(
                    valid ? teleportPos : this.position()));
        } else {
            playerMagicData.setAdditionalCastData(new TeleportSpell.TeleportData(this.position()));
        }
        return valid;
    }

    @Override
    public void setBurningDashDirectionData() {
        playerMagicData.setAdditionalCastData(
                new BurningDashSpell.BurningDashDirectionOverrideCastData());
    }

    @Override public boolean isDrinkingPotion() { return this.entityData.get(DATA_DRINKING_POTION); }
    private void setDrinkingPotion(boolean v) { this.entityData.set(DATA_DRINKING_POTION, v); }

    @Override
    public void startDrinkingPotion() {
        if (!this.level().isClientSide) {
            setDrinkingPotion(true);
            this.drinkTime = 35;
            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(SPEED_MODIFIER_DRINKING);
                speed.addTransientModifier(SPEED_MODIFIER_DRINKING);
            }
        }
    }

    @Unique
    private void finishDrinkingPotion() {
        setDrinkingPotion(false);
        this.heal(Math.min(Math.max(10.0F, this.getMaxHealth() / 10.0F), this.getMaxHealth() / 4.0F));
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_MODIFIER_DRINKING);
        if (this.isSilent()) {
            this.level().playSound((Player) null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WITCH_DRINK, this.getSoundSource(),
                    1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        }
    }
}