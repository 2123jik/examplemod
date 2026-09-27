package com.example.examplemod.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class WeaponReplacer {

    public enum Tier {
        IRON, GOLD, DIAMOND, NETHERITE
    }

    private static final Map<Tier, List<Item>> TIER_POOLS = new EnumMap<>(Tier.class);
    private static final Map<Item, Tier> VANILLA_MAPPING = new HashMap<>();
    private static volatile boolean initialized = false;

    static {
        VANILLA_MAPPING.put(Items.IRON_SWORD, Tier.IRON);
        VANILLA_MAPPING.put(Items.IRON_AXE, Tier.IRON);
        VANILLA_MAPPING.put(Items.GOLDEN_SWORD, Tier.GOLD);
        VANILLA_MAPPING.put(Items.GOLDEN_AXE, Tier.GOLD);
        VANILLA_MAPPING.put(Items.DIAMOND_SWORD, Tier.DIAMOND);
        VANILLA_MAPPING.put(Items.DIAMOND_AXE, Tier.DIAMOND);
        VANILLA_MAPPING.put(Items.NETHERITE_SWORD, Tier.NETHERITE);
        VANILLA_MAPPING.put(Items.NETHERITE_AXE, Tier.NETHERITE);

        for (Tier tier : Tier.values()) {
            TIER_POOLS.put(tier, new ArrayList<>());
        }
    }

    public static synchronized void initWeaponPools() {
        for (Tier tier : Tier.values()) {
            TIER_POOLS.get(tier).clear();
        }

        registerPool(Tier.IRON, "l2weaponry", List.of("iron_dagger", "iron_battleaxe", "iron_hammer", "iron_machete", "iron_spear", "iron_claw", "iron_scythe"));
        registerPool(Tier.GOLD, "l2weaponry", List.of("golden_dagger", "golden_battleaxe", "golden_hammer", "golden_machete", "golden_spear", "golden_claw", "golden_scythe"));
        registerPool(Tier.DIAMOND, "l2weaponry", List.of("diamond_dagger", "diamond_battleaxe", "diamond_hammer", "diamond_machete", "diamond_spear", "diamond_claw", "diamond_scythe"));
        registerPool(Tier.NETHERITE, "l2weaponry", List.of("netherite_dagger", "netherite_battleaxe", "netherite_hammer", "netherite_machete", "netherite_spear", "netherite_claw", "netherite_scythe"));

        registerPool(Tier.IRON, "simplyswords", List.of("iron_longsword", "iron_twinblade", "iron_rapier", "iron_katana", "iron_halberd", "iron_glaive", "iron_warglaive", "iron_spear", "iron_claymore", "iron_greathammer", "iron_greataxe", "iron_scythe"));
        registerPool(Tier.GOLD, "simplyswords", List.of("gold_longsword", "gold_twinblade", "gold_rapier", "gold_katana", "gold_halberd", "gold_glaive", "gold_warglaive", "gold_spear", "gold_claymore", "gold_greathammer", "gold_greataxe", "gold_scythe"));
        registerPool(Tier.DIAMOND, "simplyswords", List.of("diamond_longsword", "diamond_twinblade", "diamond_rapier", "diamond_katana", "diamond_halberd", "diamond_glaive", "diamond_warglaive", "diamond_spear", "diamond_claymore", "diamond_greathammer", "diamond_greataxe", "diamond_scythe"));
        registerPool(Tier.NETHERITE, "simplyswords", List.of("netherite_longsword", "netherite_twinblade", "netherite_rapier", "netherite_katana", "netherite_halberd", "netherite_glaive", "netherite_warglaive", "netherite_spear", "netherite_claymore", "netherite_greathammer", "netherite_greataxe", "netherite_scythe"));

        initialized = true;
    }

    private static void registerPool(Tier tier, String modid, List<String> paths) {
        for (String path : paths) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(modid, path);

            BuiltInRegistries.ITEM.getOptional(rl).ifPresent(holder -> {
                Item item = holder.asItem();
                if (item != Items.AIR) {
                    TIER_POOLS.get(tier).add(item);
                }
            });
        }
    }

    public static ItemStack tryReplace(ItemStack original, RandomSource random) {
        if (original.isEmpty()) return original;

        if (!initialized) {
            initWeaponPools();
        }

        Tier tier = VANILLA_MAPPING.get(original.getItem());
        if (tier == null) return original; 
        List<Item> candidates = TIER_POOLS.get(tier);
        if (candidates == null || candidates.isEmpty()) return original;

        Item replacementItem = candidates.get(random.nextInt(candidates.size()));

        if (replacementItem == null || replacementItem == Items.AIR) {
            return original;
        }

        return original.transmuteCopy(replacementItem, original.getCount());
    }
}