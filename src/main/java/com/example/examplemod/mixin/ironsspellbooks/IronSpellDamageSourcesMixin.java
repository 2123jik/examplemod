package com.example.examplemod.mixin.ironsspellbooks;

import com.example.examplemod.event.damage.NativeSpellDamageContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSources.class)
public abstract class IronSpellDamageSourcesMixin {

    @Inject(
            method = "applyDamage",
            at = @At("HEAD"),
            remap = false
    )
    private static void examplemod$captureInput(
            Entity target,
            float baseAmount,
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir,
            @Share("examplemod$attackPower") LocalFloatRef attackPower,
            @Share("examplemod$initialResistance") LocalFloatRef resistance
    ) {
        attackPower.set(baseAmount);
        resistance.set(1.0F);
    }

    @WrapOperation(
            method = "applyDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/damage/DamageSources;"
                            + "getResist("
                            + "Lnet/minecraft/world/entity/LivingEntity;"
                            + "Lio/redspace/ironsspellbooks/api/spells/SchoolType;"
                            + ")F",
                    remap = false
            ),
            remap = false
    )
    private static float examplemod$captureResistance(
            LivingEntity target,
            SchoolType school,
            Operation<Float> original,
            @Share("examplemod$initialResistance") LocalFloatRef resistance
    ) {
        float multiplier = original.call(target, school);
        resistance.set(multiplier);
        return multiplier;
    }

    @WrapOperation(
            method = "applyDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;"
                            + "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    remap = true
            ),
            remap = false
    )
    private static boolean examplemod$passContext(
            LivingEntity target,
            DamageSource source,
            float amount,
            Operation<Boolean> original,
            @Share("examplemod$attackPower") LocalFloatRef attackPower,
            @Share("examplemod$initialResistance") LocalFloatRef resistance
    ) {
        if (!(source instanceof SpellDamageSource spellSource)) {
            return original.call(target, source, amount);
        }

        return NativeSpellDamageContext.withNativeHit(
                target,
                spellSource,
                attackPower.get(),
                resistance.get(),
                () -> original.call(target, source, amount)
        );
    }
}
