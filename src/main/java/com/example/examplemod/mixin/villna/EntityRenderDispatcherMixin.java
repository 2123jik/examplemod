package com.example.examplemod.mixin.villna;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(
        method = "renderFlame",
        at = @At("HEAD"),
        cancellable = true
    )
    private void examplemod$hideEntityFire(CallbackInfo ci) {
        ci.cancel();
    }
}
