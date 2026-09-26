package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.bonus.DamageReductionBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * 将神化免伤宝石（DamageReductionBonus）的减伤率降低为 1/4
 */
@Mixin(value = DamageReductionBonus.class, remap = false)
public class DamageReductionBonusMixin {

    @Redirect(
        method = {"onHurt", "getSocketBonusTooltip"},
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object scaleDamageReduction(Map<?, ?> map, Object key) {
        Object val = map.get(key);
        if (val instanceof Float f) {
            return f * 0.25F; // 免伤率变为原来的 1/4 (如 20% 减免 -> 5% 减免)
        }
        return val;
    }
}