package com.example.examplemod.mixin.vanilla;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public class ExplosionParticleMixin {

//    // 拦截 finalizeExplosion 内部的 addParticle 调用
//    @Redirect(
//            method = "finalizeExplosion",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
//            )
//    )
//    private void cancelUglyExplosionParticles(Level level, ParticleOptions particle, double x, double y, double z, double vx, double vy, double vz) {
//        // 彻底丢弃原版烟雾与灰尘，把渲染权全权交给我们的几何冲击波！
//    }
}