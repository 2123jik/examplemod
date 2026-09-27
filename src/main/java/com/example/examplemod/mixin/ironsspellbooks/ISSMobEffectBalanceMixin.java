package com.example.examplemod.mixin.ironsspellbooks;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(MobEffect.class)
public abstract class ISSMobEffectBalanceMixin {

    @Unique private static final ResourceLocation FORTIFY_ID = IronsSpellbooks.id("mobeffect_fortify");
    @Unique private static final ResourceLocation VIGOR_ID = IronsSpellbooks.id("mobeffect_vigor");

    @Inject(method = "createModifiers", at = @At("HEAD"), cancellable = true)
    private void irons_spellbooks$rebalanceModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo ci) {
        MobEffect self = (MobEffect) (Object) this;
        ResourceLocation effectKey = BuiltInRegistries.MOB_EFFECT.getKey(self);

        if (effectKey == null || !effectKey.getNamespace().equals("irons_spellbooks")) {
            return;
        }

        int level = amplifier + 1; 
        switch (effectKey.getPath()) {
            case "fortify" -> {
                consumer.accept(Attributes.MAX_ABSORPTION, new AttributeModifier(
                    FORTIFY_ID,
                    level * 20.0D, 
                    AttributeModifier.Operation.ADD_VALUE
                ));
                ci.cancel();
            }

            case "vigor" -> {
                consumer.accept(Attributes.MAX_HEALTH, new AttributeModifier(
                    VIGOR_ID,
                    level * 0.1D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
                ci.cancel();
            }


            default -> {}
        }
    }
}