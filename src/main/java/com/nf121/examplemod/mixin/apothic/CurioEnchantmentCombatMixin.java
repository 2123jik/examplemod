package com.nf121.examplemod.mixin.apothic;

import com.nf121.examplemod.compat.apothic.CurioEffectHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(value = EnchantmentHelper.class, remap = false)
public abstract class CurioEnchantmentCombatMixin {

    @Inject(
            method =
                    "getDamageProtection("
                            + "Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/entity/LivingEntity;"
                            + "Lnet/minecraft/world/damagesource/DamageSource;"
                            + ")F",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void examplemod$protection(
            ServerLevel level,
            LivingEntity wearer,
            DamageSource source,
            CallbackInfoReturnable<Float> cir
    ) {
        cir.setReturnValue(
                cir.getReturnValueF()
                        + CurioEffectHooks.protection(wearer, source)
        );
    }

    @Inject(
            method =
                    "modifyDamage("
                            + "Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/item/ItemStack;"
                            + "Lnet/minecraft/world/entity/Entity;"
                            + "Lnet/minecraft/world/damagesource/DamageSource;"
                            + "F)F",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void examplemod$damageBonus(
            ServerLevel level,
            ItemStack tool,
            Entity target,
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            cir.setReturnValue(
                    cir.getReturnValueF()
                            + CurioEffectHooks.damageBonus(attacker, target)
            );
        }
    }

    @Inject(
            method =
                    "doPostAttackEffectsWithItemSource("
                            + "Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/world/entity/Entity;"
                            + "Lnet/minecraft/world/damagesource/DamageSource;"
                            + "Lnet/minecraft/world/item/ItemStack;"
                            + ")V",
            at = @At("TAIL"),
            require = 1
    )
    private static void examplemod$postEffects(
            ServerLevel level,
            Entity target,
            DamageSource source,
            @Nullable ItemStack itemSource,
            CallbackInfo ci
    ) {
        CurioEffectHooks.postEffects(target, source);
    }
}
