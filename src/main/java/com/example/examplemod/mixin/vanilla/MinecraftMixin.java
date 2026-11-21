package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.menu.enderinterface.MultiWindowClient;
import com.example.examplemod.menu.enderinterface.MultiWindowScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void interceptRemoteScreen(Screen incoming, CallbackInfo ci) {
        if (incoming instanceof AbstractContainerScreen<?> cs) {
            if (MultiWindowClient.interceptOpen(cs)) {
                ci.cancel();
                return;
            }
        }
        if (incoming != null && !(incoming instanceof MultiWindowScreen) && MultiWindowClient.isDesktopShowing()) {
            MultiWindowClient.onDesktopPreempted();
        }
    }
}