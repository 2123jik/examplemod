package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.bonus.AttributeBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

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
        return originalValue * 0.25D;
    }
}