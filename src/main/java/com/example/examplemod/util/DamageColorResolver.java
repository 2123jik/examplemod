package com.example.examplemod.util;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.HashMap;
import java.util.Map;

public class DamageColorResolver {

    private static final Map<ResourceKey<DamageType>, Integer> SCHOOL_COLOR_CACHE = new HashMap<>();
    private static boolean initialized = false;

    // 统一默认色（无须任何原版琐碎匹配）
    private static final int DEFAULT_COLOR = 0xFFFFFF;      // 普通伤害：明亮白
    private static final int DEFAULT_CRIT_COLOR = 0xFF6600; // 物理暴击：烈焰橙

    /**
     * 惰性构建 O(1) 查找表，避免每次受击都遍历 Registry
     */
    private static void ensureInitialized() {
        if (initialized) return;
        for (SchoolType school : SchoolRegistry.REGISTRY) {
            ResourceKey<DamageType> damageType = school.getDamageType();
            if (damageType != null) {
                TextColor color = school.getDisplayName().getStyle().getColor();
                int rgb = color != null ? color.getValue() : DEFAULT_COLOR;
                SCHOOL_COLOR_CACHE.put(damageType, rgb);
            }
        }
        initialized = true;
    }

    public static int resolve(DamageSource source, boolean isCrit) {
        ensureInitialized();

        // 1. 直接通过 DamageSource 的 Key 查表，O(1) 效率极高
        var typeKey = source.typeHolder().unwrapKey().orElse(null);
        if (typeKey != null && SCHOOL_COLOR_CACHE.containsKey(typeKey)) {
            return SCHOOL_COLOR_CACHE.get(typeKey);
        }

        // 2. 不匹配任何法术体系 -> 走极简默认规则
        return isCrit ? DEFAULT_CRIT_COLOR : DEFAULT_COLOR;
    }
}