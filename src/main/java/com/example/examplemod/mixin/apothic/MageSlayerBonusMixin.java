package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.bonus.special.MageSlayerBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * 将弑法者宝石（MageSlayerBonus）的魔法吸收与回血比例降低为 1/4
 */
@Mixin(value = MageSlayerBonus.class, remap = false)
public class MageSlayerBonusMixin {

    @Redirect(
        method = {"onHurt", "getSocketBonusTooltip"},
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object scaleMageSlayer(Map<?, ?> map, Object key) {
        Object val = map.get(key);
        if (val instanceof Float f) {
            return f * 0.25F; // 魔法减免与治疗效果同步降低至 1/4
        }
        return val;
    }
}