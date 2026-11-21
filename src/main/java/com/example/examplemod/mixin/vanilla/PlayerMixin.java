package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.menu.enderinterface.MultiMenuManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z")
    )
    private boolean bypassTickStillValid(AbstractContainerMenu menu, Player player, Operation<Boolean> original) {
        if (MultiMenuManager.isRemoteMenuValid(menu, player)) return true;
        return original.call(menu, player);
    }

    @Inject(method = "canInteractWithBlock(Lnet/minecraft/core/BlockPos;D)Z", at = @At("HEAD"), cancellable = true)
    private void bypassBlockRange(BlockPos pos, double distance, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide && MultiMenuManager.hasRemoteSessionAt(self, pos)) {
            cir.setReturnValue(true);
        }
    }
    @Inject(method = "canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z", at = @At("HEAD"), cancellable = true)
    private void bypassEntityRange(net.minecraft.world.phys.AABB aabb, double distance, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide && MultiMenuManager.hasRemoteEntityNear(self, aabb)) {
            cir.setReturnValue(true);
        }
    }
}