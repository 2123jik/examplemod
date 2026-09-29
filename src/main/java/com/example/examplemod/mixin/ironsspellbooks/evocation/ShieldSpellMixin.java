package com.example.examplemod.mixin.ironsspellbooks.evocation;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import io.redspace.ironsspellbooks.spells.evocation.ShieldSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShieldSpell.class, remap = false)
public class ShieldSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectOverloadShield(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var overloadAttr = entity.getAttribute(ModAttributes.SHIELD_DEFLECTION_OVERLOAD.getDelegate());
        if (overloadAttr != null && overloadAttr.getValue() >= 1.0) {
            // 扫描刚生成的盾牌实体打上属主和过载标记
            level.getEntitiesOfClass(ShieldEntity.class, entity.getBoundingBox().inflate(5.0)).forEach(shield -> {
                // 绑定属主，当盾牌被完全击破时，在盾牌死亡钩子处为施法者附加上限为 Evocation Spell Power * 15 的吸收生命值
                float evokPower = (float) entity.getAttributeValue(AttributeRegistry.EVOCATION_SPELL_POWER);
                entity.setAbsorptionAmount(entity.getAbsorptionAmount() + evokPower * 2.5f);
                
                // 注入临时抗性穿透状态标记
                entity.getPersistentData().putInt("EvocationPenetrationCharges", 1);
            });
        }
    }
}