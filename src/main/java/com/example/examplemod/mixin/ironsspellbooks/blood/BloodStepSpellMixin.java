package com.example.examplemod.mixin.ironsspellbooks.blood;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.blood.BloodStepSpell;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BloodStepSpell.class, remap = false)
public class BloodStepSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"))
    private void injectSeveranceCut(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        if (level.isClientSide) return;

        // 检测实体是否拥有质变属性
        var severanceAttr = entity.getAttribute(ModAttributes.BLOOD_STEP_SEVERANCE.getDelegate());
        if (severanceAttr != null && severanceAttr.getValue() >= 1.0) {
            Vec3 startPos = entity.position().add(0, entity.getBbHeight() / 2, 0);

            // 在位移之后执行沿途斩击
            ((ServerLevel) level).getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                Vec3 endPos = entity.position().add(0, entity.getBbHeight() / 2, 0);
                Vec3 line = endPos.subtract(startPos);
                double distance = line.length();
                Vec3 dir = line.normalize();

                // 沿切线生成血刃粒子
                for (double d = 0; d < distance; d += 0.4) {
                    Vec3 point = startPos.add(dir.scale(d));
                    ((ServerLevel) level).sendParticles(ParticleHelper.BLOOD, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.02);
                    ((ServerLevel) level).sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }

                // 判定路径范围内的敌方实体
                AABB pathBox = new AABB(startPos, endPos).inflate(1.2);
                float severanceDamage = 12.0f + spellLevel * 3.5f;

                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, pathBox)) {
                    if (victim != entity && !victim.isAlliedTo(entity)) {
                        DamageSources.applyDamage(victim, severanceDamage, ((BloodStepSpell) (Object) this).getDamageSource(entity));
                        // 击退与空间硬直
                        victim.knockback(0.4, -dir.x, -dir.z);
                    }
                }
            }));
        }
    }
}