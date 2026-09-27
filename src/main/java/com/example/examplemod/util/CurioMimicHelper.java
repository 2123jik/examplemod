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

    private static final TagKey<Item> CURIOS_TAG =
            TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("curios", "curio"));

    public static boolean isCurio(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.is(CURIOS_TAG)) return true;

        Holder<Item> holder = stack.getItem().builtInRegistryHolder();
        for (TagKey<Item> tag : holder.tags().toList()) {
            if ("curios".equals(tag.location().getNamespace())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasMimicCategory(ItemStack stack) {
        return stack.has(ModDataComponents.MIMIC_CATEGORY.get());
    }

    public static void assignRandomCategory(ItemStack stack) {
        List<LootCategory> pool = Apoth.BuiltInRegs.LOOT_CATEGORY.stream()
                .filter(cat -> {
                    ResourceLocation key = cat.getKey();
                    if (key == null) return false;
                    String path = key.getPath();
                    String ns = key.getNamespace();
                    return !"none".equals(path) && !"examplemod".equals(ns);
                })
                .toList();

        if (pool.isEmpty()) return;

        LootCategory chosen = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        stack.set(ModDataComponents.MIMIC_CATEGORY.get(), chosen.getKey());
    }
}