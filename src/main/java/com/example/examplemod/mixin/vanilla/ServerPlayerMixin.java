package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.menu.enderinterface.L2MenuStackerCompat;
import com.example.examplemod.menu.enderinterface.MultiMenuManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void tickRemoteMenus(CallbackInfo ci) {
        MultiMenuManager.tickPlayer((ServerPlayer) (Object) this);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z")
    )
    private boolean bypassTickStillValid(AbstractContainerMenu menu, Player player, Operation<Boolean> original) {
        if (MultiMenuManager.isRemoteMenuValid(menu, player)) return true;
        return original.call(menu, player);
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;",
            at = @At("HEAD"),
            remap = false
    )
    private void beforeSubMenuOpen(net.minecraft.world.MenuProvider pvd, java.util.function.Consumer<?> writer,
                                   org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<java.util.OptionalInt> cir) {
        ServerPlayer sp = (ServerPlayer) (Object) this;
        if (MultiMenuManager.isOpeningByInterface()) {
            L2MenuStackerCompat.clearServerTempOnly(sp);
        }
        MultiMenuManager.onBeforeSubMenuOpen(sp);
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;",
            at = @At("RETURN"),
            remap = false
    )
    private void afterSubMenuOpen(net.minecraft.world.MenuProvider pvd, java.util.function.Consumer<?> writer,
                                  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<java.util.OptionalInt> cir) {
        MultiMenuManager.onAfterSubMenuOpen((ServerPlayer) (Object) this, cir.getReturnValue().isPresent());
    }
}