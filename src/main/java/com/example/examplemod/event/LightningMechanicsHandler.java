package com.example.examplemod.event;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.UUID;

@EventBusSubscriber
public class LightningMechanicsHandler {

    @SubscribeEvent
    public static void onLightningDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource() instanceof SpellDamageSource spellSource && spellSource.getEntity() instanceof LivingEntity attacker) {
            // 判定雷电学派法术
            if (spellSource.spell() != null && spellSource.spell().getSchoolType().equals(SchoolRegistry.LIGHTNING.get())) {
                LivingEntity victim = event.getEntity();

                // 【EMP 感电引信二次引爆】
                if (victim.getPersistentData().contains("EMPFuseCaster") && victim.getPersistentData().getInt("EMPFuseTimer") > 0) {
                    UUID casterUUID = victim.getPersistentData().getUUID("EMPFuseCaster");
                    if (casterUUID.equals(attacker.getUUID())) {
                        victim.getPersistentData().remove("EMPFuseCaster");
                        victim.getPersistentData().remove("EMPFuseTimer");

                        // 1. 触发 100% 绝对暴击（伤害提升 100%）并清零受击无敌帧
                        float original = event.getNewDamage();
                        event.setNewDamage(original * 2.0f);
                        spellSource.setIFrames(0);

                        // 2. 引爆二次雷鸣并返还施法者法力
                        if (attacker.level() instanceof ServerLevel sl) {
                            sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.5f, 1.4f);
                            sl.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1, victim.getZ(), 1, 0, 0, 0, 0);

                            MagicData magicData = MagicData.getPlayerMagicData(attacker);
                            if (magicData != null) {
                                magicData.addMana(original * 0.40f); // 返还等额高额法力
                            }
                        }
                    }
                }
            }
        }
    }
}