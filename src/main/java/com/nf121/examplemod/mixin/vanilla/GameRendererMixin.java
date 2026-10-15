package com.nf121.examplemod.mixin.vanilla;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    @Inject(
            method = "getFov",
            at = @At("HEAD"),
            cancellable = true
    )
    private void examplemod$fixedFov(
            Camera camera,
            float partialTick,
            boolean useSetting,
            CallbackInfoReturnable<Double> cir
    ) {
        // 世界渲染采用视频设置里的 FOV。
        // 手部渲染等 useSetting=false 的调用保留原版基础值 70。
        double fov = useSetting
                ? Minecraft.getInstance().options.fov().get().doubleValue()
                : 70.0D;

        cir.setReturnValue(fov);
    }

    @Inject(
            method = "bobHurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void examplemod$disableHurtShake(
            PoseStack poseStack,
            float partialTick,
            CallbackInfo ci
    ) {
        ci.cancel();
    }

    @Inject(
            method = "bobView",
            at = @At("HEAD"),
            cancellable = true
    )
    private void examplemod$disableViewBobbing(
            PoseStack poseStack,
            float partialTick,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}