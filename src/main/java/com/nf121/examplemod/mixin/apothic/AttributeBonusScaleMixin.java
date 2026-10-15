package com.nf121.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.bonus.AttributeBonus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = AttributeBonus.class, remap = false)
public abstract class AttributeBonusScaleMixin {

    @Shadow
    @Final
    @Mutable
    protected Map<Purity, Double> values;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void examplemod$scaleValues(CallbackInfo ci) {
        Map<Purity, Double> scaled = new HashMap<>();

        this.values.forEach((purity, value) ->
                scaled.put(purity, value * 0.5D)
        );

        this.values = scaled;
    }
}
