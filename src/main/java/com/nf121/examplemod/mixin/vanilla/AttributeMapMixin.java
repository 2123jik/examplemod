package com.nf121.examplemod.mixin.vanilla;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttributeMap.class)
public abstract class AttributeMapMixin {

    @Shadow
    public abstract boolean hasAttribute(Holder<Attribute> attribute);

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void examplemod$safeGetValue(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir) {
        if (attribute == null) {
            cir.setReturnValue(0.0);
            return;
        }

        // 如果生物没有这个属性，进行安全兜底
        if (!this.hasAttribute(attribute)) {
            // 攻击力特殊处理：没攻击力的生物就应该返回 0.0
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                cir.setReturnValue(0.0);
            } else {
                // 其他属性（如重力、体型、速度等），返回该属性本身注册时规定的默认值
                cir.setReturnValue(attribute.value().getDefaultValue());
            }
        }
    }

    @Inject(method = "getBaseValue", at = @At("HEAD"), cancellable = true)
    private void examplemod$safeGetBaseValue(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir) {
        if (attribute == null) {
            cir.setReturnValue(0.0);
            return;
        }

        if (!this.hasAttribute(attribute)) {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                cir.setReturnValue(0.0);
            } else {
                cir.setReturnValue(attribute.value().getDefaultValue());
            }
        }
    }
}