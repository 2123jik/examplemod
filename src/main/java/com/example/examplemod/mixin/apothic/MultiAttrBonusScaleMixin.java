package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.MultiAttrBonus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = MultiAttrBonus.ModifierInst.class, remap = false)
public abstract class MultiAttrBonusScaleMixin {

    @Shadow
    @Final
    @Mutable
    private Map<Purity, Float> values;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void examplemod$scaleValues(CallbackInfo ci) {
        Map<Purity, Float> scaled = new HashMap<>();

        this.values.forEach((purity, value) ->
                scaled.put(purity, value * 0.5F)
        );

        this.values = scaled;
    }
}
