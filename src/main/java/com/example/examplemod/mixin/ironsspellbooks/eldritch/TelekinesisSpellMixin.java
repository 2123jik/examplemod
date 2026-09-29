package com.example.examplemod.mixin.ironsspellbooks.eldritch;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.TelekinesisData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.eldritch.TelekinesisSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TelekinesisSpell.class, remap = false)
public class TelekinesisSpellMixin {

    @Inject(method = "handleTelekinesis", at = @At("HEAD"))
    private void injectKineticAnnihilation(ServerLevel world, LivingEntity entity, MagicData playerMagicData, float strength, CallbackInfo ci) {
        var kineticAttr = entity.getAttribute(ModAttributes.KINETIC_ANNIHILATION.getDelegate());
        if (kineticAttr != null && kineticAttr.getValue() >= 1.0) {
            if (playerMagicData.getAdditionalCastData() instanceof TelekinesisData targetData) {
                var targetEntity = targetData.getTarget(world);
                if (targetEntity != null) {
                    // 检测施法即将结束，或者玩家大幅甩动视角
                    if (playerMagicData.getCastDurationRemaining() <= 4) {
                        Vec3 launchDir = entity.getLookAngle().normalize().scale(3.2); // 恐怖的动能弹射初速度
                        targetEntity.setDeltaMovement(launchDir);
                        targetEntity.hurtMarked = true;

                        world.playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 2.5f, 0.7f);
                        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(), 2, 0, 0, 0, 0);

                        // 撞击检测：延迟两刻检查撞击猛烈爆破
                        world.getServer().tell(new net.minecraft.server.TickTask(4, () -> {
                            if (targetEntity.isAlive()) {
                                float impactDamage = (float) (25.0f + entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                                DamageSources.applyDamage(targetEntity, (float) impactDamage, ((TelekinesisSpell) (Object) this).getDamageSource(entity));
                            }
                        }));
                    }
                }
            }
        }
    }
}