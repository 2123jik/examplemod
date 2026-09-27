package com.example.examplemod.mixin.villna;

import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomSpreadStructurePlacement.class)
public class RandomSpreadPlacementMixin {

    @Shadow @Final @Mutable private int spacing;
    @Shadow @Final @Mutable private int separation;

    // 2 = 4 倍密度；3 = 9 倍密度；1 = 不变
    private static final int DENSITY_MULTIPLIER = 2;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void examplemod$denserPlacement(CallbackInfo ci) {
        int newSpacing = Math.max(2, this.spacing / DENSITY_MULTIPLIER);
        int newSeparation = Math.max(0, Math.min(newSpacing - 1, this.separation / DENSITY_MULTIPLIER));
        this.spacing = newSpacing;
        this.separation = newSeparation;
    }
}