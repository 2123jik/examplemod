package com.example.examplemod.mixin.ironsspellbooks.nature;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.nature.OakskinSpell;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OakskinSpell.class, remap = false)
public class OakskinSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectAncientThornsBuff(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var thornsAttr = entity.getAttribute(ModAttributes.OAKSKIN_ANCIENT_THORNS.getDelegate());
        if (thornsAttr != null && thornsAttr.getValue() >= 1.0) {
            // 1. 彻底清除原本带来的恶心减速惩罚
            entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

            // 2. 植入远古荆棘核心标记与持续时间（与橡木持续时间对齐）
            int durationTicks = (int) (((OakskinSpell) (Object) this).getSpellPower(spellLevel, entity) * 20);
            entity.getPersistentData().putInt("AncientThornsTimer", durationTicks);
        }
    }
}