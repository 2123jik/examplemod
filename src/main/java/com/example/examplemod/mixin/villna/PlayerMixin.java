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

        if (player.level().isClientSide()) {
            return;
        }

        if (!player.isDeadOrDying()) {
            return;
        }

        player.experienceProgress = 0.0F;

        player.experienceLevel = Math.max(0, player.experienceLevel - 1);
    }
}