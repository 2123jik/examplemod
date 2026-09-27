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

    // 预存各效果对应的修饰符 ResourceLocation
    @Unique private static final ResourceLocation FORTIFY_ID = IronsSpellbooks.id("mobeffect_fortify");
    @Unique private static final ResourceLocation VIGOR_ID = IronsSpellbooks.id("mobeffect_vigor");

    @Inject(method = "createModifiers", at = @At("HEAD"), cancellable = true)
    private void irons_spellbooks$rebalanceModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo ci) {
        MobEffect self = (MobEffect) (Object) this;
        ResourceLocation effectKey = BuiltInRegistries.MOB_EFFECT.getKey(self);

        if (effectKey == null || !effectKey.getNamespace().equals("irons_spellbooks")) {
            return;
        }

        int level = amplifier + 1; // 药水等级（I 级为 1，II 级为 2 ...）

        switch (effectKey.getPath()) {
            case "fortify" -> {
                // 原版：每级 +1.0 点黄心 (ADD_VALUE)
                // 整合包改动：改为每级固定 +20.0 点（10颗心），或者按需求改成百分比
                consumer.accept(Attributes.MAX_ABSORPTION, new AttributeModifier(
                    FORTIFY_ID,
                    level * 20.0D, 
                    AttributeModifier.Operation.ADD_VALUE
                ));
                ci.cancel();
            }

            case "vigor" -> {
                // 原版：每级 +2.0 点生命 (ADD_VALUE)
                // 整合包改动：直接改成百分比生命上限提升（每级 +25% 基础生命）
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