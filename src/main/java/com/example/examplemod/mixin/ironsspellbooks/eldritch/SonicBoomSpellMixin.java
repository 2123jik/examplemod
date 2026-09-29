package com.example.examplemod.mixin.ironsspellbooks.eldritch;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.eldritch.SonicBoomSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SonicBoomSpell.class, remap = false)
public class SonicBoomSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectResonanceShatter(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        if (level.isClientSide) return;

        var shatterAttr = entity.getAttribute(ModAttributes.SONIC_RESONANCE_SHATTER.getDelegate());
        if (shatterAttr != null && shatterAttr.getValue() >= 1.0) {
            float range = SonicBoomSpell.getRange(spellLevel, entity);
            Vec3 start = entity.getEyePosition();
            Vec3 end = start.add(entity.getForward().scale(range));

            AABB searchBox = entity.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.5);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
                if (victim != entity && !victim.isAlliedTo(entity)) {
                    // 施加声学震爆神经瘫痪
                    victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 4, false, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));

                    // 次生音爆球环状裂解扩散
                    Vec3 shatterOrigin = victim.position();
                    ((ServerLevel) level).playSound(null, shatterOrigin.x, shatterOrigin.y, shatterOrigin.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 1.4f);

                    for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(4.5))) {
                        if (nearby != entity && nearby != victim && !nearby.isAlliedTo(entity)) {
                            // 裂解扩散真伤
                            DamageSources.applyDamage(nearby, ((SonicBoomSpell) (Object) this).getSpellPower(spellLevel, entity) * 0.45f, ((SonicBoomSpell) (Object) this).getDamageSource(entity));
                            Vec3 knockDir = nearby.position().subtract(shatterOrigin).normalize();
                            nearby.knockback(0.8, -knockDir.x, -knockDir.z);
                        }
                    }
                }
            }
        }
    }
}