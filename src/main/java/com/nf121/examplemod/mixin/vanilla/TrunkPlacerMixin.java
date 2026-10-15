package com.nf121.examplemod.mixin.vanilla;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrunkPlacer.class)
public abstract class TrunkPlacerMixin {

    @Inject(method = "getTreeHeight", at = @At("RETURN"), cancellable = true)
    private void makeTreesTaller(RandomSource random, CallbackInfoReturnable<Integer> cir) {
        int originalHeight = cir.getReturnValue();
        
        // 增高逻辑示例：原高度 * 1.5 + 3 格（可自行调整系数）
        int newHeight = (int) (originalHeight * 1.5) + 3;
        
        cir.setReturnValue(newHeight);
    }
}