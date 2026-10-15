// EquipmentFilter.java
package com.nf121.examplemod.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 所有字段之间 AND。
 * 材质、稀有度、分类字段内用逗号表示 OR。
 * 附魔、属性字段内用分号表示 AND。
 *
 * 附魔例：
 *   锋利>=4;耐久>=3
 *   minecraft:sharpness>=4;minecraft:unbreaking>=3
 *
 * 属性例：
 *   攻击伤害>=7
 *   minecraft:generic.attack_damage>=7
 *   攻击速度@ADD_VALUE>-3
 *   暴击@ADD_MULTIPLIED_BASE>=0.1
 *
 * 属性匹配逐条修饰符执行，不跨槽位、不跨运算类型求和。
 * 乘算值使用原始小数：0.1 表示 10%。
 */
public record EquipmentFilter(
        String search,
        String material,
        Durability durability,
        Enchanted enchanted,
        String rarity,
        String category,
        List<NumericRule> enchantmentRules,
        List<NumericRule> attributeRules
) implements Predicate<EquipmentIndex.Row> {

    public enum Durability {
        ANY("耐久：不限"),
        FULL("耐久：满"),
        GE_70("耐久：[70%, 100%)"),
        GE_40("耐久：[40%, 70%)"),
        GE_20("耐久：[20%, 40%)"),
        BELOW_20("耐久：<20%"),
        NONE("耐久：无");

        public final String label;

        Durability(String label) {
            this.label = label;
        }

        public boolean matches(EquipmentIndex.Row row) {
            if (this == ANY) return true;
            if (this == NONE) return !row.hasDurability();
            if (!row.hasDurability()) return false;

            // 整数交叉相乘，避免 70% 等边界的浮点误差。
            long remaining = row.remaining();
            long maximum = row.maximum();

            return switch (this) {
                case FULL -> remaining == maximum;
                case GE_70 -> remaining < maximum && remaining * 100 >= maximum * 70;
                case GE_40 -> remaining * 100 >= maximum * 40
                        && remaining * 100 < maximum * 70;
                case GE_20 -> remaining * 100 >= maximum * 20
                        && remaining * 100 < maximum * 40;
                case BELOW_20 -> remaining * 100 < maximum * 20;
                default -> true;
            };
        }
    }

    public enum Enchanted {
        ANY("附魔：不限"),
        YES("附魔：有"),
        NO("附魔：无");

        public final String label;

        Enchanted(String label) {
            this.label = label;
        }
    }

    public EquipmentFilter {
        enchantmentRules = List.copyOf(enchantmentRules);
        attributeRules = List.copyOf(attributeRules);
    }

    @Override
    public boolean test(EquipmentIndex.Row row) {
        if (!contains(row.name() + " " + row.itemId()
                + " " + String.join(" ", row.affixes()), search)) return false;

        if (!matchesAlternatives(String.join(" ", row.materials()), material)) return false;
        if (!durability.matches(row)) return false;

        if (enchanted == Enchanted.YES && row.enchantments().isEmpty()) return false;
        if (enchanted == Enchanted.NO && !row.enchantments().isEmpty()) return false;

        if (!matchesAlternatives(row.rarityId() + " " + row.rarityName(), rarity)) return false;
        if (!matchesAlternatives(row.categoryId() + " " + row.categoryName(), category)) return false;

        for (NumericRule rule : enchantmentRules) {
            boolean found = row.enchantments().stream().anyMatch(value ->
                    contains(value.id() + " " + value.name(), rule.name())
                            && rule.compare(value.level())
            );
            if (!found) return false;
        }

        for (NumericRule rule : attributeRules) {
            boolean found = row.attributes().stream().anyMatch(value ->
                    contains(value.id() + " " + value.name(), rule.name())
                            && (rule.operation().isEmpty()
                            || value.operation().name().equalsIgnoreCase(rule.operation()))
                            && rule.compare(value.amount())
            );
            if (!found) return false;
        }

        return true;
    }

    private static boolean contains(String text, String query) {
        return text.toLowerCase(Locale.ROOT)
                .contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean matchesAlternatives(String text, String query) {
        if (query.isBlank()) return true;
        return Arrays.stream(query.replace('，', ',').split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .anyMatch(part -> contains(text, part));
    }

    private static final Pattern RULE = Pattern.compile(
            "^(.+?)(?:\\s*(>=|<=|!=|=|>|<)\\s*"
                    + "([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)))?$"
    );

    public static List<NumericRule> parseRules(String text, boolean attributes) {
        if (text.isBlank()) return List.of();

        return Arrays.stream(text.replace('；', ';').split(";"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> parseRule(part, attributes))
                .toList();
    }

    private static NumericRule parseRule(String source, boolean attributes) {
        Matcher matcher = RULE.matcher(source);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("无法解析条件：" + source);
        }

        String name = matcher.group(1).trim();
        if (name.matches(".*[<>=!].*")) {
            throw new IllegalArgumentException("数值条件有误：" + source);
        }

        String operation = "";
        int at = name.indexOf('@');
        if (at >= 0) {
            if (!attributes) {
                throw new IllegalArgumentException("附魔条件不支持 @ 运算方式");
            }
            operation = name.substring(at + 1).trim().toUpperCase(Locale.ROOT);
            name = name.substring(0, at).trim();

            if (!List.of(
                    "ADD_VALUE", "ADD_MULTIPLIED_BASE", "ADD_MULTIPLIED_TOTAL"
            ).contains(operation)) {
                throw new IllegalArgumentException("未知属性运算：" + operation);
            }
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("条件缺少名称：" + source);
        }

        String comparator = matcher.group(2);
        double number = comparator == null ? 0 : Double.parseDouble(matcher.group(3));
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("数值超出范围：" + source);
        }

        return new NumericRule(name, operation, comparator == null ? "" : comparator, number);
    }

    public record NumericRule(
            String name, String operation, String comparator, double number
    ) {
        public boolean compare(double actual) {
            return switch (comparator) {
                case ">=" -> actual >= number;
                case "<=" -> actual <= number;
                case ">" -> actual > number;
                case "<" -> actual < number;
                case "=" -> Math.abs(actual - number) <= 1.0E-7;
                case "!=" -> Math.abs(actual - number) > 1.0E-7;
                default -> true;
            };
        }
    }
}
