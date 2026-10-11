package com.example.examplemod.mixin.vanilla;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.effect.PoisonMobEffect")
public class MixinPoisonMobEffect {

    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean modifyPoisonDamage(LivingEntity entity, DamageSource source, float amount) {
        float newAmount = entity.getMaxHealth() * 0.005F;

        return entity.hurt(source, Math.max(newAmount, 1.0F));
    }
}