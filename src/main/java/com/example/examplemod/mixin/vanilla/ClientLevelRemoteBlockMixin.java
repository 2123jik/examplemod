package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.menu.enderinterface.ClientRemoteBlockCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ClientLevelRemoteBlockMixin {
    @Shadow @Final public boolean isClientSide;

    @Inject(method = "isLoaded", at = @At("RETURN"), cancellable = true)
    private void enderinterface$isLoaded(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.isClientSide && ClientRemoteBlockCache.hasAny() && !cir.getReturnValueZ()) {
            if (ClientRemoteBlockCache.hasShadow((Level) (Object) this, pos)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void enderinterface$getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (this.isClientSide && ClientRemoteBlockCache.hasAny()) {
            BlockState shadow = ClientRemoteBlockCache.getShadowState((Level) (Object) this, pos);
            if (shadow != null) {
                cir.setReturnValue(shadow);
            }
        }
    }

    @Inject(method = "getBlockEntity", at = @At("RETURN"), cancellable = true)
    private void enderinterface$getBlockEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        if (this.isClientSide && ClientRemoteBlockCache.hasAny()) {
            BlockEntity shadow = ClientRemoteBlockCache.getShadowBlockEntity((Level) (Object) this, pos, cir.getReturnValue());
            if (shadow != null) {
                cir.setReturnValue(shadow);
            }
        }
    }
}