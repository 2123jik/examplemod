package com.nf121.examplemod.mixin.vanilla;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = 500) // 设置较高优先级（数字越小优先级越高），确保最先运行 Guard 逻辑
public class EntityNullGuardMixin {

    @Inject(
        method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void preventNullAlliedToCheck(Entity other, CallbackInfoReturnable<Boolean> cir) {
        // 如果传入的实体为 null，直接拦截并安全返回 false，防止调用其 getTeam()
        if (other == null) {
            cir.setReturnValue(false);
        }
    }
}