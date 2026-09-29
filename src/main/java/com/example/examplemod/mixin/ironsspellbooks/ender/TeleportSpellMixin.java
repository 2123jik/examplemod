package com.example.examplemod.mixin.ironsspellbooks.ender;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.particles.TeleportParticlesPacket;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TeleportSpell.class, remap = false)
public class TeleportSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void injectVoidSwap(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var swapAttr = entity.getAttribute(ModAttributes.TELEPORT_VOID_SWAP.getDelegate());
        if (swapAttr != null && swapAttr.getValue() >= 1.0) {
            float range = 32f + spellLevel * 4f;
            // 射线检测准星瞄准的活体生物
            HitResult hit = RaycastBuilder.begin(level, entity)
                    .range(range)
                    .checkForBlocks(true)
                    .bbInflation(0.8f)
                    .build();

            if (hit.getType() == HitResult.Type.ENTITY && ((EntityHitResult) hit).getEntity() instanceof LivingEntity target && target != entity) {
                ci.cancel(); // 拦截原版单向闪烁

                Vec3 casterPos = entity.position();
                Vec3 targetPos = target.position();

                // 播放双向折跃粒子
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new TeleportParticlesPacket(casterPos, targetPos));
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new TeleportParticlesPacket(targetPos, casterPos));

                // 核心：强行对调实体空间坐标
                Utils.handleSpellTeleport((TeleportSpell) (Object) this, entity, targetPos);
                Utils.handleSpellTeleport((TeleportSpell) (Object) this, target, casterPos);

                entity.resetFallDistance();
                target.resetFallDistance();

                // 目标受到空间倒错负面：失明 + 深度缓慢
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 4, false, false));
                
                // 空间对调冲击伤害
                DamageSources.applyDamage(target, 6.0f + spellLevel * 2f, ((TeleportSpell) (Object) this).getDamageSource(entity));

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleHelper.UNSTABLE_ENDER, targetPos.x, targetPos.y + 1, targetPos.z, 40, 0.4, 0.4, 0.4, 0.1);
                    serverLevel.sendParticles(ParticleHelper.UNSTABLE_ENDER, casterPos.x, casterPos.y + 1, casterPos.z, 40, 0.4, 0.4, 0.4, 0.1);
                }

                entity.playSound(SoundEvents.ENDERMAN_TELEPORT, 2.0f, 1.2f);
                target.playSound(SoundEvents.ENDERMAN_TELEPORT, 2.0f, 0.8f);
            }
            // 若没有锁定目标，则放行原版 onCast，正常赶路位移
        }
    }
}