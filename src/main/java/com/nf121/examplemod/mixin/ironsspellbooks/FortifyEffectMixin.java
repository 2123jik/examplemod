package com.nf121.examplemod.mixin.ironsspellbooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.redspace.ironsspellbooks.effect.FortifyEffect")
public class FortifyEffectMixin {

    @Unique
    private static final ResourceLocation FORTIFY_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mobeffect_fortify");

    /**
     * 拦截原版的 setAbsorptionAmount 调用：
     * 1. 动态增加/修改 MAX_ABSORPTION 上限
     * 2. 设置对应的伤害吸收量
     */
    @WrapOperation(
            method = "onEffectStarted",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V"
            )
    )
    private void modifyAbsorptionAmount(LivingEntity entity, float originalCalculatedAmount, Operation<Void> original, LivingEntity pLivingEntity, int pAmplifier) {
        // 计算基于最大生命值的新增吸收量
        float addedAbsorption = (pAmplifier + 1) * entity.getMaxHealth() * 0.05f;

        // 1. 动态调整生物的 MAX_ABSORPTION 属性上限，防止被 setAbsorptionAmount 截断
        AttributeInstance maxAbsorptionAttr = entity.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorptionAttr != null) {
            // 移除原版 FortifyEffect 注册的固定上限修饰符
            maxAbsorptionAttr.removeModifier(FORTIFY_MODIFIER_ID);
            // 替换为按生命值百分比计算的动态上限修饰符
            maxAbsorptionAttr.addTransientModifier(new AttributeModifier(
                    FORTIFY_MODIFIER_ID,
                    addedAbsorption,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }

        // 2. 调用原始方法赋予吸收量（由于 MAX_ABSORPTION 已经被提升，此处不会再被原版 clamp 截断）
        original.call(entity, entity.getAbsorptionAmount() + addedAbsorption);
    }
}