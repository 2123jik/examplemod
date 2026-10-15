package com.nf121.examplemod.compat.apothic;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class CurioEffectHooks {

    /*
     * 只防止本兼容层转发攻击效果时再次进入自己。
     * 不取消伤害，不修改神化自身的事件处理。
     */
    private static final ThreadLocal<Boolean> POST_EFFECTS_ACTIVE =
            ThreadLocal.withInitial(() -> false);

    private CurioEffectHooks() {
    }

    public static float protection(
            LivingEntity wearer,
            DamageSource source
    ) {
        float result = 0.0F;

        for (WornCurios.Entry entry : WornCurios.entries(wearer)) {
            ItemStack stack = entry.stack();

            result += SocketHelper.getGems(stack)
                    .getDamageProtection(source);

            for (AffixInstance inst :
                    AffixHelper.streamAffixes(stack).toList()) {
                result += inst.getDamageProtection(source);
            }
        }

        return result;
    }

    public static float damageBonus(
            LivingEntity wearer,
            Entity target
    ) {
        float result = 0.0F;

        for (WornCurios.Entry entry : WornCurios.entries(wearer)) {
            ItemStack stack = entry.stack();

            result += SocketHelper.getGems(stack)
                    .getDamageBonus(target);

            for (AffixInstance inst :
                    AffixHelper.streamAffixes(stack).toList()) {
                result += inst.getDamageBonus(target);
            }
        }

        return result;
    }

    public static float onHurt(
            LivingEntity wearer,
            DamageSource source,
            float amount
    ) {
        for (WornCurios.Entry entry : WornCurios.entries(wearer)) {
            ItemStack stack = entry.stack();

            amount = SocketHelper.getGems(stack)
                    .onHurt(source, wearer, amount);

            for (AffixInstance inst :
                    AffixHelper.streamAffixes(stack).toList()) {
                amount = inst.onHurt(source, wearer, amount);
            }
        }

        return amount;
    }

    public static float onShieldBlock(
            LivingEntity wearer,
            DamageSource source,
            float blocked
    ) {
        for (WornCurios.Entry entry : WornCurios.entries(wearer)) {
            ItemStack stack = entry.stack();

            blocked = SocketHelper.getGems(stack)
                    .onShieldBlock(wearer, source, blocked);

            for (AffixInstance inst :
                    AffixHelper.streamAffixes(stack).toList()) {
                blocked = inst.onShieldBlock(wearer, source, blocked);
            }
        }

        return blocked;
    }

    public static void onBlockBreak(
            Player player,
            LevelAccessor level,
            BlockPos pos,
            BlockState state
    ) {
        for (WornCurios.Entry entry : WornCurios.entries(player)) {
            ItemStack stack = entry.stack();

            SocketHelper.getGems(stack)
                    .onBlockBreak(player, level, pos, state);

            AffixHelper.streamAffixes(stack).forEach(inst ->
                    inst.onBlockBreak(player, level, pos, state)
            );
        }
    }

    public static void modifyEntityLoot(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        for (WornCurios.Entry entry : WornCurios.entries(player)) {
            AffixHelper.streamAffixes(entry.stack())
                    .forEach(inst -> inst.modifyEntityLoot(event));
        }
    }

    public static void postEffects(
            Entity target,
            DamageSource source
    ) {
        if (POST_EFFECTS_ACTIVE.get()) {
            return;
        }

        POST_EFFECTS_ACTIVE.set(true);

        try {
            if (source.getEntity() instanceof LivingEntity attacker
                    && !attacker.level().isClientSide()) {
                postAttack(attacker, target);
            }

            if (target instanceof LivingEntity victim
                    && !victim.level().isClientSide()) {
                postHurt(victim, source);
            }
        } finally {
            POST_EFFECTS_ACTIVE.remove();
        }
    }

    private static void postAttack(
            LivingEntity attacker,
            Entity target
    ) {
        for (WornCurios.Entry entry : WornCurios.entries(attacker)) {
            ItemStack stack = entry.stack();

            /*
             * 与神化 SocketedGems.doPostAttack 的行为一致，
             * 但使用 finally 恢复无敌帧，避免异常留下错误状态。
             */
            SocketHelper.getGems(stack).streamValidGems()
                    .forEach(gem -> {
                        int previous = target.invulnerableTime;

                        try {
                            target.invulnerableTime = 0;
                            gem.doPostAttack(attacker, target);
                        } finally {
                            target.invulnerableTime = previous;
                        }
                    });

            AffixHelper.streamAffixes(stack).forEach(inst -> {
                int previous = target.invulnerableTime;

                try {
                    target.invulnerableTime = 0;
                    inst.doPostAttack(attacker, target);
                } finally {
                    target.invulnerableTime = previous;
                }
            });
        }
    }

    private static void postHurt(
            LivingEntity victim,
            DamageSource source
    ) {
        for (WornCurios.Entry entry : WornCurios.entries(victim)) {
            ItemStack stack = entry.stack();

            SocketHelper.getGems(stack)
                    .doPostHurt(victim, source);

            AffixHelper.streamAffixes(stack).forEach(inst ->
                    inst.doPostHurt(victim, source)
            );
        }
    }
}
