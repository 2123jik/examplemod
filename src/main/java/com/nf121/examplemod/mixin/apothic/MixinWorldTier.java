package com.nf121.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.xkmc.l2hostility.content.logic.DifficultyLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldTier.class)
public class MixinWorldTier {
    @Inject(
            method = "isUnlocked",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void checkCustomRequirements(
            Player player,
            WorldTier tier,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) {
            return;
        }

        int hostilityLevel = DifficultyLevel.ofAny(player);

        boolean valid = switch (tier) {
            case HAVEN -> true;
            case FRONTIER ->
                    hostilityLevel >= 60 && player.experienceLevel >= 60;
            case ASCENT ->
                    hostilityLevel >= 120 && player.experienceLevel >= 120;
            case SUMMIT ->
                    hostilityLevel >= 180 && player.experienceLevel >= 180;
            case PINNACLE ->
                    hostilityLevel >= 300 && player.experienceLevel >= 300;
        };
        cir.setReturnValue(valid);
    }
}