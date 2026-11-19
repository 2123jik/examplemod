package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.event.damage.DamageTypeRemapper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageRemapMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private DamageSource examplemod$remapDamageSource(DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel) {
            return DamageTypeRemapper.remap(serverLevel, source);
        }
        return source;
    }
}