package com.example.examplemod.spell;

import com.example.examplemod.registry.ModEntities;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SummonedCaveSpider extends CaveSpider implements IMagicSummon {

    public SummonedCaveSpider(EntityType<? extends SummonedCaveSpider> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    public SummonedCaveSpider(Level level, LivingEntity owner) {
        this(ModEntities.SUMMONED_CAVE_SPIDER.get(), level);
        SummonManager.setOwner(this, owner);
    }

    @Override
    protected void registerGoals() {
        // 动作 AI
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F)); // 蜘蛛经典扑咬
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, true));
        // 跟随主人：0.9速度，最远20格开始追，5格内停下，瞬移距离25格
        this.goalSelector.addGoal(6, new GenericFollowOwnerGoal(this, this::getSummoner, 1.0, 15.0F, 3.0F, false, 25.0F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));

        // 目标仇恨 AI（完全受主人控制）
        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new GenericHurtByTargetGoal(this, (entity) -> entity == this.getSummoner())).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // 采用法术伤害源（受法强加成，触发主人击杀与法术伤害监听）
        AbstractSpell spell = ModSpells.SUMMON_CAVE_SPIDER.get();
        boolean flag = Utils.doMeleeAttack(this, target, spell.getDamageSource(this, this.getSummoner()));
        if (flag && target instanceof LivingEntity livingTarget) {
            // 继承洞穴蜘蛛特有的毒素攻击
            livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 0), this);
        }
        return flag;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || this.isAlliedHelper(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.shouldIgnoreDamage(source) ? false : super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource damageSource) {
        this.onDeathHelper();
        super.die(damageSource);
    }

    @Override
    public void onRemovedFromLevel() {
        this.onRemovedHelper(this);
        super.onRemovedFromLevel();
    }

    @Override
    public void onUnSummon() {
        if (!this.level().isClientSide) {
            MagicManager.spawnParticles(this.level(), ParticleTypes.POOF, this.getX(), this.getY() + 0.2, this.getZ(), 15, 0.3, 0.3, 0.3, 0.02, false);
            this.setRemoved(RemovalReason.DISCARDED);
        }
    }

    // 默认基础属性（会被法术等级覆盖）
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }
}