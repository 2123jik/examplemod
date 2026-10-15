package com.nf121.examplemod.compat.l2hostility;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

import java.util.Set;

public final class SpellAffixPolicy {

    public static final int REQUIRED_VISIBLE_TICKS = 10;
    public static final int INITIAL_DELAY_TICKS = 20;
    public static final int GLOBAL_RECOVERY_TICKS = 20;
    public static final int FAILED_ATTEMPT_DELAY_TICKS = 10;

    /*
     * 这是 AI 使用距离，不是法术真实射程。
     * 默认法术使用 24 格，上限同时受 FOLLOW_RANGE 限制。
     */
    private static final double DEFAULT_RANGE = 24.0;

    /*
     * 明确作为自身强化处理。
     * 开始施放需要看到敌人，施放过程中不要求继续看到敌人。
     */
    private static final Set<String> SELF_BUFFS = Set.of(
            "irons_spellbooks:oakskin",
            "irons_spellbooks:spider_aspect",
            "irons_spellbooks:echoing_strikes",
            "irons_spellbooks:heartstop"
    );

    private static final Set<String> SELF_HEALING = Set.of(
            "irons_spellbooks:healing_circle"
    );

    private static final Set<String> SELF_DEFENSE = Set.of(
            "irons_spellbooks:ice_block"
    );

    private static final Set<String> CLOSE_RANGE = Set.of(
            "irons_spellbooks:fang_swirl",
            "irons_spellbooks:fang_ward",
            "irons_spellbooks:gust",
            "irons_spellbooks:shadow_slash",
            "irons_spellbooks:flaming_strike",
            "irons_spellbooks:heat_surge",
            "irons_spellbooks:divine_smite",
            "irons_spellbooks:poison_splash",
            "irons_spellbooks:stomp",
            "irons_spellbooks:shockwave"
    );

    private static final Set<String> MEDIUM_RANGE = Set.of(
            "irons_spellbooks:cone_of_cold",
            "irons_spellbooks:frostwave",
            "irons_spellbooks:dragon_breath",
            "irons_spellbooks:fire_breath",
            "irons_spellbooks:poison_breath",
            "irons_spellbooks:electrocute",
            "irons_spellbooks:burning_dash",
            "irons_spellbooks:charge"
    );

    private SpellAffixPolicy() {
    }

    public static boolean isSupported(AbstractSpell spell) {
        return spell != null && spell != SpellRegistry.none();

    }

    public static boolean requiresTargetThroughout(AbstractSpell spell) {
        String id = spell.getSpellId();

        return !SELF_BUFFS.contains(id)
                && !SELF_HEALING.contains(id)
                && !SELF_DEFENSE.contains(id);
    }

    public static double getRange(Mob mob, AbstractSpell spell) {
        String id = spell.getSpellId();

        double range;
        if (CLOSE_RANGE.contains(id)) {
            range = 6.0;
        } else if (MEDIUM_RANGE.contains(id)) {
            range = 12.0;
        } else {
            range = DEFAULT_RANGE;
        }

        var followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            range = Math.min(range, Math.max(0.0, followRange.getValue()));
        }

        return range;
    }

    public static boolean isValidEnemy(Mob mob, LivingEntity target) {
        if (target == null
                || target == mob
                || !target.isAlive()
                || target.isRemoved()
                || target.level() != mob.level()
                || target.isSpectator()) {
            return false;
        }

        if (target instanceof Player player && player.isCreative()) {
            return false;
        }

        return !mob.isAlliedTo(target) && mob.canAttack(target);
    }

    public static boolean hasClearSight(Mob mob, LivingEntity target) {
        /*
         * 原版可见性 + 实际碰撞射线。
         *
         * 不依赖 Sensing 的缓存；玻璃等具有碰撞形状的方块
         * 也会阻止这里的攻击施法。
         *
         * 检测眼睛到眼睛的线段，不限制怪物的朝向视角。
         */
        return mob.hasLineOfSight(target)
                && mob.level().clip(new ClipContext(
                        mob.getEyePosition(),
                        target.getEyePosition(),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        mob
                )).getType() == HitResult.Type.MISS;
    }

    public static boolean canObserve(
            Mob mob,
            LivingEntity target,
            AbstractSpell spell
    ) {
        if (!mob.isAlive()
                || mob.isRemoved()
                || mob.isNoAi()
                || !isValidEnemy(mob, target)) {
            return false;
        }

        double range = getRange(mob, spell);

        return range > 0.0
                && mob.distanceToSqr(target) <= range * range
                && hasClearSight(mob, target);
    }

    public static boolean canStart(
            Mob mob,
            LivingEntity target,
            AbstractSpell spell,
            int spellLevel
    ) {
        if (!isSupported(spell)
                || !spell.isEnabled()
                || !canObserve(mob, target, spell)) {
            return false;
        }

        String id = spell.getSpellId();

        if (SELF_HEALING.contains(id)
                && mob.getHealth() >= mob.getMaxHealth() * 0.75F) {
            return false;
        }

        if (SELF_DEFENSE.contains(id)
                && mob.getHealth() >= mob.getMaxHealth() * 0.50F) {
            return false;
        }

        if ("irons_spellbooks:counterspell".equals(id)) {
            if (!io.redspace.ironsspellbooks.api.magic.MagicData
                    .getPlayerMagicData(target).isCasting()) {
                return false;
            }
        }

        return !spell.shouldAIStopCasting(spellLevel, mob, target);
    }

    public static boolean canContinue(
            Mob mob,
            LivingEntity target,
            AbstractSpell spell,
            int spellLevel
    ) {
        if (!mob.isAlive()
                || mob.isRemoved()
                || mob.isNoAi()
                || !spell.isEnabled()) {
            return false;
        }

        if (!requiresTargetThroughout(spell)) {
            /*
             * 自身强化允许离开视线后完成。
             * 有合法目标时仍尊重法术自己的停止判断。
             */
            return !isValidEnemy(mob, target)
                    || !spell.shouldAIStopCasting(spellLevel, mob, target);
        }

        return canObserve(mob, target, spell)
                && !spell.shouldAIStopCasting(spellLevel, mob, target);
    }
}
