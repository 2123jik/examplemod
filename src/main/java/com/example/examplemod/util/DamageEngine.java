package com.example.examplemod.util;

import com.example.examplemod.registry.ModAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DamageEngine {

    private static final float MIN_RESIST_MULTIPLIER = 0.05F;
    private static final float MAX_RESIST_MULTIPLIER = 2.0F;

    private static final float BASE_ARMOR_K = 25.0F;
    private static final float MIN_ARMOR_MULTIPLIER = 0.10F;

    private static final double CONVERSION_EPSILON = 0.0001;

    private DamageEngine() {}

    public record SolveResult(
            float totalDamage,
            Set<SchoolType> activeFinalSchools
    ) {}

    private record Rule(
            String from,
            String to,
            double rate
    ) {}

    private record DamageSlice(
            double baseAmount,
            String current,
            Set<String> ancestors
    ) {}

    /*
     * 对外入口：转化与法术伤害结算。
     *
     * baseDamage 是当前 LivingIncomingDamageEvent 收到的伤害，
     * 不保证是未经增伤的技能基础伤害。
     *
     * 对通过铁魔法 DamageSources.applyDamage 进入的原生法术，
     * 输入已乘过初始学派法抗：
     * 未转化切片直接保留；
     * 已转化切片除去初始法抗，再结算额外学派法强与终点法抗。
     */

    public static SolveResult solveAndCalculate(
            float baseDamage,
            String initialSchoolId,
            Entity owner,
            LivingEntity target,
            boolean isNativeSpell
    ) {
        LivingEntity caster =
                owner instanceof LivingEntity living ? living : null;

        List<String> nodeOrder = getNodeOrder();
        List<Rule> activeRules = collectActiveRules(
                caster,
                nodeOrder
        );


        if (activeRules.isEmpty()) {
            SchoolType school = getSchool(initialSchoolId);

            if (school == null) {
                return new SolveResult(baseDamage, Set.of());
            }

            if (isNativeSpell) {
                return new SolveResult(baseDamage, Set.of(school));
            }

            return new SolveResult(
                    calculateSpellDamage(baseDamage, owner, target, school),
                    Set.of(school)
            );
        }
        float nativeInitialResistance = 1.0F;

        if (isNativeSpell) {
            SchoolType initialSchool = getSchool(initialSchoolId);

            nativeInitialResistance = resolveNativeInitialResistance(
                    target,
                    initialSchool
            );

            /*
             * 非正数或非有限倍率无法在这里安全地通过除法还原。
             * 本次保留输入伤害，跳过转化。
             */
            if (!Float.isFinite(nativeInitialResistance)
                    || nativeInitialResistance <= 0.0F) {
                return new SolveResult(
                        baseDamage,
                        initialSchool == null
                                ? Set.of()
                                : Set.of(initialSchool)
                );
            }
        }

        Map<String, List<Rule>> groupedRules =
                groupRules(activeRules);

        List<DamageSlice> slices = convertDamage(
                baseDamage,
                initialSchoolId,
                nodeOrder,
                groupedRules
        );


        double generalSpellPower = getAttributeValue(
                caster,
                AttributeRegistry.SPELL_POWER,
                1.0
        );

        double generalSpellResist = getAttributeValue(
                target,
                AttributeRegistry.SPELL_RESIST,
                1.0
        );

        double totalDamage = 0.0;
        Set<SchoolType> finalSchools = new HashSet<>();

        for (DamageSlice slice : slices) {
            if (slice.baseAmount() <= CONVERSION_EPSILON) {
                continue;
            }

            SchoolType finalSchool = getSchool(slice.current());

            if (finalSchool != null) {
                finalSchools.add(finalSchool);
            }

            if (isNativeSpell) {
                totalDamage += calculateNativeSlice(
                        slice,
                        initialSchoolId,
                        caster,
                        target,
                        finalSchool,
                        generalSpellResist,
                        nativeInitialResistance
                );
            } else {
                totalDamage += calculateExternalSlice(
                        slice,
                        caster,
                        target,
                        finalSchool,
                        generalSpellPower,
                        generalSpellResist
                );
            }
        }

        return new SolveResult(
                (float) totalDamage,
                Set.copyOf(finalSchools)
        );
    }

    /*
     * 对外入口：无转化的外部魔法伤害。
     * 原 SpellDamageHelper.calculate。
     *
     * 调用方需要保证没有提前乘过本方法负责的法强和法抗。
     */
    public static float calculateSpellDamage(
            float baseDamage,
            Entity owner,
            LivingEntity target,
            SchoolType school
    ) {
        float damage = baseDamage;

        if (owner instanceof LivingEntity caster) {
            double generalPower = getAttributeValue(
                    caster,
                    AttributeRegistry.SPELL_POWER,
                    1.0
            );

            damage *= (float) (
                    generalPower * school.getPowerFor(caster)
            );
        }

        double generalResist = getAttributeValue(
                target,
                AttributeRegistry.SPELL_RESIST,
                1.0
        );

        return damage * getResistanceMultiplier(
                target,
                school,
                generalResist
        );
    }

    /*
     * 对外入口：原版护甲结算接入点。
     * 原 CombatRulesMixin 中的计算全部移动到这里。
     */
    public static float calculateArmorDamage(
            LivingEntity target,
            float damage,
            DamageSource source,
            float armor,
            float toughness
    ) {
        if (damage <= 0.0F) {
            return 0.0F;
        }

        float pureAttack = resolvePureAttackPower(target, source);

        LivingEntity attacker =
                source.getEntity() instanceof LivingEntity living
                        ? living
                        : null;

        float pierce = (float) getAttributeValue(
                attacker,
                ALObjects.Attributes.ARMOR_PIERCE,
                0.0
        );

        float shred = (float) getAttributeValue(
                attacker,
                ALObjects.Attributes.ARMOR_SHRED,
                0.0
        );

        float effectiveArmor = armor * (1.0F - shred) - pierce;
        float positiveToughness = Math.max(0.0F, toughness);
        float attackExcess = Math.max(0.0F, pureAttack - BASE_ARMOR_K);

        float armorMultiplier;

        if (effectiveArmor >= 0.0F) {
            // 正护甲：保留原来的韧性强化与攻击面板对抗。
            float fortifiedArmor =
                    effectiveArmor * (1.0F + positiveToughness * 0.05F);

            float toughnessDamping =
                    3.0F + positiveToughness * 0.25F;

            float dynamicK =
                    BASE_ARMOR_K + attackExcess / toughnessDamping;

            armorMultiplier = Math.max(
                    MIN_ARMOR_MULTIPLIER,
                    dynamicK / (dynamicK + fortifiedArmor)
            );
        } else {
            /*
             * 负护甲：韧性不再放大负护甲深度，也不再降低 K 加深易伤。
             *
             * 采用原公式 toughness = 0 时的结果：
             * 保留攻击面板的影响，并让韧性对负护甲保持中性。
             */
            float negativeDepth = -effectiveArmor;
            float negativeK = BASE_ARMOR_K + attackExcess / 3.0F;

            armorMultiplier =
                    2.0F - negativeK / (negativeK + negativeDepth);
        }

        /*
         * 附魔护甲效能只处理正减伤。
         * 无护甲减伤或处于负护甲易伤时，不将负减伤率夹成零。
         */
        ItemStack weapon = source.getWeaponItem();

        if (armorMultiplier < 1.0F
                && weapon != null
                && !weapon.isEmpty()
                && target.level() instanceof ServerLevel serverLevel) {

            float reductionRatio = 1.0F - armorMultiplier;

            reductionRatio = Mth.clamp(
                    EnchantmentHelper.modifyArmorEffectiveness(
                            serverLevel,
                            weapon,
                            target,
                            source,
                            reductionRatio
                    ),
                    0.0F,
                    1.0F
            );

            armorMultiplier = 1.0F - reductionRatio;
        }

        return damage * Math.max(0.0F, armorMultiplier);
    }
    /*
     * 兼容已有调用。
     * 护甲结算应优先使用包含 target 的重载。
     */
    public static float resolvePureAttackPower(DamageSource source) {
        return resolvePureAttackPower(null, source);
    }

    public static float resolvePureAttackPower(
            LivingEntity target,
            DamageSource source
    ) {
        NativeSpellDamageContext.Hit hit =
                NativeSpellDamageContext.current();

        if (hit != null
                && hit.source() == source
                && (target == null || hit.target() == target)) {

            float attackPower = hit.attackPower();

            if (Float.isFinite(attackPower)) {
                return Math.max(1.0F, attackPower);
            }

            return 1.0F;
        }

        Entity owner = source.getEntity();
        Entity direct = source.getDirectEntity();

        /*
         * 原生法术没有上下文：
         * 使用明确的兜底，不再猜一级法术伤害，
         * 也不退回施法者的近战攻击属性。
         */
        if (source instanceof SpellDamageSource) {
            return 1.0F;
        }

        /*
         * 暂时保留原来的箭矢估算。
         * 这仍然是命中时估算，不是发射时快照。
         */
        if (direct instanceof AbstractArrow arrow) {
            double arrowPower = arrow.getBaseDamage();
            double velocity = arrow.getDeltaMovement().length();

            if (velocity > 0.1) {
                arrowPower *= velocity;
            }

            if (owner instanceof LivingEntity shooter) {
                arrowPower *= getAttributeValue(
                        shooter,
                        ALObjects.Attributes.PROJECTILE_DAMAGE,
                        1.0
                );
            }

            return (float) Math.max(1.0, arrowPower);
        }

        /*
         * 只对直接来源就是攻击者的伤害使用近战属性面板。
         * 避免未知投射物使用射手命中时的近战攻击力。
         */
        if (owner instanceof LivingEntity attacker && direct == owner) {
            return (float) Math.max(
                    1.0,
                    getAttributeValue(
                            attacker,
                            Attributes.ATTACK_DAMAGE,
                            1.0
                    )
            );
        }

        return 1.0F;
    }
    private static float resolveNativeInitialResistance(
            LivingEntity target,
            SchoolType initialSchool
    ) {
        NativeSpellDamageContext.Hit hit =
                NativeSpellDamageContext.current();

        if (hit != null
                && hit.target() == target
                && hit.source().spell().getSchoolType() == initialSchool) {
            return hit.initialResistance();
        }

        /*
         * 无上下文时的兼容路径：
         * 使用铁魔法公式，不使用自定义的倍率下限。
         */
        return initialSchool == null
                ? 1.0F
                : DamageSources.getResist(target, initialSchool);
    }

    /*
     * 原生法术切片。
     *
     * 输入包含初始法抗以及截至桥接位置已生效的伤害修改。
     * 发生转化时，除去初始法抗，保留其他乘算效果，
     * 再按现有规则结算额外法强与终点法抗。
     */
    private static double calculateNativeSlice(
            DamageSlice slice,
            String initialSchoolId,
            LivingEntity caster,
            LivingEntity target,
            SchoolType finalSchool,
            double generalSpellResist,
            float nativeInitialResistance
    ) {
        boolean unchanged =
                slice.current().equals(initialSchoolId)
                        && slice.ancestors().size() == 1;

        if (unchanged) {
            return slice.baseAmount();
        }

        double unresisted =
                slice.baseAmount() / nativeInitialResistance;

        double extraPowerBonus = getSchoolPowerBonus(
                caster,
                slice.ancestors(),
                initialSchoolId
        );

        double powered = unresisted * (1.0 + extraPowerBonus);

        if (finalSchool == null) {
            return powered;
        }

        return powered * getResistanceMultiplier(
                target,
                finalSchool,
                generalSpellResist
        );
    }


    /*
     * 外部伤害切片。
     * 物理残留不乘法强；魔法切片结算血统法强与终点法抗。
     */
    private static double calculateExternalSlice(
            DamageSlice slice,
            LivingEntity caster,
            LivingEntity target,
            SchoolType finalSchool,
            double generalSpellPower,
            double generalSpellResist
    ) {
        if (finalSchool == null) {
            return slice.baseAmount();
        }

        double schoolPowerMultiplier =
                getExternalSchoolPowerMultiplier(
                        caster,
                        slice.ancestors()
                );

        double powered = slice.baseAmount()
                * generalSpellPower
                * schoolPowerMultiplier;

        return powered * getResistanceMultiplier(
                target,
                finalSchool,
                generalSpellResist
        );
    }
    /*
     * 外部伤害的血统法强：
     *
     * multiplier = 1 + Σ(学派法强 - 1)
     *
     * 同时累计增益与减益，最后限制总倍率不低于零。
     * 只有一个学派时，结果就是该学派自身的法强倍率。
     */
    private static double getExternalSchoolPowerMultiplier(
            LivingEntity caster,
            Set<String> ancestors
    ) {
        if (caster == null) {
            return 1.0;
        }

        double multiplier = 1.0;

        for (String schoolId : ancestors) {
            SchoolType school = getSchool(schoolId);

            if (school != null) {
                multiplier += school.getPowerFor(caster) - 1.0;
            }
        }

        return Math.max(0.0, multiplier);
    }


    /*
     * 血统法强增量。
     * 保留旧规则：只累计正增量，不累计低于 1 的学派减益。
     */
    private static double getSchoolPowerBonus(
            LivingEntity caster,
            Set<String> ancestors,
            String excludedSchoolId
    ) {
        if (caster == null) {
            return 0.0;
        }

        double bonus = 0.0;

        for (String schoolId : ancestors) {
            if (schoolId.equals(excludedSchoolId)) {
                continue;
            }

            SchoolType school = getSchool(schoolId);

            if (school != null) {
                bonus += Math.max(
                        0.0,
                        school.getPowerFor(caster) - 1.0
                );
            }
        }

        return bonus;
    }

    /*
     * 法抗公式的唯一实现。
     * 通用法抗 × 学派法抗，然后经过软上限及自定义倍率限制。
     */
    private static float getResistanceMultiplier(
            LivingEntity target,
            SchoolType school,
            double generalSpellResist
    ) {
        double combinedResist =
                generalSpellResist * school.getResistanceFor(target);

        float multiplier =
                2.0F - (float) softCapFormula(combinedResist);

        return Mth.clamp(
                multiplier,
                MIN_RESIST_MULTIPLIER,
                MAX_RESIST_MULTIPLIER
        );
    }

    // 与你提供的 Utils.softCapFormula 公式一致。
    private static double softCapFormula(double x) {
        return x <= 1.5
                ? x
                : -0.25 * (1.0 / (x - 1.0)) + 2.0;
    }

    private static List<String> getNodeOrder() {
        List<String> nodes = new ArrayList<>();
        nodes.add(ModAttributes.PHYSICAL_ID);

        for (var node : ModAttributes.TOPOLOGICAL_SCHOOLS) {
            nodes.add(node.id());
        }

        return nodes;
    }

    private static List<Rule> collectActiveRules(
            LivingEntity caster,
            List<String> nodes
    ) {
        List<Rule> rules = new ArrayList<>();

        if (caster == null) {
            return rules;
        }

        for (int i = 0; i < nodes.size(); i++) {
            String from = nodes.get(i);

            for (int j = i + 1; j < nodes.size(); j++) {
                String to = nodes.get(j);

                Holder<Attribute> attribute =
                        ModAttributes.getConversionAttribute(from, to);

                if (attribute == null) {
                    continue;
                }

                // 读取命中时实体最终属性，包含所有已生效来源的加成。
                double rate = getAttributeValue(caster, attribute, 0.0);

                if (rate > CONVERSION_EPSILON) {
                    rules.add(new Rule(from, to, rate));
                }
            }
        }

        return rules;
    }


    private static Map<String, List<Rule>> groupRules(
            List<Rule> rules
    ) {
        Map<String, List<Rule>> grouped = new HashMap<>();

        for (Rule rule : rules) {
            grouped.computeIfAbsent(
                    rule.from(),
                    ignored -> new ArrayList<>()
            ).add(rule);
        }

        return grouped;
    }


    private static List<DamageSlice> convertDamage(
            float baseDamage,
            String initialSchoolId,
            List<String> nodeOrder,
            Map<String, List<Rule>> rules
    ) {
        List<DamageSlice> slices = new ArrayList<>();

        slices.add(new DamageSlice(
                baseDamage,
                initialSchoolId,
                Set.of(initialSchoolId)
        ));

        for (String node : nodeOrder) {
            List<Rule> outgoing = rules.getOrDefault(node, List.of());

            if (outgoing.isEmpty()) {
                continue;
            }

            double totalRate = outgoing.stream()
                    .mapToDouble(Rule::rate)
                    .sum();

            double remainingRatio = Math.max(0.0, 1.0 - totalRate);
            List<DamageSlice> next = new ArrayList<>();

            for (DamageSlice slice : slices) {
                if (!slice.current().equals(node)) {
                    next.add(slice);
                    continue;
                }

                for (Rule rule : outgoing) {
                    Set<String> ancestors =
                            new HashSet<>(slice.ancestors());

                    ancestors.add(rule.to());

                    next.add(new DamageSlice(
                            slice.baseAmount() * rule.rate(),
                            rule.to(),
                            ancestors
                    ));
                }

                if (remainingRatio > CONVERSION_EPSILON) {
                    next.add(new DamageSlice(
                            slice.baseAmount() * remainingRatio,
                            node,
                            slice.ancestors()
                    ));
                }
            }

            slices = next;
        }

        return slices;
    }

    private static SchoolType getSchool(String schoolId) {
        var supplier = ModAttributes.ID_TO_SCHOOL.get(schoolId);
        return supplier == null ? null : supplier.get();
    }

    private static double getAttributeValue(
            LivingEntity entity,
            Holder<Attribute> attribute,
            double fallback
    ) {
        if (entity == null) {
            return fallback;
        }

        AttributeInstance instance = entity.getAttribute(attribute);

        return instance == null ? fallback : instance.getValue();
    }
}
