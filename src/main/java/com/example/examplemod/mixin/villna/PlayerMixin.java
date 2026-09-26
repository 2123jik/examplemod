package com.example.examplemod.mixin.villna;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "die", at = @At("TAIL"))
    private void examplemod$resetExperienceOnDeath(
            DamageSource source,
            CallbackInfo callbackInfo
    ) {
        Player player = (Player) (Object) this;

        // 只让服务端修改，避免客户端和服务端各执行一次
        if (player.level().isClientSide()) {
            return;
        }

        // 被 LivingDeathEvent 取消等情况下，不处理经验
        if (!player.isDeadOrDying()) {
            return;
        }

        // 清空当前等级经验条
        player.experienceProgress = 0.0F;

        // 等级降低一级，但不低于 0
        player.experienceLevel = Math.max(0, player.experienceLevel - 1);
    }
}