package com.example.examplemod.mixin.villna;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    private static final float HEAL_PERCENTAGE_OF_MAX_HEALTH = 0.05F;

    @Redirect(
        method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;heal(F)V",
            ordinal = 0         )
    )
    private void redirectSaturationHeal(Player player, float originalAmount) {
        float scaledHealAmount = (player.getMaxHealth() * HEAL_PERCENTAGE_OF_MAX_HEALTH) * originalAmount;

        player.heal(scaledHealAmount);
    }

    @Redirect(
        method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;heal(F)V",
            ordinal = 1         )
    )
    private void redirectNaturalRegenHeal(Player player, float originalAmount) {
        float scaledHealAmount = player.getMaxHealth() * HEAL_PERCENTAGE_OF_MAX_HEALTH;

        player.heal(scaledHealAmount);
    }

    @Unique
    private int maxFoodLevel = 20;

    @ModifyConstant(method = "add", constant = @Constant(intValue = 20))
    private int modifyMaxFoodInAdd(int constant) {
        return this.maxFoodLevel;
    }

    @ModifyConstant(method = "needsFood", constant = @Constant(intValue = 20))
    private int modifyMaxFoodInNeedsFood(int constant) {
        return this.maxFoodLevel;
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
    private int modifyMaxFoodInTick(int constant) {
        return this.maxFoodLevel;
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 18))
    private int modifyRegenThreshold(int constant) {
        return this.maxFoodLevel-4;
    }
}