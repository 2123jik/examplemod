package com.example.examplemod.mixin.villna;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({GameRenderer.class})
public class GameRendererMixin {
    @ModifyReturnValue(
        method = {"getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"},
        at = {@At("RETURN")}
    )
    private static float getNightVisionScale(float original, LivingEntity entity, float f) {
        MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null && effect.endsWithin(240)) {
            return 1;
        } else {
            return original;
        }
    }
}