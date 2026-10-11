package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RangedAttribute.class)
public class RangedAttributeMixin {

    @ModifyVariable(
            method = "<init>",                  at = @At("HEAD"),                   ordinal = 2,                        argsOnly = true             )
    private static double modifyMaxValueInConstructor(double max) {
        if (
                max == 1024.0 ||
                max == 2048.0
        ) {
            return Math.min(Double.MAX_VALUE, max*100000000) ;
        }
        return max;
    }
}