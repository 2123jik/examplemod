package com.example.examplemod.mixin.ironsspellbooks.nature;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.nature.RootSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RootSpell.class, remap = false)
public class RootSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectStrangleRupture(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var ruptureAttr = entity.getAttribute(ModAttributes.ROOT_STRANGLE_RUPTURE.getDelegate());
        if (ruptureAttr != null && ruptureAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
                LivingEntity target = castTargetingData.getTarget(serverLevel);
                if (target != null) {
                    target.getPersistentData().putBoolean("StrangleRuptureActive", true);
                    target.getPersistentData().putUUID("StrangleCaster", entity.getUUID());

                    // 持续周期绞杀（每秒造成基于自然法强的持续撕裂真伤）
                    int duration = ((RootSpell) (Object) this).getDuration(spellLevel, entity);
                    float naturePower = (float) entity.getAttributeValue(AttributeRegistry.NATURE_SPELL_POWER);

                    for (int t = 10; t < duration; t += 20) {
                        serverLevel.getServer().tell(new net.minecraft.server.TickTask(t, () -> {
                            if (target.isAlive() && target.getVehicle() != null) {
                                DamageSources.applyDamage(target, naturePower * 0.6f, entity.damageSources().magic());
                            }
                        }));
                    }
                }
            }
        }
    }
}