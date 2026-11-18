package com.example.examplemod.mixin.vanilla;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void killAllCritParticles(
            ParticleOptions particleData, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            CallbackInfoReturnable<Particle> cir
    ) {
        var type = particleData.getType();

        // 无论任何人（服务端、假人Mod、附魔武器、原版指令）调用，直接截断返回 null
        if (type == ParticleTypes.CRIT || 
            type == ParticleTypes.ENCHANTED_HIT || 
            type == ParticleTypes.DAMAGE_INDICATOR) {
            
            cir.setReturnValue(null);
        }
    }
}