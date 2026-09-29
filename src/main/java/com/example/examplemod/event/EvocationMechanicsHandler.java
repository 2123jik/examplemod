package com.example.examplemod.event;

import com.example.examplemod.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.spells.scapegoat.ScapegoatEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public class EvocationMechanicsHandler {

    /**
     * 1. 替罪羊【命元代偿】致死保护与伤害转移
     */
    @SubscribeEvent
    public static void onPlayerDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var surrogateAttr = player.getAttribute(ModAttributes.SCAPEGOAT_SOUL_SURROGATE.getDelegate());
            if (surrogateAttr != null && surrogateAttr.getValue() >= 1.0) {
                float incomingDamage = event.getNewDamage();

                // 受到致命伤害（扣除生命与吸收血量后致死）
                if (incomingDamage >= player.getHealth() + player.getAbsorptionAmount()) {
                    var goats = player.level().getEntitiesOfClass(
                            ScapegoatEntity.class,
                            player.getBoundingBox().inflate(32.0),
                            g -> g.getOwner() == player && g.isAlive() && g.getPersistentData().getBoolean("SoulSurrogateActive")
                    );

                    if (!goats.isEmpty()) {
                        ScapegoatEntity targetGoat = goats.get(0);

                        // 将玩家本次承受伤害清零（NeoForge 1.21 官方推荐免伤方式）
                        event.setNewDamage(0.0F);
                        player.setHealth(Math.max(1.0F, player.getHealth()));

                        // 将致命伤害转移给替罪羊
                        DamageSources.applyDamage(targetGoat, incomingDamage, player.damageSources().magic());
                    }
                }
            }
        }
    }

    /**
     * 2. 替罪羊【命元代偿】法力反哺与唤魔冷却缩减
     */
    @SubscribeEvent
    public static void onScapegoatDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ScapegoatEntity goat && goat.getPersistentData().getBoolean("SoulSurrogateActive")) {
            if (goat.getOwner() instanceof ServerPlayer player && player.isAlive()) {
                float actualDamageTaken = event.getNewDamage();
                if (actualDamageTaken <= 0.0F) return;

                MagicData magicData = MagicData.getPlayerMagicData(player);

                // 结合铁魔法属性：玩家法术抗性越强，法力转化效率越高
                double spellResist = player.getAttributeValue(AttributeRegistry.SPELL_RESIST);
                float manaGain = (float) (actualDamageTaken * 0.35F * Math.max(0.5, spellResist));

                magicData.addMana(manaGain);

                // 唤魔冷却流转：扣减全法术冷却进度 20 刻 (1秒)
                magicData.getPlayerCooldowns().getSpellCooldowns().forEach((spellId, cd) -> {
                    magicData.getPlayerCooldowns().decrementCooldown(cd, 20);
                });
            }
        }
    }

    /**
     * 3. 护盾质变【纯粹穿透与过载反哺】结算
     * 此处严格匹配 SpellDamageSource 源码中的 spell() 方法
     */
    @SubscribeEvent
    public static void onSpellAttackPre(LivingDamageEvent.Pre event) {
        // 精确类型判定与解包
        if (event.getSource() instanceof SpellDamageSource spellSource && spellSource.getEntity() instanceof LivingEntity attacker) {
            if (attacker.getPersistentData().getInt("EvocationPenetrationCharges") > 0) {
                // 调用源码中的 spell() 方法获取 AbstractSpell
                if (spellSource.spell() != null && spellSource.spell().getSchoolType().equals(SchoolRegistry.EVOCATION.get())) {
                    // 消耗穿透充能
                    attacker.getPersistentData().putInt("EvocationPenetrationCharges", 0);

                    LivingEntity target = event.getEntity();
                    AttributeInstance resist = target.getAttribute(AttributeRegistry.SPELL_RESIST);
                    
                    // 1. 穿透结算：如果目标具有法术抗性减免，强行拉回 OriginalDamage 达成 100% 破抗
                    if (resist != null && resist.getValue() > 0) {
                        float cleanDamage = event.getOriginalDamage();
                        event.setNewDamage(cleanDamage);
                    }

                    // 2. 机制质变衍生：穿透状态下强制无视受击无敌帧，保证多段判定完全命中
                    spellSource.setIFrames(0);
                }
            }
        }
    }
}