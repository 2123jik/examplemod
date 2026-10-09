package com.example.examplemod.compat.l2hostility;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

public class AutoSpellTrait extends MobTrait {

    private static final Logger LOGGER = LogUtils.getLogger();

    /*
     * 仅服务端 tick 访问。
     * 同一个 mob 的所有 AutoSpellTrait 共享公共恢复时间。
     */
    private static final Map<Mob, MobState> STATES = new WeakHashMap<>();

    private static final Set<String> TOOLTIP_ERRORS = new HashSet<>();

    private final Supplier<? extends AbstractSpell> spellSupplier;

    public AutoSpellTrait(Supplier<? extends AbstractSpell> spellSupplier) {
        super(() -> SpellTraitHelper.getDynamicColor(spellSupplier.get()));
        this.spellSupplier = spellSupplier;
    }

    @Override
    public void tick(LivingEntity entity, int traitLevel) {
        if (entity.level().isClientSide()
                || !(entity instanceof PathfinderMob mob)
                || !(mob instanceof IMagicEntity magicMob)) {
            return;
        }

        /*
         * 原生法师有自己的生命周期、Goal、Boss 阶段和动画控制。
         * 本实现不向它们的状态机插入词缀施法。
         */
        if (mob instanceof AbstractSpellCastingMob) {
            return;
        }

        if (!mob.isAlive() || mob.isRemoved() || mob.isNoAi()) {
            return;
        }

        AbstractSpell spell = spellSupplier.get();
        if (!SpellAffixPolicy.isSupported(spell) || !spell.isEnabled()) {
            return;
        }

        long now = mob.level().getGameTime();

        MobState state = STATES.computeIfAbsent(
                mob,
                ignored -> new MobState(
                        now + SpellAffixPolicy.INITIAL_DELAY_TICKS
                                + mob.getRandom().nextInt(20)
                )
        );

        /*
         * 即使同一 tick 有多个词缀，公共状态只更新一次。
         */
        if (state.lastUpdate != now) {
            state.lastUpdate = now;

            boolean busy = magicMob.isCasting() || magicMob.isDrinkingPotion();

            if (state.wasBusy && !busy) {
                state.nextGlobalAttempt = Math.max(
                        state.nextGlobalAttempt,
                        now + SpellAffixPolicy.GLOBAL_RECOVERY_TICKS
                );
            }

            state.wasBusy = busy;
        }

        String spellId = spell.getSpellId();
        SpellState spellState = state.spells.computeIfAbsent(
                spellId,
                ignored -> new SpellState()
        );

        LivingEntity target = mob.getTarget();

        /*
         * 即使冷却未结束，也持续观察目标。
         * 每个法术按自己的使用距离记录连续可见时间。
         */
        if (!SpellAffixPolicy.canObserve(mob, target, spell)) {
            spellState.resetObservation();
            return;
        }

        UUID targetId = target.getUUID();

        if (!targetId.equals(spellState.targetId)
                || spellState.lastSeenTick < now - 1) {
            spellState.targetId = targetId;
            spellState.visibleSince = now;
        }

        spellState.lastSeenTick = now;

        if (magicMob.isCasting() || magicMob.isDrinkingPotion()) {
            return;
        }

        if (now < state.nextGlobalAttempt
                || now < spellState.nextAttempt
                || now - spellState.visibleSince
                < SpellAffixPolicy.REQUIRED_VISIBLE_TICKS) {
            return;
        }

        /*
         * 减少多个词缀固定遍历顺序导致的长期偏向。
         * 这是随机尝试，不保证严格轮询公平。
         */
        if (mob.getRandom().nextInt(5) != 0) {
            return;
        }

        int maxTraitLevel = Math.max(
                1,
                getMaxLevel(mob.level().registryAccess())
        );

        int spellLevel = SpellTraitHelper.scaleSpellLevel(
                traitLevel,
                maxTraitLevel,
                spell
        );

        spellLevel = Math.max(1, Math.min(spellLevel, spell.getMaxLevel()));

        if (!SpellAffixPolicy.canStart(mob, target, spell, spellLevel)) {
            spellState.nextAttempt =
                    now + SpellAffixPolicy.FAILED_ATTEMPT_DELAY_TICKS;
            return;
        }

        /*
         * 只有 initiateCastSpell 负责调用 checkPreCastConditions。
         * 不在这里重复调用，避免第三方法术重复设置 additionalCastData。
         */
        magicMob.initiateCastSpell(spell, spellLevel);

        var activeSpell = magicMob.getMagicData().getCastingSpell();

        if (magicMob.isCasting()
                && activeSpell != null
                && activeSpell.getSpell() == spell) {

            /*
             * 沿用现有 helper 的 tick 单位约定。
             * 不永久缓存配置结果，配置改变后能读取新冷却。
             */
            int cooldown = Math.max(
                    40,
                    SpellTraitHelper.getSafeCooldown(spell)
            );

            spellState.nextAttempt = now + cooldown;
            state.nextGlobalAttempt =
                    now + SpellAffixPolicy.GLOBAL_RECOVERY_TICKS;
            state.wasBusy = true;
        } else {
            spellState.nextAttempt =
                    now + SpellAffixPolicy.FAILED_ATTEMPT_DELAY_TICKS;
            state.nextGlobalAttempt = now + 5;
        }
    }

    @Override
    public String getDescriptionId() {
        AbstractSpell spell = spellSupplier.get();
        return spell != null
                ? spell.getComponentId()
                : super.getDescriptionId();
    }

    @Override
    public void addDetail(RegistryAccess access, List<Component> list) {
        AbstractSpell spell = spellSupplier.get();

        if (spell == null) {
            super.addDetail(access, list);
            return;
        }

        list.add(Component.translatable(spell.getComponentId() + ".guide")
                .withStyle(ChatFormatting.GRAY));

        try {
            for (MutableComponent line : spell.getUniqueInfo(1, null)) {
                list.add(line.copy().withStyle(ChatFormatting.GRAY));
            }
        } catch (RuntimeException exception) {
            /*
             * 部分法术的说明方法不接受 null 玩家。
             * 只记录一次，不吞掉 Error 等严重问题。
             */
            if (TOOLTIP_ERRORS.add(spell.getSpellId())) {
                LOGGER.warn(
                        "Unable to build mob spell trait tooltip for {}",
                        spell.getSpellId(),
                        exception
                );
            }
        }
    }

    private static final class MobState {
        private final Map<String, SpellState> spells = new HashMap<>();
        private long lastUpdate = Long.MIN_VALUE;
        private long nextGlobalAttempt;
        private boolean wasBusy;

        private MobState(long nextGlobalAttempt) {
            this.nextGlobalAttempt = nextGlobalAttempt;
        }
    }

    private static final class SpellState {
        private UUID targetId;
        private long visibleSince;
        private long lastSeenTick = Long.MIN_VALUE;
        private long nextAttempt;

        private void resetObservation() {
            targetId = null;
            visibleSince = 0;
            lastSeenTick = Long.MIN_VALUE;
        }
    }
}
