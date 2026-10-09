package com.example.examplemod.mixin.ironsspellbooks.eldritch;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.eldritch_blast.EldritchBlastVisualEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.spells.eldritch.EldritchBlastSpell;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EldritchBlastSpell.class, remap = false)
public class EldritchBlastSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void injectSingularity(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var singularityAttr = entity.getAttribute(ModAttributes.ELDRITCH_SINGULARITY.getDelegate());
        if (singularityAttr != null && singularityAttr.getValue() >= 1.0) {
            // 阻断原版 Recast 连发逻辑，直接单发质变为引力奇点
            ci.cancel();

            var hitResult = RaycastBuilder.begin(level, entity)
                    .range(EldritchBlastSpell.getRange(spellLevel, entity))
                    .checkForBlocks(true)
                    .bbInflation(0.2f)
                    .build();

            Vec3 singularityPos = hitResult.getLocation();
            level.addFreshEntity(new EldritchBlastVisualEntity(level, entity.getEyePosition().subtract(0, 0.75f, 0), singularityPos, entity));

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, singularityPos.x, singularityPos.y, singularityPos.z, SoundRegistry.ELECTROCUTE_LOOP.get(), SoundSource.PLAYERS, 2.5f, 0.5f);
                CameraShakeManager.addCameraShake(new CameraShakeData(serverLevel, 40, singularityPos, 12));

                float baseDamage = ((EldritchBlastSpell) (Object) this).getSpellPower(spellLevel, entity);
                double pullRadius = 8.5;

                // 调度 3 秒（60刻）的黑洞引力场与持续空间撕裂
                for (int tick = 0; tick < 60; tick += 2) {
                    final int currentTick = tick;
                    serverLevel.getServer().tell(new net.minecraft.server.TickTask(tick, () -> {
                        // 旋涡引力粒子
                        serverLevel.sendParticles(ParticleHelper.UNSTABLE_ENDER, singularityPos.x, singularityPos.y, singularityPos.z, 20, 0.4, 0.4, 0.4, 0.15);
                        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, singularityPos.x, singularityPos.y, singularityPos.z, 15, 2.0, 2.0, 2.0, -0.5);

                        AABB pullBox = new AABB(singularityPos, singularityPos).inflate(pullRadius);
                        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, pullBox)) {
                            if (target != entity && !target.isAlliedTo(entity)) {
                                // 计算中心强引力向量
                                Vec3 force = singularityPos.subtract(target.position()).normalize().scale(0.45);
                                target.setDeltaMovement(target.getDeltaMovement().add(force));
                                target.hurtMarked = true;

                                // 每 10 刻受到一次空间撕裂
                                if (currentTick % 10 == 0) {
                                    DamageSources.applyDamage(target, baseDamage * 0.25f, ((EldritchBlastSpell) (Object) this).getDamageSource(entity));
                                }
                            }
                        }

                        // 终焉爆发：奇点坍缩瞬间
                        if (currentTick == 58) {
                            serverLevel.playSound(null, singularityPos.x, singularityPos.y, singularityPos.z, SoundRegistry.ELDRITCH_BLAST.get(), SoundSource.PLAYERS, 3.0f, 0.6f);
                            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, singularityPos.x, singularityPos.y, singularityPos.z, 1, 0, 0, 0, 0);
                            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, pullBox.deflate(3.0))) {
                                if (victim != entity && !victim.isAlliedTo(entity)) {
                                    DamageSources.applyDamage(victim, baseDamage * 1.5f, ((EldritchBlastSpell) (Object) this).getDamageSource(entity));
                                }
                            }
                        }
                    }));
                }
            }
        }
    }
}