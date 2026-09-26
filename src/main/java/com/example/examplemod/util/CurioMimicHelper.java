package com.example.examplemod.util;

import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CurioMimicHelper {

    // Curios 官方通用主标签：#curios:curio
    private static final TagKey<Item> CURIOS_TAG =
            TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("curios", "curio"));

    /**
     * 判断物品是否为 Curios 饰品
     */
    public static boolean isCurio(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 1. 最标准快速判定：是否带有 #curios:curio 标签
        if (stack.is(CURIOS_TAG)) return true;

        // 2. 兜底判定：遍历物品标签，只要属于 "curios" 命名空间（比如 #curios:ring, #curios:charm）即算作饰品
        Holder<Item> holder = stack.getItem().builtInRegistryHolder();
        for (TagKey<Item> tag : holder.tags().toList()) {
            if ("curios".equals(tag.location().getNamespace())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否已经分配了伪装分类
     */
    public static boolean hasMimicCategory(ItemStack stack) {
        return stack.has(ModDataComponents.MIMIC_CATEGORY.get());
    }

    /**
     * 随机抽取一个神化原有分类并写入物品
     */
    public static void assignRandomCategory(ItemStack stack) {
        // 当场从神化已注册的分类池中筛选（过滤掉 none 和你自己模组的分类）
        List<LootCategory> pool = Apoth.BuiltInRegs.LOOT_CATEGORY.stream()
                .filter(cat -> {
                    ResourceLocation key = cat.getKey();
                    if (key == null) return false;
                    String path = key.getPath();
                    String ns = key.getNamespace();
                    // 排除无效分类、自身模组分类
                    return !"none".equals(path) && !"examplemod".equals(ns);
                })
                .toList();

        if (pool.isEmpty()) return;

        // 随机抽取一个
        LootCategory chosen = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        // 写入组件
        stack.set(ModDataComponents.MIMIC_CATEGORY.get(), chosen.getKey());
    }
}