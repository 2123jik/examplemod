package com.example.examplemod.event;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.UUID;

@EventBusSubscriber
public class HolyMechanicsHandler {

    /**
     * 1. 【过量光佑】结算：监听铁魔法原生 SpellHealEvent，将溢出治疗 100% 转为 Absorption 护盾
     */
    @SubscribeEvent
    public static void onSpellHeal(SpellHealEvent event) {
        LivingEntity caster = event.getEntity();
        LivingEntity target = event.getTargetEntity();

        if (event.getSchoolType().equals(SchoolRegistry.HOLY.get())) {
            var overhealAttr = caster.getAttribute(ModAttributes.BLESSING_OVERHEAL_AEGIS.getDelegate());
            if (overhealAttr != null && overhealAttr.getValue() >= 1.0) {
                float currentHealth = target.getHealth();
                float maxHealth = target.getMaxHealth();
                float healAmount = event.getHealAmount();

                // 产生过量治疗
                if (currentHealth + healAmount > maxHealth) {
                    float overheal = (currentHealth + healAmount) - maxHealth;
                    float currentAbsorption = target.getAbsorptionAmount();

                    // 护盾上限不超过目标最大生命值的 50%
                    float maxAbsorptionCap = maxHealth * 0.5f;
                    float newAbsorption = Math.min(maxAbsorptionCap, currentAbsorption + overheal);

                    target.setAbsorptionAmount(newAbsorption);
                }
            }
        }
    }

    /**
     * 2. 【圣裁审判】以战养疗 & 【殉道光环】伤害分摊
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();

        // A. 【圣裁以战养疗】：受击目标若带有圣裁烙印，将伤害的 20% 辐射回血给施法者与友军
        if (victim.getPersistentData().contains("JudgedByCaster") && victim.getPersistentData().getInt("JudgedTimer") > 0) {
            UUID casterUUID = victim.getPersistentData().getUUID("JudgedByCaster");
            if (casterUUID != null && victim.level().getPlayerByUUID(casterUUID) instanceof ServerPlayer caster) {
                float healPulse = event.getNewDamage() * 0.20f;
                // 辐射 8 格内所有友方
                victim.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(8.0), ally -> ally == caster || ally.isAlliedTo(caster))
                        .forEach(ally -> ally.heal(healPulse));
            }
        }

        // B. 【殉道光环】：友军受击时分摊 40% 伤害给施法者
        if (victim.getPersistentData().contains("MartyrProtector") && victim.getPersistentData().getInt("MartyrTimer") > 0) {
            UUID protectorUUID = victim.getPersistentData().getUUID("MartyrProtector");
            if (protectorUUID != null && victim.level().getPlayerByUUID(protectorUUID) instanceof ServerPlayer protector) {
                if (protector.isAlive() && protector != victim) {
                    float rawDamage = event.getNewDamage();
                    float splitDamage = rawDamage * 0.40f;

                    // 友方本次只承受 60%
                    event.setNewDamage(rawDamage - splitDamage);

                    // 施法者代为承受 40%
                    DamageSources.applyDamage(protector, splitDamage, victim.damageSources().magic());

                    // 施法者反震冲击波并叠减蓝印记
                    MagicData magicData = MagicData.getPlayerMagicData(protector);
                    int discount = protector.getPersistentData().getInt("MartyrManaDiscount");
                    protector.getPersistentData().putInt("MartyrManaDiscount", Math.min(5, discount + 1));
                }
            }
        }
    }
}