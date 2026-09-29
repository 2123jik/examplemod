package com.example.examplemod.mixin.ironsspellbooks.evocation;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = SpellDamageSource.class, remap = false)
public class SpellDamageSourceMixin {

    /**
     * 拦截法术伤害在扣除抗性时的逻辑，若攻击者持有过载穿透标记，强制将目标的有效抗性视作 0
     */
    @ModifyVariable(method = "getEffectiveSpellResistance", at = @At("HEAD"), argsOnly = true)
    private static float bypassResistance(float originalResistance, LivingEntity victim, DamageSource damageSource) {
        Entity attacker = damageSource.getEntity();
        if (attacker instanceof LivingEntity livingAttacker && livingAttacker.getPersistentData().getInt("EvocationPenetrationCharges") > 0) {
            livingAttacker.getPersistentData().putInt("EvocationPenetrationCharges", 0); // 消耗掉该次穿透
            return 0.0f; // 彻底无视目标的法术抗性
        }
        return originalResistance;
    }
}