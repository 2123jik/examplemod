package com.example.examplemod.event;

import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public class NatureMechanicsHandler {

    @SubscribeEvent
    public static void onNatureCombatPre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();

        // 1. 【橡木之躯：远古荆棘反震】：未折减原始伤害的 60% 真实自然震荡反弹
        if (victim.getPersistentData().getInt("AncientThornsTimer") > 0) {
            // 自身享受额外 35% 绝对伤害减免
            float rawDamage = event.getNewDamage();
            event.setNewDamage(rawDamage * 0.65f);

            if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != victim) {
                float reflectedDamage = rawDamage * 0.60f;
                DamageSources.applyDamage(attacker, reflectedDamage, victim.damageSources().thorns(victim));

                // 震退近身攻击者
                Vec3 push = attacker.position().subtract(victim.position()).normalize().scale(0.8).add(0, 0.2, 0);
                attacker.setDeltaMovement(attacker.getDeltaMovement().add(push));
                attacker.hurtMarked = true;
            }
        }

        // 2. 【强酸腐蚀死区】：额外追加基于已损失护甲值的真实腐蚀伤害
        if (victim.getPersistentData().getInt("AcidCorrosionZoneTimer") > 0) {
            double armor = victim.getAttributeValue(Attributes.ARMOR);
            // 护甲越低/被撕裂得越多，受到的腐蚀暴击越恐怖
            float bonusCorrosion = (float) Math.max(4.0, (30.0 - armor) * 0.5f);
            event.setNewDamage(event.getNewDamage() + bonusCorrosion);
        }

        // 3. 【缠绕术：断筋碎裂核爆】：受控目标遭到外部重击时，引爆荆棘造成残疾重创
        if (victim.getPersistentData().getBoolean("StrangleRuptureActive") && victim.getVehicle() != null) {
            victim.getPersistentData().remove("StrangleRuptureActive");
            victim.stopRiding(); // 摧毁缠绕体

            float ruptureDamage = event.getNewDamage() * 1.5f;
            event.setNewDamage(event.getNewDamage() + ruptureDamage);

            // 施加 5 秒深度重残废：移速降低 50%，虚弱 II
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, false));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));

            if (victim.level() instanceof ServerLevel sl) {
                sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 2.0f, 0.8f);
                sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1, victim.getZ(), 35, 0.4, 0.4, 0.4, 0.2);
            }
        }

        // 4. 【枯萎术：疫病生息传染】：带有 Blight 的受击目标向周围传染恶疾
        if (victim.hasEffect(MobEffectRegistry.BLIGHT)) {
            var blightInst = victim.getEffect(MobEffectRegistry.BLIGHT);
            if (blightInst != null && victim.level() instanceof ServerLevel sl) {
                AABB spreadBox = victim.getBoundingBox().inflate(8.0);
                for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, spreadBox)) {
                    if (nearby != victim && !nearby.hasEffect(MobEffectRegistry.BLIGHT) && event.getSource().getEntity() instanceof LivingEntity caster && nearby != caster && !nearby.isAlliedTo(caster)) {
                        nearby.addEffect(new MobEffectInstance(MobEffectRegistry.BLIGHT, blightInst.getDuration(), blightInst.getAmplifier()));
                        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, nearby.getX(), nearby.getY() + 1, nearby.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
        }
    }
}