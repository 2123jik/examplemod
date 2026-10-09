package com.example.examplemod.mixin.ironsspellbooks.lightning;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.spells.lightning.LightningBoltSpell;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightningBoltSpell.class, remap = false)
public class LightningBoltSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectEMPFuse(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var empAttr = entity.getAttribute(ModAttributes.LIGHTNING_BOLT_EMP.getDelegate());
        if (empAttr != null && empAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            Vec3 strikePos = entity.position().add(entity.getLookAngle().scale(12.0));
            float empRadius = 8.0f;

            serverLevel.getServer().tell(new net.minecraft.server.TickTask(2, () -> {
                CameraShakeManager.addCameraShake(new CameraShakeData(serverLevel, 25, strikePos, 16));
                serverLevel.playSound(null, strikePos.x, strikePos.y, strikePos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 3.5f, 0.8f);

                // 环状 EMP 冲击波粒子
                serverLevel.sendParticles(ParticleHelper.ELECTRICITY, strikePos.x, strikePos.y + 1, strikePos.z, 120, empRadius * 0.5, 0.5, empRadius * 0.5, 0.2);
                serverLevel.sendParticles(ParticleTypes.FLASH, strikePos.x, strikePos.y + 1, strikePos.z, 2, 0, 0, 0, 0);

                AABB empArea = new AABB(strikePos, strikePos).inflate(empRadius);
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, empArea, e -> e != entity && !e.isAlliedTo(entity))) {
                    // 标记感电引信（持续 4 秒 = 80 刻）
                    victim.getPersistentData().putUUID("EMPFuseCaster", entity.getUUID());
                    victim.getPersistentData().putInt("EMPFuseTimer", 80);
                }
            }));
        }
    }
}