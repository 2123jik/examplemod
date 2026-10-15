package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.util.NativeSpellDamageContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageContextMixin {

    @WrapMethod(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
    )
    private boolean examplemod$scopeDamage(
            DamageSource source,
            float amount,
            Operation<Boolean> original
    ) {
        LivingEntity target = (LivingEntity) (Object) this;

        return NativeSpellDamageContext.withinHurt(
                target,
                source,
                () -> original.call(source, amount)
        );
    }
}
