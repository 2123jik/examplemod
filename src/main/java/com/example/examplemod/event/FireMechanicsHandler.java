package com.example.examplemod.event;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public class FireMechanicsHandler {

    @SubscribeEvent
    public static void onFireSpellDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource() instanceof SpellDamageSource spellSource && spellSource.getEntity() instanceof LivingEntity attacker) {
            // 仅对火系法术生效
            if (spellSource.spell() != null && spellSource.spell().getSchoolType().equals(SchoolRegistry.FIRE.get())) {
                LivingEntity target = event.getEntity();

                // 1. 【劫火回旋充能】结算：消耗印记，提供额外独立增伤与必定暴击破抗
                int charges = attacker.getPersistentData().getInt("IgnitionCharges");
                if (charges > 0) {
                    attacker.getPersistentData().putInt("IgnitionCharges", 0); // 消耗全部印记

                    // 每层增加 15% 伤害
                    float multiplier = 1.0f + (charges * 0.15f);
                    float enhancedDamage = event.getNewDamage() * multiplier;
                    event.setNewDamage(enhancedDamage);

                    // 破除无敌帧
                    spellSource.setIFrames(0);

                    // 25% 伤害转化为施法者法力与护盾
                    if (attacker.level() instanceof ServerLevel) {
                        MagicData magicData = MagicData.getPlayerMagicData(attacker);
                        if (magicData != null) {
                            magicData.addMana(enhancedDamage * 0.25f);
                        }
                        attacker.setAbsorptionAmount(attacker.getAbsorptionAmount() + enhancedDamage * 0.15f);
                    }
                }

                // 2. 【超临界聚爆】结算：检测目标当前燃烧刻数，并进行 300% 聚爆清算
                var critAttr = attacker.getAttribute(ModAttributes.FIREBALL_SUPERCRITICAL.getDelegate());
                if (critAttr != null && critAttr.getValue() >= 1.0 && target.getRemainingFireTicks() > 0) {
                    int remainingFire = target.getRemainingFireTicks();
                    target.clearFire(); // 瞬间吞噬燃烧状态

                    // 将剩余点燃时间折算为爆炸性真伤（每秒燃烧折合施法者火强 100%）
                    float fireSeconds = remainingFire / 20.0f;
                    float baseFirePower = (float) attacker.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER);
                    float detonationDamage = fireSeconds * baseFirePower * 0.5f;

                    event.setNewDamage(event.getNewDamage() + detonationDamage);

                    // 聚爆视觉与音效
                    if (target.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, 1.2f);
                    }
                }
            }
        }
    }
}