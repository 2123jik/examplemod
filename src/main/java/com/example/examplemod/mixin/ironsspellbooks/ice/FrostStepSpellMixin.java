package com.example.examplemod.mixin.ironsspellbooks.ice;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.ice.FrostStepSpell;
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

@Mixin(value = FrostStepSpell.class, remap = false)
public class FrostStepSpellMixin {

    @Inject(method = "onCast", at = @At("TAIL"))
    private void injectAvalancheArrival(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var avalancheAttr = entity.getAttribute(ModAttributes.FROST_STEP_AVALANCHE.getDelegate());
        if (avalancheAttr != null && avalancheAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            // 延迟 1 刻等传送位置完全同步
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                Vec3 arrivalPos = entity.position();
                float radius = 4.5f;
                float damage = (float) entity.getAttributeValue(AttributeRegistry.ICE_SPELL_POWER) * 1.2f;

                serverLevel.playSound(null, arrivalPos.x, arrivalPos.y, arrivalPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.5f, 0.7f);
                serverLevel.sendParticles(ParticleHelper.SNOWFLAKE, arrivalPos.x, arrivalPos.y + 0.5, arrivalPos.z, 80, radius * 0.6, 0.3, radius * 0.6, 0.1);
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, arrivalPos.x, arrivalPos.y + 0.2, arrivalPos.z, 60, radius * 0.5, 0.2, radius * 0.5, 0.2);

                AABB hitArea = new AABB(arrivalPos, arrivalPos).inflate(radius);
                var victims = serverLevel.getEntitiesOfClass(LivingEntity.class, hitArea, e -> e != entity && !e.isAlliedTo(entity));

                for (LivingEntity victim : victims) {
                    var dmgSrc = ((FrostStepSpell) (Object) this).getDamageSource(entity);
                    dmgSrc.setFreezeTicks(140); // 强行深度受冻
                    dmgSrc.setIFrames(0);
                    DamageSources.applyDamage(victim, damage, dmgSrc);
                }

                // 命中敌人后为自身构筑冰霜护盾（按命中敌人数量提供 Absorption）
                if (!victims.isEmpty()) {
                    float shieldAmount = victims.size() * (damage * 0.35f);
                    entity.setAbsorptionAmount(entity.getAbsorptionAmount() + Math.min(20.0f, shieldAmount));
                }
            }));
        }
    }
}