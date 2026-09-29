package com.example.examplemod.mixin.ironsspellbooks.ender;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import io.redspace.ironsspellbooks.spells.ender.MagicMissileSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = MagicMissileSpell.class, remap = false)
public class MagicMissileSpellMixin {

    @Inject(method = "onCast", at = @At("HEAD"), cancellable = true)
    private void injectSwarmMissiles(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        var swarmAttr = entity.getAttribute(ModAttributes.MAGIC_MISSILE_SWARM.getDelegate());
        if (swarmAttr != null && swarmAttr.getValue() >= 1.0 && world instanceof ServerLevel serverLevel) {
            ci.cancel(); // 阻断单发

            float damage = ((MagicMissileSpell) (Object) this).getSpellPower(spellLevel, entity) * 0.4f;
            int missileCount = 5;

            // 调度浮游蜂群连环点火索敌
            for (int i = 0; i < missileCount; i++) {
                final int index = i;
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(index * 4, () -> {
                    if (!entity.isAlive()) return;

                    // 检索周围 20 格内的最近敌对生物
                    AABB detectionBox = entity.getBoundingBox().inflate(20.0);
                    List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, detectionBox, e -> e != entity && !e.isAlliedTo(entity));

                    Vec3 spawnPos = entity.getEyePosition().add((Math.random() - 0.5) * 2.0, 0.8 + Math.random() * 0.8, (Math.random() - 0.5) * 2.0);
                    MagicMissileProjectile missile = new MagicMissileProjectile(world, entity);
                    missile.setPos(spawnPos);
                    missile.setDamage(damage);

                    Vec3 shootDir;
                    if (!enemies.isEmpty()) {
                        // 自动锁敌追踪
                        shootDir = enemies.get(0).getBoundingBox().getCenter().subtract(spawnPos).normalize();
                    } else {
                        // 随准星散射
                        shootDir = entity.getLookAngle();
                    }

                    missile.shoot(shootDir);
                    world.addFreshEntity(missile);
                    entity.playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.2f, 1.6f + (index * 0.1f));
                }));
            }
        }
    }
}