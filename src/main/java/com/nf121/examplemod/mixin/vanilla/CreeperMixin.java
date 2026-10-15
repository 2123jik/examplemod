package com.nf121.examplemod.mixin.vanilla;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Creeper.class)
public abstract class CreeperMixin {

    @ModifyArg(
        method = "explodeCreeper",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(" +
                     "Lnet/minecraft/world/entity/Entity;" +
                     "DDDF" +
                     "Lnet/minecraft/world/level/Level$ExplosionInteraction;" +
                     ")Lnet/minecraft/world/level/Explosion;"
        ),
        index = 4
    )
    private float examplemod$scaleExplosionByHealth(float originalRadius) {
        Creeper creeper = (Creeper) (Object) this;

        float healthPercentage = creeper.getHealth() / creeper.getMaxHealth();
        healthPercentage = Mth.clamp(healthPercentage, 0.0F, 1.0F);

        return originalRadius * healthPercentage;
    }
}
