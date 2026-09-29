package com.example.examplemod.mixin.ironsspellbooks.blood;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.spells.blood.AcupunctureSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AcupunctureSpell.class, remap = false)
public class AcupunctureSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"))
    private void injectCrucifixion(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        if (world.isClientSide) return;

        var crucAttr = entity.getAttribute(ModAttributes.ACUPUNCTURE_CRUCIFIXION.getDelegate());
        if (crucAttr != null && crucAttr.getValue() >= 1.0) {
            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
                var targetEntity = targetData.getTarget((ServerLevel) world);
                if (targetEntity != null) {
                    // 强制锁死：极高等级缓慢 + 漂浮脱离地面，无法逃逸
                    targetEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 6, false, false));
                    targetEntity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 35, 1, false, false));
                    // 施加虚弱，提升受到的血系针刺伤害
                    targetEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));
                }
            }
        }
    }
}