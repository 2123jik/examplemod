package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.AllStatsBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * 将全属性宝石（AllStatsBonus）的全局属性增幅降低为 1/4
 */
@Mixin(value = AllStatsBonus.class, remap = false)
public class AllStatsBonusMixin {

    @Redirect(
        method = {"addModifiers", "getSocketBonusTooltip"},
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object scaleAllStats(Map<?, ?> map, Object key) {
        Object val = map.get(key);
        if (val instanceof Float f) {
            return f * 0.25F; // 所有关联属性的加成统一降低为 1/4
        }
        return val;
    }
}