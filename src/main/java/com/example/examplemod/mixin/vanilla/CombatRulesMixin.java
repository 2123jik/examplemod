package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.combat.PureOffenseResolver;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = CombatRules.class, priority = 1200)
public class CombatRulesMixin {

    /**
     * @author YourName
     * @reason 彻底重构护甲对抗：纯白值属性对抗，暴击作为独立外层乘区，互不干涉
     */
    @Overwrite
    public static float getDamageAfterAbsorb(LivingEntity target, float damage, DamageSource damageSource, float armor, float toughness) {
        if (damage <= 0.0F) return 0.0F;

        // 1. 提取攻方【纯白值基础面板 P】（法术读 AbstractMagicProjectile.damage，箭矢读动能，近战读物攻白值）
        float pureAttack = PureOffenseResolver.resolvePureAttackPower(damageSource);

        // 2. 提取攻方的神化穿透与撕裂纯属性
        float pierce = 0.0F;
        float shred = 0.0F;
        if (damageSource.getEntity() instanceof LivingEntity attacker) {
            AttributeInstance pInst = attacker.getAttribute(ALObjects.Attributes.ARMOR_PIERCE);
            if (pInst != null) pierce = (float) pInst.getValue();

            AttributeInstance sInst = attacker.getAttribute(ALObjects.Attributes.ARMOR_SHRED);
            if (sInst != null) shred = (float) sInst.getValue();
        }

        // 3. 防方有效护甲结算
        float effectiveArmor = armor * (1.0F - shred) - pierce;

        // 4. 防方韧性刚度强化（每点韧性提升 5% 护甲效能）
        float fortifiedArmor = effectiveArmor * (1.0F + Math.max(0.0F, toughness) * 0.05F);

        // 5. 纯属性对抗阻抗 K：
        // 只有 pureAttack（纯白值面板）参与抗阻冲击，damage（包含暴击）被彻底排除在外！
        float baseK = 25.0F;
        float toughnessDamping = 3.0F + Math.max(0.0F, toughness) * 0.25F;
        float dynamicK = baseK + Math.max(0.0F, pureAttack - baseK) / toughnessDamping;

        // 6. 纯属性对抗计算出恒定的减免/易伤乘率
        float armorMultiplier;
        if (fortifiedArmor >= 0.0F) {
            // [正护甲减伤]
            armorMultiplier = dynamicK / (dynamicK + fortifiedArmor);
            // 设定最高 90% 减免保底
            armorMultiplier = Math.max(0.10F, armorMultiplier);
        } else {
            // [负护甲易伤]：平滑反函数收敛至 2.0 倍
            float negativeDepth = -fortifiedArmor;
            armorMultiplier = 2.0F - (dynamicK / (dynamicK + negativeDepth));
        }

        // 7. 兼容 1.21 原版武器附魔（如破甲附魔 Breaching）
        ItemStack weapon = damageSource.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            Level level = target.level();
            if (level instanceof ServerLevel serverLevel) {
                float reductionRatio = 1.0F - armorMultiplier;
                reductionRatio = Mth.clamp(
                    EnchantmentHelper.modifyArmorEffectiveness(serverLevel, weapon, target, damageSource, reductionRatio),
                    0.0F,
                    1.0F
                );
                armorMultiplier = 1.0F - reductionRatio;
            }
        }

        // 8. 最终承伤计算：
        // armorMultiplier 是纯属性对抗得出的固定系数（例如减免 40%，乘数为 0.6）。
        // damage 是当前伤害（若暴击了则是暴击后伤害，没暴击则是普通伤害）。
        // 两者相乘，既保证了暴击带来完整的收益，又保证了暴击绝不会扭曲破甲判定！
        return damage * Math.max(0.0F, armorMultiplier);
    }
}