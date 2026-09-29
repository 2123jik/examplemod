package com.example.examplemod.mixin.ironsspellbooks.evocation;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.scapegoat.ScapegoatEntity;
import io.redspace.ironsspellbooks.spells.evocation.ScapegoatSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScapegoatSpell.class, remap = false)
public class ScapegoatSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void tagSoulSurrogate(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var surrogateAttr = entity.getAttribute(ModAttributes.SCAPEGOAT_SOUL_SURROGATE.getDelegate());
        if (surrogateAttr != null && surrogateAttr.getValue() >= 1.0) {
            // 获取刚刚生成的替罪羊实体，打上代偿标签
            world.getEntitiesOfClass(ScapegoatEntity.class, entity.getBoundingBox().inflate(24.0), goat -> goat.getOwner() == entity)
                    .forEach(goat -> goat.getPersistentData().putBoolean("SoulSurrogateActive", true));
        }
    }
}