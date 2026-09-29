package com.example.examplemod.mixin.ironsspellbooks.ice;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import io.redspace.ironsspellbooks.spells.ice.IceTombSpell;
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

@Mixin(value = IceTombSpell.class, remap = false)
public class IceTombSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectAbsoluteZeroExplosion(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var zeroAttr = entity.getAttribute(ModAttributes.ICE_TOMB_ABSOLUTE_ZERO.getDelegate());
        if (zeroAttr != null && zeroAttr.getValue() >= 1.0 && world instanceof ServerLevel serverLevel) {
            int duration = (int) ((IceTombSpell) (Object) this).getDuration(spellLevel, entity);

            // 持续时间内提升施法者生命与法力恢复，并在棺木结束时引发冰爆
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(duration - 2, () -> {
                if (!entity.isAlive()) return;

                Vec3 burstPos = entity.position();
                float blastRadius = 8.0f;
                float baseIcePower = (float) entity.getAttributeValue(AttributeRegistry.ICE_SPELL_POWER);
                float explosionDamage = 20.0f + baseIcePower * 2.0f;

                serverLevel.playSound(null, burstPos.x, burstPos.y, burstPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 1.4f);
                serverLevel.playSound(null, burstPos.x, burstPos.y, burstPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 4.0f, 0.5f);

                // 震波与碎冰暴风雨粒子
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, burstPos.x, burstPos.y + 1, burstPos.z, 2, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleHelper.SNOWFLAKE, burstPos.x, burstPos.y + 1, burstPos.z, 200, blastRadius * 0.5, 1.0, blastRadius * 0.5, 0.3);

                AABB burstBox = new AABB(burstPos, burstPos).inflate(blastRadius);
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, burstBox)) {
                    if (victim != entity && !victim.isAlliedTo(entity)) {
                        var src = ((IceTombSpell) (Object) this).getDamageSource(entity);
                        src.setFreezeTicks(180); // 9 秒深度受冻
                        src.setIFrames(0);
                        DamageSources.applyDamage(victim, explosionDamage, src);

                        // 强推斥冲击波震飞
                        Vec3 push = victim.position().subtract(burstPos).normalize().scale(1.6).add(0, 0.4, 0);
                        victim.setDeltaMovement(push);
                        victim.hurtMarked = true;
                    }
                }
            }));
        }
    }
}