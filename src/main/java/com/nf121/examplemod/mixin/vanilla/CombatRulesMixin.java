package com.nf121.examplemod.mixin.vanilla;

import com.nf121.examplemod.util.DamageEngine;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = CombatRules.class, priority = 1200)
public class CombatRulesMixin {

    /**
     * @author YourName
     * @reason 将护甲伤害计算统一交给 DamageEngine
     */
    @Overwrite
    public static float getDamageAfterAbsorb(
            LivingEntity target,
            float damage,
            DamageSource damageSource,
            float armor,
            float toughness
    ) {
        return DamageEngine.calculateArmorDamage(
                target,
                damage,
                damageSource,
                armor,
                toughness
        );
    }
}
