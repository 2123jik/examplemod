package com.example.examplemod.mixin.ironsspellbooks.evocation;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.evocation.FangWardSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FangWardSpell.class, remap = false)
public class FangWardSpellMixin {

    private static final ResourceLocation SHRED_ID = ResourceLocation.fromNamespaceAndPath("examplemod", "fang_shred_debuff");

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectFangShred(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var shredAttr = entity.getAttribute(ModAttributes.FANG_WARD_SHRED.getDelegate());
        if (shredAttr != null && shredAttr.getValue() >= 1.0 && world instanceof ServerLevel serverLevel) {
            // 延时检测尖牙判定区内的目标（尖牙咬合生效时）
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(10, () -> {
                double radius = 1.5 * (2 + (spellLevel - 1) / 3.0) + 1.0;
                var targets = world.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), e -> e != entity && !e.isAlliedTo(entity));

                for (LivingEntity victim : targets) {
                    // 1. 削减 40% 法术抗性 (AttributeRegistry.SPELL_RESIST)
                    AttributeInstance resistInst = victim.getAttribute(AttributeRegistry.SPELL_RESIST);
                    if (resistInst != null) {
                        resistInst.removeModifier(SHRED_ID);
                        resistInst.addTransientModifier(new AttributeModifier(SHRED_ID, -0.40, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }

                    // 2. 施法打断与法力真实反噬判定
                    MagicData victimData = MagicData.getPlayerMagicData(victim);
                    if (victimData != null && victimData.isCasting()) {
                        // 强制打断施法
                        victimData.resetCastingState();
                        // 依照施法者唤魔强度给予真伤打击
                        float backlash = (float) entity.getAttributeValue(AttributeRegistry.EVOCATION_SPELL_POWER) * 1.5f;
                        DamageSources.applyDamage(victim, backlash, ((FangWardSpell) (Object) this).getDamageSource(entity));
                    }
                }
            }));
        }
    }
}