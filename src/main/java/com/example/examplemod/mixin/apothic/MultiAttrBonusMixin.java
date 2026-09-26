package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.bonus.MultiAttrBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 针对神化多属性复合宝石（MultiAttrBonus）内部类 ModifierInst
 * 将构建 AttributeModifier 时的数值缩减为 1/4 (0.25x)
 */
@Mixin(value = MultiAttrBonus.ModifierInst.class, remap = false)
public class MultiAttrBonusMixin {

    @ModifyArg(
        method = "build(Lnet/minecraft/resources/ResourceLocation;Ldev/shadowsoffire/apotheosis/socket/gem/Purity;)Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;<init>(Lnet/minecraft/resources/ResourceLocation;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)V"
        ),
        index = 1
    )
    private double scaleMultiAttributeValueToQuarter(double originalValue) {
        // 将复合宝石提供的每个属性数值都乘以 0.25
        return originalValue * 0.25D;
    }
}