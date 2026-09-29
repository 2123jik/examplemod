package com.example.examplemod.mixin.ironsspellbooks.ender;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.ender.BlackHoleSpell;
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

@Mixin(value = BlackHoleSpell.class, remap = false)
public class BlackHoleSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectSupernovaCountdown(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var novaAttr = entity.getAttribute(ModAttributes.BLACK_HOLE_SUPERNOVA.getDelegate());
        if (novaAttr != null && novaAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            Vec3 targetCenter = entity.getEyePosition().add(entity.getForward().scale(16.0));
            float explosionRadius = 14.0f;
            float novaDamage = ((BlackHoleSpell) (Object) this).getSpellPower(spellLevel, entity) * 3.5f;

            // 黑洞持续约 140 刻后引爆超新星终结大爆炸
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(140, () -> {
                serverLevel.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.5f);
                CameraShakeManager.addCameraShake(new CameraShakeData(serverLevel, 30, targetCenter, 20));

                // 冲击波粒子
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, targetCenter.x, targetCenter.y, targetCenter.z, 3, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleHelper.UNSTABLE_ENDER, targetCenter.x, targetCenter.y, targetCenter.z, 200, 3.0, 3.0, 3.0, 0.8);

                AABB blastBox = new AABB(targetCenter, targetCenter).inflate(explosionRadius);
                for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox)) {
                    if (victim != entity && !victim.isAlliedTo(entity)) {
                        DamageSources.applyDamage(victim, novaDamage, ((BlackHoleSpell) (Object) this).getDamageSource(entity));
                        // 强推斥冲击波向外震飞
                        Vec3 push = victim.position().subtract(targetCenter).normalize().scale(1.8).add(0, 0.6, 0);
                        victim.setDeltaMovement(push);
                        victim.hurtMarked = true;
                    }
                }
            }));
        }
    }
}