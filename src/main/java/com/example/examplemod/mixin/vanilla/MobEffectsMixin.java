package com.example.examplemod.mixin.vanilla;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobEffects.class)
public class MobEffectsMixin {

    @Redirect(
            method = "<clinit>",             at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffect;addAttributeModifier(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceLocation;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Lnet/minecraft/world/effect/MobEffect;"
            )
    )
    private static MobEffect modifyAttributeModifiers(
            MobEffect instance,
            Holder<Attribute> attribute,
            ResourceLocation id,
            double amount,
            AttributeModifier.Operation operation
    ) {
        if (id.getPath().equals("effect.strength")) {
            
            
            double newAmount = 0.125;
            
            AttributeModifier.Operation newOperation = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL; 

            return instance.addAttributeModifier(attribute, id, newAmount, newOperation);
        }

        if (id.getPath().equals("effect.weakness")) {
            return instance.addAttributeModifier(attribute, id, -0.166, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        

        return instance.addAttributeModifier(attribute, id, amount, operation);
    }
}