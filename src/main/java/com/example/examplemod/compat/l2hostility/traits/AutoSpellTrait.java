package com.example.examplemod.compat.l2hostility.traits;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.capabilities.magic.TelekinesisData;
import io.redspace.ironsspellbooks.spells.eldritch.TelekinesisSpell;
import io.redspace.ironsspellbooks.spells.ender.BlackHoleSpell;
import io.redspace.ironsspellbooks.spells.ender.StarfallSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class AutoSpellTrait extends MobTrait {

    private static final Map<Mob, CastSession> SESSIONS = new WeakHashMap<>();

    private final Supplier<? extends AbstractSpell> spellSupplier;
    private final int cooldownTicks;
    private final Map<Mob, Integer> lastCastPerMob = new WeakHashMap<>();

    public AutoSpellTrait(Supplier<? extends AbstractSpell> spellSupplier) {
        super(() -> SpellTraitHelper.getDynamicColor(spellSupplier.get()));
        this.spellSupplier = spellSupplier;
        AbstractSpell spell = spellSupplier.get();
        this.cooldownTicks = spell != null ? SpellTraitHelper.getSafeCooldown(spell) : 60;
    }

    @Override
    public void tick(LivingEntity entity, int traitLevel) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Mob mob)) return;

        AbstractSpell spell = spellSupplier.get();
        if (spell == null || !spell.isEnabled()) return;

        CastSession session = SESSIONS.get(mob);
        if (session != null) {
            if (session.owner == this) tickSession(mob, session);
            return;
        }

        Integer last = lastCastPerMob.get(mob);
        if (last != null) {
            int ownNext = last + Math.max(40, this.cooldownTicks);
            if (mob.tickCount < ownNext) return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        int maxTraitLv = this.getMaxLevel(mob.level().registryAccess());
        int spellLevel = SpellTraitHelper.scaleSpellLevel(traitLevel, maxTraitLv, spell);
        CastType castType = spell.getCastType();
        int windupTicks = (castType == CastType.INSTANT) ? 4 : 20;
        int channelTicks = (castType == CastType.CONTINUOUS)
                ? Math.max(40, spell.getCastTime(spellLevel)) : 0;
        int total = windupTicks + channelTicks + 10;

        CastSession newSession = new CastSession(
                this, spell, mob.tickCount, total,
                castType == CastType.CONTINUOUS,
                target, spellLevel, windupTicks, channelTicks);

        SESSIONS.put(mob, newSession);
        lastCastPerMob.put(mob, mob.tickCount);
        mob.getLookControl().setLookAt(target, 45.0F, 45.0F);
    }

    private void tickSession(Mob mob, CastSession s) {
        int elapsed = mob.tickCount - s.startTick;
        MagicData magicData = MagicData.getPlayerMagicData(mob);
        if (magicData == null) {
            SESSIONS.remove(mob);
            return;
        }
        if (elapsed < s.windupTicks) {
            mob.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 5, 2, false, false, false));
            if (elapsed % 4 == 0) {
                spawnCastingParticles((ServerLevel) mob.level(), mob, s.spell);
            }
            if (elapsed == s.windupTicks - 1 && s.target.isAlive()) {
                mob.getLookControl().setLookAt(s.target, 45.0F, 45.0F);
                if (s.spell instanceof BlackHoleSpell) {
                    aimAtGround(mob, s.target);
                }
            }
            return;
        }

        if (elapsed == s.windupTicks) {
            if (!s.target.isAlive()) {
                finishSession(mob, s, magicData, true);
                return;
            }

            injectCastData(mob, s.target, s.spell, magicData);

            // 【关键修复】如果是持续施法，通知 magicData 开始引导施法
            if (s.continuous) {
                magicData.initiateCast(s.spell, s.spellLevel, s.channelTicks, CastSource.MOB, "trait");
            }

            s.spell.onCast(mob.level(), s.spellLevel, mob, CastSource.MOB, magicData);

            if (!s.continuous) {
                try {
                    s.spell.onServerCastComplete(mob.level(), s.spellLevel, mob, magicData, false);
                } catch (Throwable ignored) {}
                SESSIONS.remove(mob);
            }
            return;
        }

        if (s.continuous && elapsed < s.windupTicks + s.channelTicks) {
            if (!s.target.isAlive()) {
                finishSession(mob, s, magicData, true);
                return;
            }

            // 关键：每 tick 递减施法剩余时间，并调用法术 tick
            magicData.handleCastDuration();
            s.spell.onServerCastTick(mob.level(), s.spellLevel, mob, magicData);
            return;
        }

        if (s.continuous && elapsed >= s.windupTicks + s.channelTicks) {
            try {
                s.spell.onServerCastComplete(mob.level(), s.spellLevel, mob, magicData, false);
            } catch (Throwable ignored) {}

            // 关键：结束施法状态
            magicData.resetCastingState();
            SESSIONS.remove(mob);
            return;
        }

        if (elapsed >= s.duration) {
            finishSession(mob, s, magicData, true);
        }
    }

    private void finishSession(Mob mob, CastSession s, MagicData magicData, boolean cancelled) {
        try {
            s.spell.onServerCastComplete(mob.level(), s.spellLevel, mob, magicData, cancelled);
        } catch (Throwable ignored) {}

        magicData.resetCastingState(); //
        SESSIONS.remove(mob);
    }

    private void injectCastData(Mob mob, LivingEntity target, AbstractSpell spell, MagicData magicData) {
        if (magicData.getAdditionalCastData() != null) {
            magicData.resetAdditionalCastData();
        }

        if (spell instanceof StarfallSpell) {
            Vec3 ground = Utils.moveToRelativeGroundLevel(mob.level(), target.position(), 12);
            magicData.setAdditionalCastData(new StarfallSpell.StarfallCastData(ground));
            return;
        }
        if (spell instanceof TelekinesisSpell) {
            magicData.setAdditionalCastData(
                    new TelekinesisData(mob.distanceTo(target), target, 6));
            return;
        }
        if (spell instanceof BlackHoleSpell) {
            aimAtGround(mob, target);
            return;
        }
        magicData.setAdditionalCastData(new TargetEntityCastData(target));
    }

    private void aimAtGround(Mob mob, LivingEntity target) {
        double ex = mob.getX(), ey = mob.getEyeY(), ez = mob.getZ();
        double tx = target.getX(), ty = target.getY() + 0.1, tz = target.getZ();
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horiz)));

        mob.setYRot(yaw);
        mob.setXRot(pitch);
        mob.yHeadRot = yaw;
        mob.yBodyRot = yaw;
    }

    private void spawnCastingParticles(ServerLevel level, Mob mob, AbstractSpell spell) {
        Vector3f color = spell.getTargetingColor();
        DustParticleOptions dust = new DustParticleOptions(color, 1.2F);

        double x = mob.getX();
        double y = mob.getY() + (mob.getBbHeight() * 0.5);
        double z = mob.getZ();

        level.sendParticles(dust, x, y, z, 3, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.5, z, 2, 0.2, 0.2, 0.2, 0.5);
    }

    // ============================================================
    // 词条名 = 法术名。SpellTraitSymbol 靠这个拿名字，lang 用 Iron's 自带
    // ============================================================
    @Override
    public String getDescriptionId() {
        AbstractSpell spell = spellSupplier.get();
        return spell != null ? spell.getComponentId() : super.getDescriptionId();
    }

    // ============================================================
    // 详情：显示 cost / weight / min_level / max_rank，全部从 DataMap 读
    // ============================================================
    @Override
    public void addDetail(RegistryAccess access, List<Component> list) {
        AbstractSpell spell = spellSupplier.get();
        if (spell == null) { super.addDetail(access, list); return; }

        try {
            for (MutableComponent line : spell.getUniqueInfo(1, null)) {
                list.add(Component.literal(" • ").append(line).withStyle(ChatFormatting.GRAY));
            }
        } catch (Throwable ignored) {}
    }

    private static final class CastSession {
        final AutoSpellTrait owner;
        final AbstractSpell spell;
        final int startTick;
        final int duration;
        final boolean continuous;
        final LivingEntity target;
        final int spellLevel;
        final int windupTicks;
        final int channelTicks;

        CastSession(AutoSpellTrait owner, AbstractSpell spell, int startTick, int duration,
                    boolean continuous, LivingEntity target, int spellLevel,
                    int windupTicks, int channelTicks) {
            this.owner = owner;
            this.spell = spell;
            this.startTick = startTick;
            this.duration = duration;
            this.continuous = continuous;
            this.target = target;
            this.spellLevel = spellLevel;
            this.windupTicks = windupTicks;
            this.channelTicks = channelTicks;
        }
    }
}