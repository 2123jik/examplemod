package com.example.examplemod.mixin.ironsspellbooks.ender;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.spells.ender.ShadowSlashSpell;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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

import java.util.List;

@Mixin(value = ShadowSlashSpell.class, remap = false)
public class ShadowSlashSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void injectOmnislash(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var omniAttr = entity.getAttribute(ModAttributes.SHADOW_SLASH_OMNISLASH.getDelegate());
        if (omniAttr != null && omniAttr.getValue() >= 1.0 && level instanceof ServerLevel serverLevel) {
            ci.cancel(); // 阻断原版单发位移斩

            Vec3 forward = entity.getForward();
            Vec3 searchCenter = entity.position().add(forward.scale(6.0));
            AABB targetBox = new AABB(searchCenter, searchCenter).inflate(8.0);
            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, targetBox, e -> e != entity && !e.isAlliedTo(entity));

            if (victims.isEmpty()) {
                // 没有目标时向前方空挥一道末影剑气
                entity.setDeltaMovement(forward.scale(1.2));
                return;
            }

            // 获得短暂虚空庇护（完全无敌避险）
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 4, false, false, false));

            float slashDamage = ((ShadowSlashSpell) (Object) this).getSpellPower(spellLevel, entity) * 0.7f;
            int totalSlashes = 5;

            // 调度极速多段折跃连击
            for (int i = 0; i < totalSlashes; i++) {
                final int strikeIndex = i;
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(strikeIndex * 3, () -> {
                    if (victims.isEmpty() || !entity.isAlive()) return;

                    // 循环在敌人之间随机折跃穿梭
                    LivingEntity target = victims.get(strikeIndex % victims.size());
                    if (target.isAlive()) {
                        Vec3 strikePos = target.position().add((Math.random() - 0.5) * 1.5, 0.2, (Math.random() - 0.5) * 1.5);
                        entity.teleportTo(strikePos.x, strikePos.y, strikePos.z);
                        entity.resetFallDistance();

                        // 造成空间斩击
                        DamageSources.applyDamage(target, slashDamage, ((ShadowSlashSpell) (Object) this).getDamageSource(entity));

                        // 视觉与音效
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                        serverLevel.sendParticles(ParticleHelper.ENDER_SPARKS, target.getX(), target.getY() + 1, target.getZ(), 15, 0.2, 0.2, 0.2, 0.1);
                        entity.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 1.2f + (float) Math.random() * 0.4f);
                    }
                }));
            }
        }
    }
}