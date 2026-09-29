package com.example.examplemod.mixin.ironsspellbooks.lightning;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.ZapParticleOption;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import io.redspace.ironsspellbooks.spells.lightning.ThunderStepSpell;
import io.redspace.ironsspellbooks.util.ParticleHelper;
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

@Mixin(value = ThunderStepSpell.class, remap = false)
public class ThunderStepSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void injectPlasmaShear(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var shearAttr = entity.getAttribute(ModAttributes.THUNDER_STEP_SHEAR.getDelegate());
        if (shearAttr != null && shearAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            ci.cancel(); // 阻断慢速投掷雷球，改为瞬发穿刺折跃

            float distance = 24.0f;
            Vec3 startPos = entity.position().add(0, entity.getEyeHeight() * 0.5, 0);
            Vec3 dest = TeleportSpell.findTeleportLocation(level, entity, distance);

            // 1. 执行瞬发位移
            Utils.handleSpellTeleport((ThunderStepSpell) (Object) this, entity, dest);
            entity.resetFallDistance();

            serverLevel.playSound(null, dest.x, dest.y, dest.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5f, 1.4f);

            // 2. 沿途等离子切割带
            Vec3 line = dest.subtract(startPos);
            double lineLength = line.length();
            Vec3 dir = line.normalize();
            float shearDamage = (float) entity.getAttributeValue(AttributeRegistry.LIGHTNING_SPELL_POWER) * 2.0f;

            for (double d = 0; d < lineLength; d += 0.5) {
                Vec3 p = startPos.add(dir.scale(d));
                serverLevel.sendParticles(new ZapParticleOption(p.add(0, 0.5, 0)), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleHelper.ELECTRICITY, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.05);
            }

            AABB pathBox = new AABB(startPos, dest).inflate(1.5);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, pathBox, e -> e != entity && !e.isAlliedTo(entity))) {
                var src = ((ThunderStepSpell) (Object) this).getDamageSource(entity);
                src.setIFrames(0);
                DamageSources.applyDamage(victim, shearDamage, src);

                // 强减速瘫痪
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, false));
            }
        }
    }
}