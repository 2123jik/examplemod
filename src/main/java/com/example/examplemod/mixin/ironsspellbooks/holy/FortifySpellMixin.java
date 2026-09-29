package com.example.examplemod.mixin.ironsspellbooks.holy;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.holy.FortifySpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FortifySpell.class, remap = false)
public class FortifySpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectMartyrTag(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var martyrAttr = entity.getAttribute(ModAttributes.FORTIFY_MARTYR_AEGIS.getDelegate());
        if (martyrAttr != null && martyrAttr.getValue() >= 1.0) {
            float radius = FortifySpell.radius;
            // 标记范围内所有友军与自身的殉道链接
            level.getEntitiesOfClass(LivingEntity.class, new AABB(entity.position().subtract(radius, radius, radius), entity.position().add(radius, radius, radius)))
                    .forEach(target -> {
                        if (target != entity && (target.isAlliedTo(entity) || entity.getUUID().equals(target.getPersistentData().getUUID("Summoner")))) {
                            target.getPersistentData().putUUID("MartyrProtector", entity.getUUID());
                            target.getPersistentData().putInt("MartyrTimer", 20 * 120); // 持续 120 秒
                        }
                    });
        }
    }
}