package com.nf121.examplemod.mixin.vanilla;

import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomSpreadStructurePlacement.class)
public class RandomSpreadPlacementMixin {

    @Shadow @Final @Mutable private int spacing;
    @Shadow @Final @Mutable private int separation;

    @Unique
    private static final int DENSITY_REDUCTION = 2;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void examplemod$denserPlacement(CallbackInfo ci) {
        // 1. Calculate ideal reduced separation, keeping it >= 0
        int targetSeparation = Math.max(0, this.separation - DENSITY_REDUCTION);

        // 2. Calculate ideal reduced spacing, ensuring spacing >= targetSeparation + 1
        int targetSpacing = Math.max(targetSeparation + 1, this.spacing - DENSITY_REDUCTION);

        // 3. Ensure spacing never drops below 2
        this.spacing = Math.max(2, targetSpacing);

        this.separation = Math.min(targetSeparation, this.spacing - 1);
    }
}