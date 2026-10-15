package com.nf121.examplemod.mixin.viScriptShop;

import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ViScriptShopServerUtil.class, remap = false)
public abstract class ViScriptShopServerUtilMixin {

    @Inject(
            method = "serverOpenShop(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)V",
            at = @At("HEAD"),
            remap = false
    )
    private static void examplemod$reloadBeforeOpen(
            ServerPlayer player,
            String shopLocation,
            CallbackInfo ci
    ) {
        ViScriptShopServerUtil.reloadOpenShop(shopLocation);
    }
}