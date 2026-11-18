package com.example.examplemod.util;

import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

public final class NativeSpellDamageContext {

    public record Hit(
            LivingEntity target,
            SpellDamageSource source,
            float attackPower,
            float initialResistance
    ) {}

    /*
     * PENDING 只负责将数据交给紧接着进入的 hurt()。
     * CURRENT 只代表当前 hurt() 对应的数据。
     */
    private static final ThreadLocal<Hit> PENDING = new ThreadLocal<>();
    private static final ThreadLocal<Hit> CURRENT = new ThreadLocal<>();

    private NativeSpellDamageContext() {}

    public static Hit current() {
        return CURRENT.get();
    }

    public static boolean withNativeHit(
            LivingEntity target,
            SpellDamageSource source,
            float attackPower,
            float initialResistance,
            Supplier<Boolean> action
    ) {
        Hit previous = PENDING.get();

        PENDING.set(new Hit(
                target,
                source,
                attackPower,
                initialResistance
        ));

        try {
            return action.get();
        } finally {
            restore(PENDING, previous);
        }
    }

    public static boolean withinHurt(
            LivingEntity target,
            DamageSource source,
            Supplier<Boolean> action
    ) {
        Hit pending = PENDING.get();

        // 只允许消费一次，防止嵌套伤害重复使用。
        PENDING.remove();

        Hit previous = CURRENT.get();

        if (pending != null
                && pending.target() == target
                && pending.source() == source) {
            CURRENT.set(pending);
        } else {
            /*
             * 即使当前伤害没有原生上下文，也必须清空当前值，
             * 防止它读到外层法术伤害的数据。
             */
            CURRENT.remove();
        }

        try {
            return action.get();
        } finally {
            restore(CURRENT, previous);
        }
    }

    private static void restore(ThreadLocal<Hit> local, Hit value) {
        if (value == null) {
            local.remove();
        } else {
            local.set(value);
        }
    }
}
