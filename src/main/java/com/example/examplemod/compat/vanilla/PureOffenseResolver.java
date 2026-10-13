package com.example.examplemod.combat;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class PureOffenseResolver {

    /**
     * 获取攻方绝对纯净的基础攻击面板 P（绝不包含任何跳劈、神化暴击乘区）
     */
    public static float resolvePureAttackPower(DamageSource source) {
        Entity trueSource = source.getEntity();
        Entity directSource = source.getDirectEntity();

        // ---------------------------------------------------------------------
        // 1. 铁魔法流派
        // ---------------------------------------------------------------------
        // 分支 A：绝大多数铁魔法伤害实体都是 AbstractMagicProjectile
        if (directSource instanceof AbstractMagicProjectile magicProj) {
            // 直接读取施法时写入的纯法强威力！绝对干净，无暴击，脱离法杖物攻！
            return Math.max(1.0F, magicProj.getDamage());
        }

        // 分支 B：非弹射物法术（即时/范围法术）
        if (source instanceof SpellDamageSource spellSource) {
            AbstractSpell spell = spellSource.spell();
            if (spell != null) {
                LivingEntity caster = (trueSource instanceof LivingEntity le) ? le : null;
                // 读取纯法强面板基准
                return Math.max(1.0F, spell.getSpellPower(1, caster));
            }
        }

        // ---------------------------------------------------------------------
        // 2. 神化射手 / 远程弹射物
        // ---------------------------------------------------------------------
        if (directSource instanceof AbstractArrow arrow) {
            // 箭矢基础伤害（神化在生成时已乘入 ARROW_DAMAGE）
            double pureArrowPower = arrow.getBaseDamage();

            // 乘上飞行初速（排除暴击随机量，仅取物理动能模长）
            double velocity = arrow.getDeltaMovement().length();
            if (velocity > 0.1) {
                pureArrowPower *= velocity;
            }

            // 乘上神化的远程伤害乘区 PROJECTILE_DAMAGE
            if (trueSource instanceof LivingEntity shooter) {
                AttributeInstance projDmgInst = shooter.getAttribute(ALObjects.Attributes.PROJECTILE_DAMAGE);
                if (projDmgInst != null) {
                    pureArrowPower *= projDmgInst.getValue();
                }
            }

            return (float) Math.max(1.0, pureArrowPower);
        }

        // ---------------------------------------------------------------------
        // 3. 物理近战流派
        // ---------------------------------------------------------------------
        if (trueSource instanceof LivingEntity attacker) {
            if (attacker.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                // 直接读取 ATTACK_DAMAGE 白值（包含武器、锋利、词缀、恶意倍率，绝不含跳劈暴击！）
                double baseAttack = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
                return (float) Math.max(1.0, baseAttack);
            }
        }

        // 兜底（环境、摔落、窒息等无面板攻击）
        return 1.0F;
    }
}