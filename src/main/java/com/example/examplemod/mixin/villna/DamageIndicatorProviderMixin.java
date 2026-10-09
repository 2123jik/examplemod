package com.example.examplemod.mixin.villna;

import com.example.examplemod.client.particle.DamageSparkParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CritParticle.DamageIndicatorProvider.class)
public abstract class DamageIndicatorProviderMixin {
    @Shadow
    @Final
    private SpriteSet sprite;

    @Inject(
        method =
            "createParticle("
            + "Lnet/minecraft/core/particles/SimpleParticleType;"
            + "Lnet/minecraft/client/multiplayer/ClientLevel;"
            + "DDDDDD"
            + ")Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true,
        require = 1
    )
    private void hitfx$createDamageSpark(
        SimpleParticleType type,
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        CallbackInfoReturnable<Particle> cir
    ) {
        cir.setReturnValue(new DamageSparkParticle(
            level,
            x, y, z,
            xSpeed, ySpeed, zSpeed,
            this.sprite
        ));
    }
}
