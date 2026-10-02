package com.example.examplemod.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.*;

public class WeaponReplacer {

    private static final NavigableMap<Double, List<Item>> DAMAGE_POOLS = new TreeMap<>();
    private static volatile boolean initialized = false;

    /**
     * 自动遍历注册表，仅读取物品注册时的“硬编码默认伤害”
     */
    public static synchronized void initWeaponPools() {
        DAMAGE_POOLS.clear();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;

            // 关键点：直接从 Item 对象的默认组件池中获取属性修饰符！
            // 这代表了该 Item 在代码里 register 时的初始硬编码状态
            ItemAttributeModifiers defaultModifiers = item.components().getOrDefault(
                    DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.EMPTY
            );

            double baseDamage = getRawBaseDamage(defaultModifiers);

            // 如果该物品注册时就配置了主手攻击伤害，且伤害大于 0，归入原生武器池
            if (baseDamage > 0) {
                DAMAGE_POOLS.computeIfAbsent(baseDamage, k -> new ArrayList<>()).add(item);
            }
        }

        initialized = true;
    }

    /**
     * 从默认属性修饰符中提取原生的数值加成
     */
    private static double getRawBaseDamage(ItemAttributeModifiers modifiers) {
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE) &&
                    (entry.slot() == EquipmentSlotGroup.MAINHAND || entry.slot() == EquipmentSlotGroup.ANY)) {

                AttributeModifier modifier = entry.modifier();
                // 只看数值加成 (ADD_VALUE)
                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    return modifier.amount(); // 返回注册时直接赋予的 damage 数值
                }
            }
        }
        return 0.0;
    }

    /**
     * 替换逻辑：只匹配原生的注册数值
     */
    public static ItemStack tryReplace(ItemStack original, RandomSource random) {
        if (original.isEmpty()) return original;

        if (!initialized) {
            initWeaponPools();
        }

        // 读取原始物品原生的注册伤害
        ItemAttributeModifiers rawModifiers = original.getItem().components().getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY
        );

        double originalRawDamage = getRawBaseDamage(rawModifiers);
        if (originalRawDamage <= 0) {
            return original;
        }

        // 匹配同等原生伤害（或最接近档位）的武器池
        List<Item> candidates = getBestMatchingPool(originalRawDamage);
        if (candidates == null || candidates.isEmpty()) {
            return original;
        }

        Item replacementItem = candidates.get(random.nextInt(candidates.size()));
        if (replacementItem == null || replacementItem == Items.AIR) {
            return original;
        }

        return original.transmuteCopy(replacementItem, original.getCount());
    }

    private static List<Item> getBestMatchingPool(double targetDamage) {
        if (DAMAGE_POOLS.isEmpty()) return Collections.emptyList();

        if (DAMAGE_POOLS.containsKey(targetDamage)) {
            return DAMAGE_POOLS.get(targetDamage);
        }

        Map.Entry<Double, List<Item>> floor = DAMAGE_POOLS.floorEntry(targetDamage);
        Map.Entry<Double, List<Item>> ceiling = DAMAGE_POOLS.ceilingEntry(targetDamage);

        if (floor == null) return ceiling.getValue();
        if (ceiling == null) return floor.getValue();

        return (targetDamage - floor.getKey() <= ceiling.getKey() - targetDamage) ? floor.getValue() : ceiling.getValue();
    }
}