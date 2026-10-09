package com.example.examplemod.mixin.ironsspellbooks.fire;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import io.redspace.ironsspellbooks.spells.fire.FireballSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FireballSpell.class, remap = false)
public class FireballSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectSupercriticalIgnite(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var critAttr = entity.getAttribute(ModAttributes.FIREBALL_SUPERCRITICAL.getDelegate());
        if (critAttr != null && critAttr.getValue() >= 1.0 && world instanceof ServerLevel serverLevel) {
            // 获取刚刚发射的火球，注册命中引发的聚爆监听
            serverLevel.getEntitiesOfClass(MagicFireball.class, entity.getBoundingBox().inflate(4.0), fb -> fb.getOwner() == entity)
                    .forEach(fireball -> fireball.getPersistentData().putBoolean("SupercriticalActive", true));
        }
    }
}