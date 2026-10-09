package com.example.examplemod.mixin.villna;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(
        method = "renderFire",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void examplemod$hideFireOverlay(
        Minecraft minecraft,
        PoseStack poseStack,
        CallbackInfo ci
    ) {
        ci.cancel();
    }
}
