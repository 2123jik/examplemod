package com.example.examplemod.event;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public class IceMechanicsHandler {

    private static final ResourceLocation SHATTER_RESIST_DEBUFF = ResourceLocation.fromNamespaceAndPath("examplemod", "ice_shatter_shred");

    @SubscribeEvent
    public static void onIceDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource() instanceof SpellDamageSource spellSource && spellSource.getEntity() instanceof LivingEntity attacker) {
            // 判定是否为冰霜学派法术
            if (spellSource.spell() != null && spellSource.spell().getSchoolType().equals(SchoolRegistry.ICE.get())) {
                LivingEntity victim = event.getEntity();

                var shatterAttr = attacker.getAttribute(ModAttributes.ICE_SPIKES_SHATTER.getDelegate());
                if (shatterAttr != null && shatterAttr.getValue() >= 1.0) {
                    // 核心碎冰判定：目标处于受冻（Fully Frozen）或附带铁魔法 CHILLED / FROSTBITTEN 状态
                    boolean isFrozenOrChilled = victim.isFullyFrozen() || victim.hasEffect(MobEffectRegistry.CHILLED);

                    if (isFrozenOrChilled) {
                        // 1. 碎冰暴击：伤害直接翻倍 (100% 独立乘区提升)
                        float original = event.getNewDamage();
                        event.setNewDamage(original * 2.0f);

                        // 清零受击无敌帧，确保高频多段冰刺不被吞伤
                        spellSource.setIFrames(0);

                        // 2. 剥离目标 30% 全法术抗性 (Spell Resist)
                        AttributeInstance resist = victim.getAttribute(AttributeRegistry.SPELL_RESIST);
                        if (resist != null) {
                            resist.removeModifier(SHATTER_RESIST_DEBUFF);
                            resist.addTransientModifier(new AttributeModifier(SHATTER_RESIST_DEBUFF, -0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                        }

                        // 3. 碎冰视听反馈
                        if (victim.level() instanceof ServerLevel sl) {
                            sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0f, 1.2f);
                            sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, victim.getX(), victim.getY() + 1, victim.getZ(), 40, 0.4, 0.4, 0.4, 0.15);
                        }
                    }
                }
            }
        }
    }
}