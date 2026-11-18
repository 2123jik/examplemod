package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Noises;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets =
    "net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise"
)
public abstract class ShiftedNoiseMixin {

    /**
     * 1.0：原版尺度
     * 2.0：水平变化频率约为原来的两倍
     * 4.0：水平变化频率约为原来的四倍
     */
    @Unique
    private static final double examplemod$FREQUENCY = 4.0D;

    @Shadow
    public abstract DensityFunction.NoiseHolder noise();

    @Unique
    private boolean examplemod$isClimateNoise() {
        var holder = noise().noiseData();

        return holder.is(Noises.TEMPERATURE)
            || holder.is(Noises.TEMPERATURE_LARGE)
            || holder.is(Noises.VEGETATION)
            || holder.is(Noises.VEGETATION_LARGE)
            || holder.is(Noises.CONTINENTALNESS)
            || holder.is(Noises.CONTINENTALNESS_LARGE)
            || holder.is(Noises.EROSION)
            || holder.is(Noises.EROSION_LARGE)
            || holder.is(Noises.RIDGE);
    }

    @ModifyArg(
        method =
            "compute(Lnet/minecraft/world/level/levelgen/"
            + "DensityFunction$FunctionContext;)D",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/level/levelgen/"
                + "DensityFunction$NoiseHolder;getValue(DDD)D"
        ),
        index = 0,
        require = 1
    )
    private double examplemod$increaseXFrequency(double noiseX) {
        return examplemod$isClimateNoise()
            ? noiseX * examplemod$FREQUENCY
            : noiseX;
    }

    @ModifyArg(
        method =
            "compute(Lnet/minecraft/world/level/levelgen/"
            + "DensityFunction$FunctionContext;)D",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/level/levelgen/"
                + "DensityFunction$NoiseHolder;getValue(DDD)D"
        ),
        index = 2,
        require = 1
    )
    private double examplemod$increaseZFrequency(double noiseZ) {
        return examplemod$isClimateNoise()
            ? noiseZ * examplemod$FREQUENCY
            : noiseZ;
    }
}
