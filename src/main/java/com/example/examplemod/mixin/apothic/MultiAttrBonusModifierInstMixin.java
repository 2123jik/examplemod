package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.bonus.MultiAttrBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = MultiAttrBonus.ModifierInst.class, remap = false)
public class MultiAttrBonusModifierInstMixin {

    /**
     * 拦截 build 方法里的 Map.get：
     * 因为 addModifiers 和 getSocketBonusTooltip 都统一调用 build 方法，
     * 所以只需缩放这里，实际属性与 Tooltip 显示自动完全同步为 0.25x！
     */
    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private Object scaleMultiAttrBonusValue(Map<?, ?> map, Object key) {
        Object val = map.get(key);
        if (val instanceof Float f) {
            return f * 0.25F;
        }
        if (val instanceof Double d) {
            return d * 0.25D;
        }
        return val;
    }
}