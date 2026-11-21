package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.menu.enderinterface.MultiWindowClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void skipChildDarkOverlay(GuiGraphics gg, CallbackInfo ci) {
        if (MultiWindowClient.isRenderingChild()) ci.cancel();
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void skipChildBlur(float partialTick, CallbackInfo ci) {
        if (MultiWindowClient.isRenderingChild()) ci.cancel();
    }
}