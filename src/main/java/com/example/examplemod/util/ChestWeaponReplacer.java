package com.example.examplemod.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestWeaponReplacer {

    private static final Map<Item, List<Item>> TIER_POOLS = new HashMap<>();
    private static volatile boolean initialized = false;

    // ==========================================
    // 1. 各个材料阶级的安全物品 ID 清单（严格剔除 Boss 掉落与傀儡配件）
    // ==========================================
    private static final List<String> WOOD_TIER = List.of(
            "farmersdelight:flint_knife",
            "better_weaponry:wooden_battleaxe", "better_weaponry:wooden_broadsword", "better_weaponry:wooden_chakram",
            "better_weaponry:wooden_claymore", "better_weaponry:wooden_cutlass", "better_weaponry:wooden_dagger",
            "better_weaponry:wooden_glaive", "better_weaponry:wooden_hammer", "better_weaponry:wooden_katana",
            "better_weaponry:wooden_rapier", "better_weaponry:wooden_sai", "better_weaponry:wooden_scythe",
            "better_weaponry:wooden_sickle", "better_weaponry:wooden_spear"
    );

    private static final List<String> STONE_TIER = List.of(
            // Better Weaponry 石制与铜制
            "better_weaponry:stone_battleaxe", "better_weaponry:stone_broadsword", "better_weaponry:stone_chakram",
            "better_weaponry:stone_claymore", "better_weaponry:stone_cutlass", "better_weaponry:stone_dagger",
            "better_weaponry:stone_glaive", "better_weaponry:stone_hammer", "better_weaponry:stone_katana",
            "better_weaponry:stone_rapier", "better_weaponry:stone_sai", "better_weaponry:stone_scythe",
            "better_weaponry:stone_sickle", "better_weaponry:stone_spear",
            "better_weaponry:copper_sword", "better_weaponry:copper_axe", "better_weaponry:copper_battleaxe",
            "better_weaponry:copper_broadsword", "better_weaponry:copper_chakram", "better_weaponry:copper_claymore",
            "better_weaponry:copper_cutlass", "better_weaponry:copper_dagger", "better_weaponry:copper_glaive",
            "better_weaponry:copper_hammer", "better_weaponry:copper_katana", "better_weaponry:copper_rapier",
            "better_weaponry:copper_sai", "better_weaponry:copper_scythe", "better_weaponry:copper_sickle",
            "better_weaponry:copper_spear"
    );

    private static final List<String> IRON_TIER = List.of(
            "farmersdelight:iron_knife",
            "twilightdelight:ironwood_knife", "twilightdelight:wrought_iron_sword",
            "twilightforest:ironwood_axe", "twilightforest:ironwood_sword",
            // Better Weaponry (紫晶/铁)
            "better_weaponry:amethyst_sword", "better_weaponry:amethyst_axe", "better_weaponry:amethyst_claymore",
            "better_weaponry:amethyst_dagger", "better_weaponry:amethyst_katana", "better_weaponry:amethyst_spear",
            "better_weaponry:iron_battleaxe", "better_weaponry:iron_broadsword", "better_weaponry:iron_chakram",
            "better_weaponry:iron_claymore", "better_weaponry:iron_cutlass", "better_weaponry:iron_dagger",
            "better_weaponry:iron_glaive", "better_weaponry:iron_hammer", "better_weaponry:iron_katana",
            "better_weaponry:iron_rapier", "better_weaponry:iron_sai", "better_weaponry:iron_scythe",
            "better_weaponry:iron_sickle", "better_weaponry:iron_spear",
            // Simply Swords (铁)
            "simplyswords:iron_chakram", "simplyswords:iron_claymore", "simplyswords:iron_cutlass",
            "simplyswords:iron_glaive", "simplyswords:iron_greataxe", "simplyswords:iron_greathammer",
            "simplyswords:iron_halberd", "simplyswords:iron_katana", "simplyswords:iron_longsword",
            "simplyswords:iron_rapier", "simplyswords:iron_sai", "simplyswords:iron_scythe",
            "simplyswords:iron_spear", "simplyswords:iron_twinblade", "simplyswords:iron_warglaive",
            // L2 Weaponry (铁 & 铁木)
            "l2weaponry:iron_battle_axe", "l2weaponry:iron_claw", "l2weaponry:iron_dagger", "l2weaponry:iron_hammer",
            "l2weaponry:iron_javelin", "l2weaponry:iron_machete", "l2weaponry:iron_nunchaku", "l2weaponry:iron_scythe",
            "l2weaponry:iron_spear", "l2weaponry:iron_throwing_axe",
            "l2weaponry:ironwood_battle_axe", "l2weaponry:ironwood_claw", "l2weaponry:ironwood_dagger",
            "l2weaponry:ironwood_hammer", "l2weaponry:ironwood_javelin", "l2weaponry:ironwood_machete",
            "l2weaponry:ironwood_scythe", "l2weaponry:ironwood_spear",
            // Minecraft Dungeons
            "minecraftdungeonsv3:battlestaff", "minecraftdungeonsv3:claymore", "minecraftdungeonsv3:cutlass",
            "minecraftdungeonsv3:dagger", "minecraftdungeonsv3:double_axe", "minecraftdungeonsv3:fist",
            "minecraftdungeonsv3:great_hammer", "minecraftdungeonsv3:mace", "minecraftdungeonsv3:sickle",
            "minecraftdungeonsv3:reinforced_iron_sword", "minecraftdungeonsv3:reinforced_iron_axe"
                    ,"farmersdelight:skillet" // 农夫乐事平底锅（归入铁级）
    );



    private static final List<String> GOLD_TIER = List.of(
            "farmersdelight:golden_knife",
            "twilightforest:gold_minotaur_axe",
            // Better Weaponry
            "better_weaponry:golden_battleaxe", "better_weaponry:golden_broadsword", "better_weaponry:golden_chakram",
            "better_weaponry:golden_claymore", "better_weaponry:golden_cutlass", "better_weaponry:golden_dagger",
            "better_weaponry:golden_glaive", "better_weaponry:golden_hammer", "better_weaponry:golden_katana",
            "better_weaponry:golden_rapier", "better_weaponry:golden_sai", "better_weaponry:golden_scythe",
            "better_weaponry:golden_sickle", "better_weaponry:golden_spear",
            // Simply Swords
            "simplyswords:gold_chakram", "simplyswords:gold_claymore", "simplyswords:gold_cutlass",
            "simplyswords:gold_glaive", "simplyswords:gold_greataxe", "simplyswords:gold_greathammer",
            "simplyswords:gold_halberd", "simplyswords:gold_katana", "simplyswords:gold_longsword",
            "simplyswords:gold_rapier", "simplyswords:gold_sai", "simplyswords:gold_scythe",
            "simplyswords:gold_spear", "simplyswords:gold_twinblade", "simplyswords:gold_warglaive",
            // L2 Weaponry (金 & 图腾金)
            "l2weaponry:gold_battle_axe", "l2weaponry:gold_claw", "l2weaponry:gold_dagger", "l2weaponry:gold_hammer",
            "l2weaponry:gold_javelin", "l2weaponry:gold_machete", "l2weaponry:gold_nunchaku", "l2weaponry:gold_scythe",
            "l2weaponry:gold_spear", "l2weaponry:totemic_gold_battle_axe", "l2weaponry:totemic_gold_claw",
            "l2weaponry:totemic_gold_dagger", "l2weaponry:totemic_gold_hammer", "l2weaponry:totemic_gold_spear"
    );

    private static final List<String> DIAMOND_TIER = List.of(
            "farmersdelight:diamond_knife",
            "twilightdelight:knightmetal_knife", "twilightdelight:steeleaf_knife", "twilightdelight:teardrop_sword",
            "twilightforest:diamond_minotaur_axe", "twilightforest:knightmetal_axe", "twilightforest:knightmetal_sword",
            "twilightforest:steeleaf_axe", "twilightforest:steeleaf_sword", "twilightforest:ice_sword",
            // Better Weaponry (钻 & 绿宝石)
            "better_weaponry:diamond_battleaxe", "better_weaponry:diamond_broadsword", "better_weaponry:diamond_chakram",
            "better_weaponry:diamond_claymore", "better_weaponry:diamond_cutlass", "better_weaponry:diamond_dagger",
            "better_weaponry:diamond_glaive", "better_weaponry:diamond_hammer", "better_weaponry:diamond_katana",
            "better_weaponry:diamond_rapier", "better_weaponry:diamond_sai", "better_weaponry:diamond_scythe",
            "better_weaponry:diamond_sickle", "better_weaponry:diamond_spear",
            "better_weaponry:emerald_sword", "better_weaponry:emerald_axe", "better_weaponry:emerald_battleaxe",
            "better_weaponry:emerald_claymore", "better_weaponry:emerald_dagger", "better_weaponry:emerald_katana",
            // Simply Swords (钻 & 符文)
            "simplyswords:diamond_chakram", "simplyswords:diamond_claymore", "simplyswords:diamond_cutlass",
            "simplyswords:diamond_glaive", "simplyswords:diamond_greataxe", "simplyswords:diamond_greathammer",
            "simplyswords:diamond_halberd", "simplyswords:diamond_katana", "simplyswords:diamond_longsword",
            "simplyswords:diamond_rapier", "simplyswords:diamond_sai", "simplyswords:diamond_scythe",
            "simplyswords:diamond_spear", "simplyswords:diamond_twinblade", "simplyswords:diamond_warglaive",
            "simplyswords:runic_claymore", "simplyswords:runic_greataxe", "simplyswords:runic_katana",
            "simplyswords:runic_longsword", "simplyswords:runic_twinblade", "simplyswords:runic_spear",
            // L2 Weaponry (钻、骑士金属、钢叶)
            "l2weaponry:diamond_battle_axe", "l2weaponry:diamond_claw", "l2weaponry:diamond_dagger",
            "l2weaponry:diamond_hammer", "l2weaponry:diamond_javelin", "l2weaponry:diamond_machete",
            "l2weaponry:diamond_nunchaku", "l2weaponry:diamond_scythe", "l2weaponry:diamond_spear",
            "l2weaponry:knightmetal_battle_axe", "l2weaponry:knightmetal_claw", "l2weaponry:knightmetal_dagger",
            "l2weaponry:knightmetal_hammer", "l2weaponry:knightmetal_scythe", "l2weaponry:knightmetal_spear",
            "l2weaponry:steeleaf_battle_axe", "l2weaponry:steeleaf_claw", "l2weaponry:steeleaf_dagger",
            "l2weaponry:steeleaf_hammer", "l2weaponry:steeleaf_scythe", "l2weaponry:steeleaf_spear",
            // Minecraft Dungeons 进阶
            "minecraftdungeonsv3:reinforced_diamond_sword", "minecraftdungeonsv3:hawkbrand", "minecraftdungeonsv3:broad_sword_claymore"
    );

    private static final List<String> NETHERITE_TIER = List.of(
            "farmersdelight:netherite_knife",
            "twilightdelight:fiery_knife",
            "twilightforest:fiery_sword",
            // Better Weaponry
            "better_weaponry:netherite_battleaxe", "better_weaponry:netherite_broadsword", "better_weaponry:netherite_chakram",
            "better_weaponry:netherite_claymore", "better_weaponry:netherite_cutlass", "better_weaponry:netherite_dagger",
            "better_weaponry:netherite_glaive", "better_weaponry:netherite_hammer", "better_weaponry:netherite_katana",
            "better_weaponry:netherite_rapier", "better_weaponry:netherite_sai", "better_weaponry:netherite_scythe",
            "better_weaponry:netherite_sickle", "better_weaponry:netherite_spear",
            // Simply Swords
            "simplyswords:netherite_chakram", "simplyswords:netherite_claymore", "simplyswords:netherite_cutlass",
            "simplyswords:netherite_glaive", "simplyswords:netherite_greataxe", "simplyswords:netherite_greathammer",
            "simplyswords:netherite_halberd", "simplyswords:netherite_katana", "simplyswords:netherite_longsword",
            "simplyswords:netherite_rapier", "simplyswords:netherite_sai", "simplyswords:netherite_scythe",
            "simplyswords:netherite_spear", "simplyswords:netherite_twinblade", "simplyswords:netherite_warglaive",
            // L2 Weaponry (下界合金 & 炽焰)
            "l2weaponry:netherite_battle_axe", "l2weaponry:netherite_claw", "l2weaponry:netherite_dagger",
            "l2weaponry:netherite_hammer", "l2weaponry:netherite_javelin", "l2weaponry:netherite_machete",
            "l2weaponry:netherite_nunchaku", "l2weaponry:netherite_scythe", "l2weaponry:netherite_spear",
            "l2weaponry:fiery_battle_axe", "l2weaponry:fiery_claw", "l2weaponry:fiery_dagger",
            "l2weaponry:fiery_hammer", "l2weaponry:fiery_scythe", "l2weaponry:fiery_spear"
    );

    /**
     * 初始化对应池（安全读取当前注册表，自动过滤未加载的项）
     */
    private static synchronized void init() {
        if (initialized) return;

        List<Item> woodItems = parseIds(WOOD_TIER);
        List<Item> stoneItems = parseIds(STONE_TIER);
        List<Item> ironItems = parseIds(IRON_TIER);
        List<Item> goldItems = parseIds(GOLD_TIER);
        List<Item> diamondItems = parseIds(DIAMOND_TIER);
        List<Item> netheriteItems = parseIds(NETHERITE_TIER);

        // 绑定原版武器 -> 模组对应平级池
        bind(woodItems, Items.WOODEN_SWORD, Items.WOODEN_AXE);
        bind(stoneItems, Items.STONE_SWORD, Items.STONE_AXE);
        bind(ironItems, Items.IRON_SWORD, Items.IRON_AXE);
        bind(goldItems, Items.GOLDEN_SWORD, Items.GOLDEN_AXE);
        bind(diamondItems, Items.DIAMOND_SWORD, Items.DIAMOND_AXE);
        bind(netheriteItems, Items.NETHERITE_SWORD, Items.NETHERITE_AXE);

        initialized = true;
    }

    private static void bind(List<Item> pool, Item... originals) {
        if (pool.isEmpty()) return;
        for (Item item : originals) {
            TIER_POOLS.put(item, pool);
        }
    }

    private static List<Item> parseIds(List<String> ids) {
        List<Item> result = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                Item item = BuiltInRegistries.ITEM.get(loc);
                if (item != Items.AIR) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    /**
     * 核心替换入口：仅当是原版剑/斧时进行同级随机替换，并保留原有附魔与耐久损耗
     */
    public static ItemStack tryReplaceChestWeapon(ItemStack original, RandomSource random) {
        if (original.isEmpty()) return original;

        if (!initialized) {
            init();
        }

        List<Item> candidates = TIER_POOLS.get(original.getItem());
        if (candidates == null || candidates.isEmpty()) {
            return original; // 其它物品或模组原生物品不碰
        }

        Item targetItem = candidates.get(random.nextInt(candidates.size()));
        // transmuteCopy 会复制原 ItemStack 的所有组件（附魔、损伤度、自定义名称等），仅改变物品类型
        return original.transmuteCopy(targetItem, original.getCount());
    }
}