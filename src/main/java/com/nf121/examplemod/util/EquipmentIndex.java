// EquipmentIndex.java
package com.nf121.examplemod.util;

import com.nf121.examplemod.ExampleMod;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiers;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

/**
 * 在 Minecraft 客户端主线程提取数据，再把不可变 Row 交给 Modern UI 线程。
 * Row 不持有 ItemStack、动态注册表对象或可变 Component。
 *
 * 属性定义：
 * 展示装备的属性修饰符，不计算玩家穿戴后的最终属性。
 * 属性运算及生效槽组保留，不把 ADD_VALUE 和乘算混加。
 *
 * 属性来源：
 * ItemStack.getAttributeModifiers() 的结果转换为 Apothic Attributes 格式，
 * 再发布 StackAttributeModifiersEvent，让订阅者补充词缀、宝石等修饰符。
 * 不再手动调用 AffixInstance.addModifiers，避免重复添加。
 *
 * 如实际整合包使用了额外的“替代基础属性组件”，在 readAttributes 中接入
 * 该版本 Apothic Attributes 的官方查询入口即可，UI 与筛选不需要修改。
 */
public final class EquipmentIndex {
    private EquipmentIndex() {}

    public record EnchantmentValue(String id, String name, int level) {}

    public record AttributeValue(
            String id,
            String name,
            String modifierId,
            String slots,
            AttributeModifier.Operation operation,
            double amount
    ) {
        public String display() {
            String value = operation == AttributeModifier.Operation.ADD_VALUE
                    ? String.format(Locale.ROOT, "%+.3f", amount)
                    : String.format(Locale.ROOT, "%+.2f%%", amount * 100.0);

            return name + " " + value + " [" + operation.name() + "; " + slots + "]";
        }
    }

    public record Row(
            int menuSlot,
            String itemId,
            String name,
            Set<String> materials,
            int remaining,
            int maximum,
            boolean unbreakable,
            List<EnchantmentValue> enchantments,
            String rarityId,
            String rarityName,
            int rarityOrder,
            int rarityColor,
            String categoryId,
            String categoryName,
            List<AttributeValue> attributes,
            List<String> affixes
    ) {
        public Row {
            materials = Set.copyOf(materials);
            enchantments = List.copyOf(enchantments);
            attributes = List.copyOf(attributes);
            affixes = List.copyOf(affixes);
        }

        public boolean hasDurability() {
            return maximum > 0;
        }

        public double durabilityPercent() {
            return maximum > 0 ? remaining * 100.0 / maximum : Double.NaN;
        }

        public String durabilityText() {
            if (!hasDurability()) {
                return "无耐久";
            }
            return String.format(Locale.ROOT, "%.1f%%", durabilityPercent())
                    + (unbreakable ? " · 不可破坏" : "");
        }

        public String details() {
            StringBuilder out = new StringBuilder();
            out.append(name).append('\n')
                    .append(itemId).append('\n')
                    .append("材质：").append(String.join(", ", new TreeSet<>(materials))).append('\n')
                    .append("耐久：").append(durabilityText());

            if (hasDurability()) {
                out.append(" (").append(remaining).append('/').append(maximum).append(')');
            }

            out.append('\n').append("神化稀有度：").append(rarityName)
                    .append(" [").append(rarityId).append(']')
                    .append('\n').append("LootCategory：").append(categoryName)
                    .append(" [").append(categoryId).append(']')
                    .append("\n\n附魔：\n");

            if (enchantments.isEmpty()) out.append("无\n");
            for (EnchantmentValue enchantment : enchantments) {
                out.append(enchantment.name()).append(" ").append(enchantment.level())
                        .append(" [").append(enchantment.id()).append("]\n");
            }

            out.append("\n装备属性修饰符：\n");
            if (attributes.isEmpty()) out.append("无\n");
            for (AttributeValue attribute : attributes) {
                out.append(attribute.display()).append('\n')
                        .append("  ").append(attribute.id()).append('\n');
            }

            out.append("\n神化词缀：\n");
            if (affixes.isEmpty()) out.append("无\n");
            for (String affix : affixes) out.append(affix).append('\n');

            return out.toString();
        }
    }

    public static Row read(int menuSlot, ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        // isDamageableItem() 可能受不可破坏组件影响；这里保留原始耐久字段。
        int maximum = Math.max(0, stack.getMaxDamage());
        int remaining = Math.max(0, maximum - Math.max(0, stack.getDamageValue()));
        remaining = Math.min(maximum, remaining);

        List<EnchantmentValue> enchantments = new ArrayList<>();
        ItemEnchantments stored = stack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY
        );

        for (var entry : stored.entrySet()) {
            String id = entry.getKey().unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("unregistered");
            enchantments.add(new EnchantmentValue(
                    id,
                    entry.getKey().value().description().getString(),
                    entry.getIntValue()
            ));
        }
        enchantments.sort(Comparator.comparing(EnchantmentValue::id));

        String rarityId = "none";
        String rarityName = "无稀有度";
        int rarityOrder = -1;
        int rarityColor = 0xFFBBBBBB;

        var rarityHolder = AffixHelper.getRarity(stack);
        if (stack.has(Apoth.Components.RARITY)) {
            rarityId = rarityHolder.getId().toString();
            if (rarityHolder.isBound()) {
                var rarity = rarityHolder.get();
                rarityName = rarity.toComponent().getString();
                rarityOrder = rarity.sortIndex();
                rarityColor = 0xFF000000 | rarity.color().getValue();
            } else {
                rarityName = "未解析：" + rarityId;
            }
        }

        // 即便没有神化稀有度，也能有 LootCategory。
        LootCategory category = LootCategory.forItem(stack);

        List<String> affixes = new ArrayList<>();
        ItemAffixes itemAffixes = stack.getOrDefault(
                Apoth.Components.AFFIXES, ItemAffixes.EMPTY
        );
        for (var entry : itemAffixes.entrySet()) {
            var holder = entry.getKey();
            String name = holder.isBound()
                    ? holder.get().getName(true).getString()
                    : "未解析词缀";

            affixes.add(name + " [" + holder.getId() + "] "
                    + String.format(Locale.ROOT, "强度 %.3f", entry.getFloatValue()));
        }
        affixes.sort(String::compareTo);

        return new Row(
                menuSlot,
                itemId,
                stack.getHoverName().getString(),
                materials(stack),
                remaining,
                maximum,
                stack.has(DataComponents.UNBREAKABLE),
                enchantments,
                rarityId,
                rarityName,
                rarityOrder,
                rarityColor,
                category.getKey().toString(),
                Component.translatable(category.getDescId()).getString(),
                readAttributes(stack),
                affixes
        );
    }

    private static List<AttributeValue> readAttributes(ItemStack stack) {
        var event = new StackAttributeModifiersEvent(
                stack,
                StackAttributeModifiers.fromVanilla(stack.getAttributeModifiers())
        );
        NeoForge.EVENT_BUS.post(event);

        List<AttributeValue> attributes = new ArrayList<>();
        for (var entry : event.build().modifiers()) {
            String attributeId = entry.attribute().unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("unregistered");

            // 使用 Codec 保留槽组语义，避免依赖未提供的 getName/getId 方法。
            String slots = EntitySlotGroup.CODEC
                    .encodeStart(JsonOps.INSTANCE, entry.slots())
                    .result()
                    .map(Object::toString)
                    .orElse("unknown");

            attributes.add(new AttributeValue(
                    attributeId,
                    Component.translatable(
                            entry.attribute().value().getDescriptionId()
                    ).getString(),
                    entry.modifier().id().toString(),
                    slots,
                    entry.modifier().operation(),
                    entry.modifier().amount()
            ));
        }

        attributes.sort(Comparator.comparing(AttributeValue::id)
                .thenComparing(AttributeValue::slots)
                .thenComparing(AttributeValue::modifierId));

        return attributes;
    }

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

    /**
     * 材质规则：
     * 1. examplemod:equipment_material/<任意系列> 物品标签优先。
     * 2. 原版工具 Tier 和护甲材质注册键。
     * 3. 暮色已知装备系列。
     * 4. 未识别的保留“未分类”，不拿 LootRarity 当材质。
     *
     * 标签例：examplemod:equipment_material/twilightforest/ironwood
     * 用户可通过数据包补充，不必改 Java。
     */
    private static Set<String> materials(ItemStack stack) {
        TreeSet<String> result = new TreeSet<>();

        stack.getTags().forEach(tag -> {
            var id = tag.location();
            String prefix = "equipment_material/";
            if (id.getNamespace().equals(ExampleMod.MOD_ID)
                    && id.getPath().startsWith(prefix)) {
                String series = id.getPath().substring(prefix.length());
                result.add(series);
                String lastPart = series.substring(series.lastIndexOf('/') + 1);
                result.add(MATERIAL_NAMES.getOrDefault(lastPart, lastPart));
            }
        });

        if (!result.isEmpty()) return result;

        var item = stack.getItem();
        if (item instanceof TieredItem tiered && tiered.getTier() instanceof Tiers tier) {
            String id = tier.name().toLowerCase(Locale.ROOT);
            result.add(id);
            result.add(MATERIAL_NAMES.getOrDefault(id, id));
        }

        if (item instanceof ArmorItem armor) {
            armor.getMaterial().unwrapKey().ifPresent(key -> {
                var id = key.location();
                result.add(id.toString());
                result.add(MATERIAL_NAMES.getOrDefault(id.getPath(), id.getPath()));
            });
        }

        var id = BuiltInRegistries.ITEM.getKey(item);
        if (id.getNamespace().equals("twilightforest")) {
            for (String series : List.of("ironwood", "steeleaf", "knightmetal", "fiery")) {
                if (id.getPath().startsWith(series + "_")) {
                    result.clear();
                    result.add("twilightforest:" + series);
                    result.add(MATERIAL_NAMES.get(series));
                    break;
                }
            }
        }

        if (result.isEmpty()) result.add("未分类");
        return result;
    }
}
