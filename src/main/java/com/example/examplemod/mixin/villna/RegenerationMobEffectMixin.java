package com.example.examplemod.mixin.villna;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.effect.RegenerationMobEffect")
public class RegenerationMobEffectMixin {

    @Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
    private void examplemod$percentHeal(LivingEntity entity, int amplifier,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(entity.getMaxHealth() * 0.05F);
        }
        cir.setReturnValue(true);
    }
}
