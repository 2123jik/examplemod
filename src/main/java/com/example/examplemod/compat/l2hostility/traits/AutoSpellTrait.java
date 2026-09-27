package com.example.examplemod.compat.l2hostility.traits;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
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

    // ============================================================
    // 全局共享状态：一个 mob 同一时刻只能有一个施法会话
    // ============================================================
    /** mob -> 当前正在进行的施法会话。WeakHashMap 保证 mob 被回收后自动清理。 */
    private static final Map<Mob, CastSession> SESSIONS = new WeakHashMap<>();

    // ============================================================
    // 本 trait 实例字段（trait 是单例，但 map key 是 mob → 达到 per-mob 效果）
    // ============================================================
    private final Supplier<? extends AbstractSpell> spellSupplier;
    private final int cooldownTicks;
    /** mob -> 本 trait 上次开始施法的 mob.tickCount（用于本 trait 自己的冷却） */
    private final Map<Mob, Integer> lastCastPerMob = new WeakHashMap<>();

    public AutoSpellTrait(Supplier<? extends AbstractSpell> spellSupplier) {
        super(() -> SpellTraitHelper.getDynamicColor(spellSupplier.get()));
        this.spellSupplier = spellSupplier;
        AbstractSpell spell = spellSupplier.get();
        this.cooldownTicks = spell != null ? SpellTraitHelper.getSafeCooldown(spell) : 60;
    }

    // ============================================================
    // 主入口
    // ============================================================
    @Override
    public void tick(LivingEntity entity, int traitLevel) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Mob mob)) return;

        AbstractSpell spell = spellSupplier.get();
        if (spell == null || !spell.isEnabled()) return;

        // ---- 1. 该 mob 有正在进行的施法会话 ----
        CastSession session = SESSIONS.get(mob);
        if (session != null) {
            // 只有会话的 owner 才推进状态机，其他 trait 直接跳过
            if (session.owner == this) {
                tickSession(mob, session);
            }
            return;
        }

        // ---- 2. 检查本 trait 在该 mob 上的冷却 ----
        Integer last = lastCastPerMob.get(mob);
        if (last != null) {
            int ownNext = last + Math.max(40, this.cooldownTicks);
            if (mob.tickCount < ownNext) return;
        }

        // ---- 3. 目标检查 ----
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        // ---- 4. 创建施法会话，抢占该 mob ----
        int maxTraitLv = this.getMaxLevel(mob.level().registryAccess());
        int spellLevel = SpellTraitHelper.scaleSpellLevel(traitLevel, maxTraitLv, spell);
        CastType castType = spell.getCastType();
        int windupTicks = (castType == CastType.INSTANT) ? 4 : 20;
        int channelTicks = (castType == CastType.CONTINUOUS)
                ? Math.max(40, spell.getCastTime(spellLevel)) : 0;
        int total = windupTicks + channelTicks + 10; // +10 收尾冗余

        CastSession newSession = new CastSession(
                this, spell, mob.tickCount, total,
                castType == CastType.CONTINUOUS,
                target, spellLevel, windupTicks, channelTicks);

        SESSIONS.put(mob, newSession);
        lastCastPerMob.put(mob, mob.tickCount);

        // 立即设置朝向（持续到前摇结束）
        mob.getLookControl().setLookAt(target, 45.0F, 45.0F);
    }

    // ============================================================
    // 会话状态机
    // ============================================================
    private void tickSession(Mob mob, CastSession s) {
        int elapsed = mob.tickCount - s.startTick;
        MagicData magicData = MagicData.getPlayerMagicData(mob);

        // ---------- 前摇阶段 ----------
        if (elapsed < s.windupTicks) {
            mob.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 5, 2, false, false, false));
            if (elapsed % 4 == 0) {
                spawnCastingParticles((ServerLevel) mob.level(), mob, s.spell);
            }
            // 前摇最后一 tick：把朝向"钉死"，确保 getLookAngle() 生效
            if (elapsed == s.windupTicks - 1 && s.target.isAlive()) {
                mob.getLookControl().setLookAt(s.target, 45.0F, 45.0F);
                if (s.spell instanceof BlackHoleSpell) {
                    aimAtGround(mob, s.target); // 模式 C：直接改写 xRot/yRot
                }
            }
            return;
        }

        // ---------- 触发点 ----------
        if (elapsed == s.windupTicks) {
            if (!s.target.isAlive()) {
                finishSession(mob, s, magicData, true);
                return;
            }
            injectCastData(mob, s.target, s.spell, magicData);
            s.spell.onCast(mob.level(), s.spellLevel, mob, CastSource.MOB, magicData);

            // 非持续法术：同一 tick 立即收尾
            if (!s.continuous) {
                try {
                    s.spell.onServerCastComplete(mob.level(), s.spellLevel, mob, magicData, false);
                } catch (Throwable ignored) {}
                SESSIONS.remove(mob);
            }
            return;
        }

        // ---------- 引导阶段（仅 CONTINUOUS）----------
        if (s.continuous && elapsed < s.windupTicks + s.channelTicks) {
            if (!s.target.isAlive()) {
                finishSession(mob, s, magicData, true);
                return;
            }
            s.spell.onServerCastTick(mob.level(), s.spellLevel, mob, magicData);
            return;
        }

        // ---------- 引导结束 ----------
        if (s.continuous && elapsed == s.windupTicks + s.channelTicks) {
            try {
                s.spell.onServerCastComplete(mob.level(), s.spellLevel, mob, magicData, false);
            } catch (Throwable ignored) {}
            SESSIONS.remove(mob);
            return;
        }

        // ---------- 兜底：超时强制结束 ----------
        if (elapsed >= s.duration) {
            finishSession(mob, s, magicData, true);
        }
    }

    private void finishSession(Mob mob, CastSession s, MagicData magicData, boolean cancelled) {
        try {
            s.spell.onServerCastComplete(mob.level(), s.spellLevel, mob, magicData, cancelled);
        } catch (Throwable ignored) {
            // 防御性：某些法术在 cancelled=true 时内部状态可能不一致
        }
        SESSIONS.remove(mob);
    }

    // ============================================================
    // CastData 注入：模式 A / B / C 分派
    // ============================================================
    private void injectCastData(Mob mob, LivingEntity target, AbstractSpell spell, MagicData magicData) {
        // 清掉上一轮残留（防御性）
        if (magicData.getAdditionalCastData() != null) {
            magicData.resetAdditionalCastData();
        }

        // 模式 B：专属 CastData
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

        // 模式 C：纯射线，没有 cast data，只能矫正朝向（已在 session 前摇末尾做过，这里再兜一次）
        if (spell instanceof BlackHoleSpell) {
            aimAtGround(mob, target);
            return;
        }

        // 模式 A：TargetEntityCastData（Iron's 本体 + iss_magicfromtheeast 绝大多数法术）
        magicData.setAdditionalCastData(new TargetEntityCastData(target));
    }

    // ============================================================
    // 模式 C 的俯视瞄准
    // ============================================================
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

    // ============================================================
    // 粒子预警
    // ============================================================
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
    // L2H 接口实现
    // ============================================================
    @Override
    public boolean allow(LivingEntity le, int difficulty, int maxModLv) {
        if (this.isBanned()) return false;
        return difficulty >= 1;
    }

    @Override
    public int getMaxLevel(RegistryAccess access) {
        AbstractSpell spell = spellSupplier.get();
        return spell != null ? spell.getMaxLevel() : 5;
    }

    @Override
    public int getCost(RegistryAccess access, double factor) {
        AbstractSpell spell = spellSupplier.get();
        if (spell == null) return 1;

        int rawMana = spell.getManaCost(1);
        double effectiveMana = (spell.getCastType() == CastType.CONTINUOUS)
                ? rawMana * 20.0
                : Math.max(10, rawMana);

        double manaScore = Math.sqrt(effectiveMana);

        SpellRarity rarity = spell.getRarity(1);
        double rarityMultiplier = 1.0 + (rarity.getValue() * 0.2);

        int maxLv = Math.max(1, spell.getMaxLevel());
        double levelMultiplier = 1.0 + (Math.sqrt(maxLv) * 0.4);

        double totalCost = manaScore * rarityMultiplier * levelMultiplier * factor;
        return Math.max(1, (int) Math.round(totalCost));
    }

    // ============================================================
    // 施法会话
    // ============================================================
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
    @Override
    public String getDescriptionId() {
        AbstractSpell spell = spellSupplier.get();
        return spell != null ? spell.getComponentId() : super.getDescriptionId();
    }

    @Override
    public void addDetail(RegistryAccess access, List<Component> list) {
        AbstractSpell spell = spellSupplier.get();
        if (spell == null) {
            super.addDetail(access, list);
            return;
        }
        // 用 Iron's 的 getUniqueInfo 显示伤害/半径/持续时间等信息
        try {
            List<MutableComponent> info = spell.getUniqueInfo(1, null);
            for (MutableComponent line : info) {
                list.add(line.withStyle(ChatFormatting.GRAY));
            }
        } catch (Throwable ignored) {
            // 兜底：极少数法术的 getUniqueInfo 可能对 null caster 敏感
            list.add(Component.translatable(spell.getComponentId()).withStyle(ChatFormatting.GRAY));
        }
    }
}