package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.bonus.AttributeBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 将神化所有单属性宝石（AttributeBonus）的数值强制降低至原版的 1/4 (0.25x)
 */
@Mixin(value = AttributeBonus.class, remap = false)
public class AttributeBonusMixin {

    @ModifyArg(
        method = "createModifier(Ldev/shadowsoffire/apotheosis/socket/gem/GemView;)Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;<init>(Lnet/minecraft/resources/ResourceLocation;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)V"
        ),
        index = 1
    )
    private double scaleGemAttributeValueToQuarter(double originalValue) {
        // 将宝石提供的属性数值直接缩减为 1/4
        return originalValue * 0.25D;
    }
}