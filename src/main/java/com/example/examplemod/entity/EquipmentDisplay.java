package com.example.examplemod.entity;

import com.example.examplemod.util.EquipmentIndex;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EquipmentDisplay {
    private EquipmentDisplay() {}

    private static final Map<String, String> MATERIAL_NAMES = Map.ofEntries(
            Map.entry("wood", "木"),
            Map.entry("stone", "石"),
            Map.entry("iron", "铁"),
            Map.entry("gold", "金"),
            Map.entry("diamond", "钻石"),
            Map.entry("netherite", "下界合金"),
            Map.entry("leather", "皮革"),
            Map.entry("chainmail", "锁链"),
            Map.entry("turtle", "海龟"),
            Map.entry("ironwood", "铁木"),
            Map.entry("steeleaf", "钢叶"),
            Map.entry("knightmetal", "骑士金属"),
            Map.entry("fiery", "炽铁")
    );

    public static String materialName(Set<String> names) {
        // 上一版索引同时存中文和 ID，这里优先选择中文。
        String chinese = names.stream()
                .filter(EquipmentDisplay::hasChinese)
                .sorted()
                .findFirst()
                .orElse(null);

        if (chinese != null) return chinese;

        for (String name : names.stream().sorted().toList()) {
            String path = name.substring(name.lastIndexOf(':') + 1);
            path = path.substring(path.lastIndexOf('/') + 1);
            String translated = MATERIAL_NAMES.get(path);
            if (translated != null) return translated;
        }

        return "其他材质";
    }

    public static String operationName(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_VALUE -> "固定加成";
            case ADD_MULTIPLIED_BASE -> "基础百分比加成";
            case ADD_MULTIPLIED_TOTAL -> "总量百分比加成";
        };
    }

    public static String attributeText(EquipmentIndex.AttributeValue value) {
        String amount = value.operation() == AttributeModifier.Operation.ADD_VALUE
                ? String.format(Locale.ROOT, "%+.2f", value.amount())
                : String.format(Locale.ROOT, "%+.1f%%", value.amount() * 100.0);

        // 未提供本地化的模组属性不直接展示 translation key。
        String name = friendly(value.name(), "未命名属性");

        return name + "  " + amount
                + "（" + operationName(value.operation()) + "）";
    }

    public static String friendly(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        if (name.startsWith("未解析")) return fallback;

        // 这些通常是未被翻译的键或原始资源 ID。
        if (name.contains(":")
                || name.startsWith("attribute.")
                || name.startsWith("loot_category.")
                || name.startsWith("rarity.")
                || name.startsWith("enchantment.")) {
            return fallback;
        }
        return name;
    }

    public static String details(EquipmentIndex.Row row) {
        StringBuilder text = new StringBuilder();

        text.append(row.name()).append('\n');
        text.append("材质：").append(materialName(row.materials())).append('\n');
        text.append("类型：")
                .append(friendly(row.categoryName(), "其他装备")).append('\n');
        text.append("品质：")
                .append(friendly(row.rarityName(), "未知品质")).append('\n');
        text.append("耐久：").append(row.durabilityText());

        if (row.hasDurability()) {
            text.append("（剩余 ").append(row.remaining())
                    .append(" / ").append(row.maximum()).append("）");
        }

        text.append("\n\n附魔\n");
        if (row.enchantments().isEmpty()) {
            text.append("没有附魔\n");
        } else {
            row.enchantments().stream()
                    .sorted(Comparator.comparing(EquipmentIndex.EnchantmentValue::name))
                    .forEach(enchantment -> text
                            .append(friendly(enchantment.name(), "未命名附魔"))
                            .append(" ").append(enchantment.level()).append(" 级\n"));
        }

        text.append("\n装备属性\n");
        if (row.attributes().isEmpty()) {
            text.append("没有属性加成\n");
        } else {
            for (var attribute : row.attributes()) {
                text.append(attributeText(attribute)).append('\n');
            }
            text.append("以上为装备修饰值，实际效果取决于生效槽位。\n");
        }

        text.append("\n神化词缀\n");
        if (row.affixes().isEmpty()) {
            text.append("没有词缀\n");
        } else {
            for (String affix : row.affixes()) {
                // 兼容上一版“名称 [ID] 强度 数字”的摘要。
                // 强度是神化内部参数，不应作为玩家属性百分比显示。
                int idStart = affix.indexOf(" [");
                String name = idStart >= 0 ? affix.substring(0, idStart) : affix;
                text.append(friendly(name, "未知词缀")).append('\n');
            }
        }

        return text.toString();
    }

    private static boolean hasChinese(String value) {
        return value.codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint)
                        == Character.UnicodeScript.HAN);
    }
}
