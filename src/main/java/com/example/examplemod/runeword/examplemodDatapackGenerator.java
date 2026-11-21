package com.example.examplemod.runeword;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class examplemodDatapackGenerator {

    record AttributeMod(String attribute, double amount, String operation, String desc) {}

    record Runeword(
            String id,
            String nameEn,
            String nameCn,
            int allSpellLevels,
            List<String> validBaseTags,
            List<Integer> runeSequence,
            List<AttributeMod> attributes
    ) {}

    record GemStat(
            String slug, String nameEn, String nameCn,
            int tierGroup, // 1=Low(1~15), 2=Mid(16~25), 3=High(26~33), 0=D2 Gem
            String weaponAttr, double weaponVal, String weaponOp,
            String armorAttr,  double armorVal,  String armorOp,
            String shieldAttr, double shieldVal, String shieldOp
    ) {}

    record RuneInfo(int id, String name, String cnName, GemStat stat) {}

    private static final Map<Integer, RuneInfo> RUNES = new LinkedHashMap<>();
    private static final List<GemStat> D2_GEMS = new ArrayList<>();

    static {
        // ==================== 33 颗 PD2 符文（严格还原 武器 / 头胸 / 盾牌 三栏差异 + 自定义属性） ====================
        addRune(1,  "El",    "艾尔",   1, "minecraft:generic.attack_damage",        1.5,  "add_value",            "minecraft:generic.armor",                  1.5,  "add_value",            "l2weaponry:shield_defense",                2.0,  "add_value");
        addRune(2,  "Eld",   "艾德",   1, "examplemod:damage_to_undead",            75.0, "add_value",            "minecraft:generic.movement_speed",         0.04, "add_multiplied_base",  "l2weaponry:shield_defense",                3.5,  "add_value");
        addRune(3,  "Tir",   "特尔",   1, "examplemod:mana_on_kill",                2.0,  "add_value",            "examplemod:mana_on_kill",                  2.0,  "add_value",            "examplemod:mana_on_kill",                  2.0,  "add_value");
        addRune(4,  "Nef",   "那夫",   1, "minecraft:generic.attack_knockback",     0.6,  "add_value",            "minecraft:generic.knockback_resistance",   0.08, "add_value",            "minecraft:generic.armor",                  2.5,  "add_value");
        addRune(5,  "Eth",   "爱斯",   1, "apothic_attributes:armor_shred",         0.12, "add_value",            "irons_spellbooks:mana_regen",              0.15, "add_multiplied_base",  "irons_spellbooks:mana_regen",              0.15, "add_multiplied_base");
        addRune(6,  "Ith",   "伊司",   1, "minecraft:generic.attack_damage",        2.5,  "add_value",            "examplemod:damage_to_mana",                15.0, "add_value",            "examplemod:damage_to_mana",                15.0, "add_value");
        addRune(7,  "Tal",   "塔尔",   1, "irons_spellbooks:nature_spell_power",    0.15, "add_multiplied_base",  "irons_spellbooks:nature_magic_resist",     0.25, "add_multiplied_base",  "irons_spellbooks:nature_magic_resist",     0.30, "add_multiplied_base");
        addRune(8,  "Ral",   "拉尔",   1, "irons_spellbooks:fire_spell_power",      0.15, "add_multiplied_base",  "irons_spellbooks:fire_magic_resist",       0.25, "add_multiplied_base",  "irons_spellbooks:fire_magic_resist",       0.30, "add_multiplied_base");
        addRune(9,  "Ort",   "欧特",   1, "irons_spellbooks:lightning_spell_power", 0.15, "add_multiplied_base",  "irons_spellbooks:lightning_magic_resist",  0.25, "add_multiplied_base",  "irons_spellbooks:lightning_magic_resist",  0.30, "add_multiplied_base");
        addRune(10, "Thul",  "书尔",   1, "irons_spellbooks:ice_spell_power",       0.15, "add_multiplied_base",  "irons_spellbooks:ice_magic_resist",        0.25, "add_multiplied_base",  "irons_spellbooks:ice_magic_resist",        0.30, "add_multiplied_base");
        addRune(11, "Amn",   "安姆",   1, "apothic_attributes:life_steal",          0.05, "add_value",            "examplemod:thorns_damage",                 8.0,  "add_value",            "examplemod:thorns_damage",                 10.0, "add_value");
        addRune(12, "Sol",   "索尔",   1, "minecraft:generic.attack_damage",        3.0,  "add_value",            "l2damagetracker:damage_absorption",        3.0,  "add_value",            "l2damagetracker:damage_absorption",        4.0,  "add_value");
        addRune(13, "Shael", "夏勒",   1, "minecraft:generic.attack_speed",         0.15, "add_multiplied_base",  "apothic_attributes:dodge_chance",          0.05, "add_value",            "l2weaponry:reflect_time",                  2.0,  "add_value");
        addRune(14, "Dol",   "多尔",   1, "minecraft:generic.attack_damage",        0.12, "add_multiplied_base",  "l2damagetracker:regen",                    0.15, "add_multiplied_base",  "l2damagetracker:regen",                    0.22, "add_multiplied_base");

        addRune(15, "Hel",   "海尔",   2, "irons_spellbooks:cast_time_reduction",   0.10, "add_multiplied_base",  "minecraft:generic.movement_speed",         0.06, "add_multiplied_base",  "minecraft:generic.movement_speed",         0.06, "add_multiplied_base");
        addRune(16, "Io",    "艾欧",   2, "minecraft:generic.max_health",           3.0,  "add_value",            "minecraft:generic.max_health",             3.0,  "add_value",            "minecraft:generic.max_health",             3.0,  "add_value");
        addRune(17, "Lum",   "卢姆",   2, "irons_spellbooks:max_mana",              60.0, "add_value",            "irons_spellbooks:max_mana",                60.0, "add_value",            "irons_spellbooks:max_mana",                60.0, "add_value");
        addRune(18, "Ko",    "科",     2, "apothic_attributes:crit_chance",         0.06, "add_value",            "apothic_attributes:dodge_chance",          0.06, "add_value",            "apothic_attributes:dodge_chance",          0.06, "add_value");
        addRune(19, "Fal",   "法尔",   2, "minecraft:generic.attack_damage",        3.5,  "add_value",            "minecraft:generic.armor_toughness",        2.5,  "add_value",            "minecraft:generic.armor_toughness",        2.5,  "add_value");
        addRune(20, "Lem",   "蓝姆",   2, "apothic_attributes:experience_gained",   0.25, "add_multiplied_base",  "apothic_attributes:experience_gained",     0.18, "add_multiplied_base",  "apothic_attributes:experience_gained",     0.18, "add_multiplied_base");
        addRune(21, "Pul",   "普尔",   2, "examplemod:damage_to_demons",            75.0, "add_value",            "minecraft:generic.armor",                  4.0,  "add_value",            "minecraft:generic.armor",                  4.0,  "add_value");
        addRune(22, "Um",    "乌姆",   2, "examplemod:open_wounds",                 25.0, "add_value",            "irons_spellbooks:spell_resist",            0.15, "add_multiplied_base",  "irons_spellbooks:spell_resist",            0.22, "add_multiplied_base");
        addRune(23, "Mal",   "马尔",   2, "examplemod:prevent_heal",                2.5,  "add_value",            "l2damagetracker:damage_absorption",        4.0,  "add_value",            "l2damagetracker:damage_absorption",        5.0,  "add_value");
        addRune(24, "Ist",   "伊斯特", 2, "minecraft:generic.luck",                 1.5,  "add_value",            "minecraft:generic.luck",                   1.5,  "add_value",            "minecraft:generic.luck",                   1.5,  "add_value");
        addRune(25, "Gul",   "古尔",   2, "apothic_attributes:crit_damage",         0.25, "add_multiplied_base",  "irons_spellbooks:nature_magic_resist",     0.35, "add_multiplied_base",  "irons_spellbooks:nature_magic_resist",     0.35, "add_multiplied_base");

        addRune(26, "Vex",   "伐克斯", 3, "examplemod:mana_steal",                  7.0,  "add_value",            "irons_spellbooks:fire_magic_resist",       0.35, "add_multiplied_base",  "irons_spellbooks:fire_magic_resist",       0.35, "add_multiplied_base");
        addRune(27, "Ohm",   "欧姆",   3, "minecraft:generic.attack_damage",        0.22, "add_multiplied_base",  "irons_spellbooks:ice_magic_resist",        0.35, "add_multiplied_base",  "irons_spellbooks:ice_magic_resist",        0.35, "add_multiplied_base");
        addRune(28, "Lo",    "罗",     3, "apothic_attributes:crit_chance",         0.15, "add_value",            "irons_spellbooks:lightning_magic_resist",  0.35, "add_multiplied_base",  "irons_spellbooks:lightning_magic_resist",  0.35, "add_multiplied_base");
        addRune(29, "Sur",   "瑟",     3, "examplemod:hit_blind",                   25.0, "add_value",            "irons_spellbooks:max_mana",                0.10, "add_multiplied_base",  "irons_spellbooks:max_mana",                90.0, "add_value");
        addRune(30, "Ber",   "贝",     3, "apothic_attributes:current_hp_damage",   0.05, "add_value",            "l2damagetracker:damage_reduction",         0.06, "add_multiplied_base",  "l2damagetracker:damage_reduction",         0.06, "add_multiplied_base");
        addRune(31, "Jah",   "乔",     3, "apothic_attributes:armor_pierce",        6.0,  "add_value",            "minecraft:generic.max_health",             0.08, "add_multiplied_base",  "minecraft:generic.max_health",             8.0,  "add_value");
// 武器镶嵌直接给 +200.0 冰冷值！防具/盾牌保留 2.5 无法冰冻
        addRune(32, "Cham", "查姆", 3,
                "examplemod:freeze_target",   200.0, "add_value",
                "examplemod:cannot_be_frozen", 2.5,  "add_value",
                "examplemod:cannot_be_frozen", 2.5,  "add_value");
        addRune(33, "Zod",   "佐德",   3, "minecraft:generic.attack_damage",        6.5,  "add_value",            "minecraft:generic.armor",                  6.0,  "add_value",            "l2weaponry:shield_defense",                12.0, "add_value");

        // ==================== 7 种 D2 经典镶嵌宝石（按铁魔法体系转换） ====================
        D2_GEMS.add(new GemStat("d2_sapphire", "Sapphire", "暗黑蓝宝石", 0,
                "irons_spellbooks:ice_spell_power",       0.18, "add_multiplied_base",
                "irons_spellbooks:max_mana",              50.0, "add_value",
                "irons_spellbooks:ice_magic_resist",      0.30, "add_multiplied_base"));

        D2_GEMS.add(new GemStat("d2_emerald", "Emerald", "暗黑绿宝石", 0,
                "irons_spellbooks:nature_spell_power",    0.18, "add_multiplied_base",
                "apothic_attributes:dodge_chance",        0.06, "add_value",
                "irons_spellbooks:nature_magic_resist",   0.30, "add_multiplied_base"));

        D2_GEMS.add(new GemStat("d2_topaz", "Topaz", "暗黑黄宝石", 0,
                "irons_spellbooks:lightning_spell_power", 0.18, "add_multiplied_base",
                "minecraft:generic.luck",                 1.5,  "add_value",
                "irons_spellbooks:lightning_magic_resist",0.30, "add_multiplied_base"));

        D2_GEMS.add(new GemStat("d2_ruby", "Ruby", "暗黑红宝石", 0,
                "irons_spellbooks:fire_spell_power",      0.18, "add_multiplied_base",
                "minecraft:generic.max_health",           4.0,  "add_value",
                "irons_spellbooks:fire_magic_resist",     0.30, "add_multiplied_base"));

        D2_GEMS.add(new GemStat("d2_amethyst", "Amethyst", "暗黑紫宝石", 0,
                "apothic_attributes:crit_chance",         0.08, "add_value",
                "minecraft:generic.attack_damage",        2.0,  "add_value",
                "minecraft:generic.armor",                4.0,  "add_value"));

        D2_GEMS.add(new GemStat("d2_diamond", "Diamond", "暗黑钻石", 0,
                "irons_spellbooks:holy_spell_power",      0.18, "add_multiplied_base",
                "apothic_attributes:armor_pierce",        2.5,  "add_value",
                "irons_spellbooks:spell_resist",          0.20, "add_multiplied_base"));

        D2_GEMS.add(new GemStat("d2_skull", "Skull", "暗黑骷髅", 0,
                "apothic_attributes:life_steal",          0.04, "add_value",
                "irons_spellbooks:mana_regen",            0.18, "add_multiplied_base",
                "l2weaponry:reflect_time",                2.5,  "add_value"));
    }

    private static void addRune(int id, String name, String cnName, int group,
                                String wAttr, double wVal, String wOp,
                                String aAttr, double aVal, String aOp,
                                String sAttr, double sVal, String sOp) {
        String slug = String.format("runes/rune_%02d_%s", id, name.toLowerCase(Locale.ROOT));
        RUNES.put(id, new RuneInfo(id, name, cnName,
                new GemStat(slug, name, cnName, group, wAttr, wVal, wOp, aAttr, aVal, aOp, sAttr, sVal, sOp)));
    }

    public static void main(String[] args) {
        String rootDir = "examplemod_Datapack";
        deleteDirectory(new File(rootDir));

        createPackMcmeta(rootDir);
        generateLanguageFiles(rootDir);
        generateItemTags(rootDir);
        generateQualityTags(rootDir); // 新增此处调用
        generateGemFiles(rootDir);
        generateRunewordData(rootDir);
        generateGemModels(rootDir);
        generateRuneUpgradeRecipe(rootDir);

        System.out.println(">>> 全部生成完成！目录: " + rootDir);
    }
    private static void generateQualityTags(String root) {
        File qDir = new File(root, "data/examplemod/tags/item/quality");
        qDir.mkdirs();

        // ==================== 1. 暗金品质 (Unique / 独特) ====================
        writeTag(new File(qDir, "unique.json"), List.of(
                // Simply Swords 专属暗金神器 (补全 harbinger, sunfire, tempest)
                "simplyswords:arcanethyst", "simplyswords:awakened_lichblade", "simplyswords:slumbering_lichblade", "simplyswords:waking_lichblade",
                "simplyswords:bloodwake", "simplyswords:bramblethorn", "simplyswords:brimstone_claymore", "simplyswords:caelestis", "simplyswords:chompolotl", "simplyswords:dawnquiver",
                "simplyswords:dreadwhisper", "simplyswords:emberblade", "simplyswords:emberlash", "simplyswords:enigma", "simplyswords:flamewind", "simplyswords:frostfall", "simplyswords:gloampiercer",
                "simplyswords:harbinger", "simplyswords:hearthflame", "simplyswords:hiveheart", "simplyswords:icewhisper", "simplyswords:ionbound_stormscale", "simplyswords:livyatan", "simplyswords:magiblade",
                "simplyswords:magiscythe", "simplyswords:magispear", "simplyswords:mjolnir", "simplyswords:molten_edge", "simplyswords:ribboncleaver", "simplyswords:riftmane", "simplyswords:shadowsting",
                "simplyswords:soulkeeper", "simplyswords:soulpyre", "simplyswords:soulrender", "simplyswords:soulstalker", "simplyswords:soulstealer", "simplyswords:stars_edge", "simplyswords:stormbringer",
                "simplyswords:storms_edge", "simplyswords:stormscale", "simplyswords:sunfire", "simplyswords:tempest", "simplyswords:the_devourer", "simplyswords:thunderbrand", "simplyswords:toxic_longsword",
                "simplyswords:twisted_blade", "simplyswords:watcher_claymore", "simplyswords:watching_warglaive", "simplyswords:waxweaver", "simplyswords:whisperwind", "simplyswords:wickpiercer",
                "simplyswords:wraithfang", "simplyswords:wraithmaw",
                // Cataclysm 灾变 BOSS 掉落神器 & 独立暗金防具
                "cataclysm:the_incinerator", "cataclysm:the_annihilator", "cataclysm:the_immolator", "cataclysm:ceraunus", "cataclysm:brontes", "cataclysm:soul_render", "cataclysm:meat_shredder",
                "cataclysm:infernal_forge", "cataclysm:void_forge", "cataclysm:tidal_claws", "cataclysm:astrape", "cataclysm:ancient_spear", "cataclysm:cursed_bow", "cataclysm:gauntlet_of_bulwark",
                "cataclysm:gauntlet_of_guard", "cataclysm:gauntlet_of_maelstrom", "cataclysm:athame",
                "cataclysm:bloom_stone_pauldrons", "cataclysm:monstrous_helm",
                // L2Weaponry 独立机制暗金
                "l2weaponry:death_scythe", "l2weaponry:abyss_echo", "l2weaponry:abyss_resonance", "l2weaponry:abyss_shock", "l2weaponry:abyss_terror", "l2weaponry:barbaric_hallow", "l2weaponry:blade_of_illusion",
                "l2weaponry:claw_of_determination", "l2weaponry:hammer_of_incarceration", "l2weaponry:haunting_demon_of_the_end", "l2weaponry:shadow_hunter", "l2weaponry:shadow_shredder", "l2weaponry:dogmatic_punishment",
                "l2weaponry:dogmatic_standoff", "l2weaponry:poseidon_madness", "l2weaponry:axe_of_cursed_flame", "l2weaponry:soul_harvester", "l2weaponry:ancient_traveller", "l2weaponry:vampire_desire", "l2weaponry:void_escape", "l2weaponry:spear_of_winter_storm",
                // Mowzie's Mobs 武器与独特头盔
                "mowziesmobs:wrought_axe", "mowziesmobs:earthrend_gauntlet", "mowziesmobs:naga_fang_dagger", "mowziesmobs:sculptor_staff", "mowziesmobs:blowgun",
                "mowziesmobs:sol_visage", "mowziesmobs:wrought_helmet",
                // Iron's Spells & 东方魔法 & Tunes 'n Tomes 神器与独特防具
                "irons_spellbooks:hellrazor", "irons_spellbooks:boreal_blade", "irons_spellbooks:amethyst_rapier", "irons_spellbooks:misery", "irons_spellbooks:spellbreaker", "irons_spellbooks:twilight_gale",
                "irons_spellbooks:infernal_sorcerer_chestplate", "irons_spellbooks:paladin_chestplate", "irons_spellbooks:speed_boots",
                "iss_magicfromtheeast:jade_guandao", "iss_magicfromtheeast:muramasa", "iss_magicfromtheeast:soul_breaker", "iss_magicfromtheeast:soul_katana", "iss_magicfromtheeast:spirit_crusher", "iss_magicfromtheeast:soulpiercer",
                "iss_magicfromtheeast:elemental_commander_chestplate", "iss_magicfromtheeast:mist_boots",
                "tunes_n_tomes:bard_harp",
                // 其他独立神器与独特装备
                "bosses_of_mass_destruction:earthdive_spear", "endermanoverhaul:corrupted_blade", "endermanoverhaul:corrupted_shield",
                "twilightforest:giant_sword", "twilightforest:glass_sword", "twilightforest:mystic_crown", "twilightdelight:teardrop_sword",
                "minecraft:elytra"
        ));

        // ==================== 2. 绿色品质 (Set / 套装) ====================
        writeTag(new File(qDir, "set.json"), List.of(
                // 暮色森林套装武器与防具 (铁木、钢叶、骑士金属、炽铁、极地、雪怪、娜迦、幻影)
                "twilightforest:ironwood_sword", "twilightforest:ironwood_axe",
                "twilightforest:steeleaf_sword", "twilightforest:steeleaf_axe",
                "twilightforest:knightmetal_sword", "twilightforest:knightmetal_axe", "twilightforest:knightmetal_shield",
                "twilightforest:fiery_sword",
                "twilightforest:ironwood_helmet", "twilightforest:ironwood_chestplate", "twilightforest:ironwood_leggings", "twilightforest:ironwood_boots",
                "twilightforest:steeleaf_helmet", "twilightforest:steeleaf_chestplate", "twilightforest:steeleaf_leggings", "twilightforest:steeleaf_boots",
                "twilightforest:knightmetal_helmet", "twilightforest:knightmetal_chestplate", "twilightforest:knightmetal_leggings", "twilightforest:knightmetal_boots",
                "twilightforest:fiery_helmet", "twilightforest:fiery_chestplate", "twilightforest:fiery_leggings", "twilightforest:fiery_boots",
                "twilightforest:arctic_helmet", "twilightforest:arctic_chestplate", "twilightforest:arctic_leggings", "twilightforest:arctic_boots",
                "twilightforest:yeti_helmet", "twilightforest:yeti_chestplate", "twilightforest:yeti_leggings", "twilightforest:yeti_boots",
                "twilightforest:naga_chestplate", "twilightforest:naga_leggings",
                "twilightforest:phantom_helmet", "twilightforest:phantom_chestplate",
                // KnavesNeeds 暮色 4 材质全套冷兵器
                "knavesneeds:twilightforest/ironwood/chakram", "knavesneeds:twilightforest/ironwood/claymore", "knavesneeds:twilightforest/ironwood/cutlass", "knavesneeds:twilightforest/ironwood/glaive", "knavesneeds:twilightforest/ironwood/greataxe", "knavesneeds:twilightforest/ironwood/greathammer", "knavesneeds:twilightforest/ironwood/halberd", "knavesneeds:twilightforest/ironwood/katana", "knavesneeds:twilightforest/ironwood/longsword", "knavesneeds:twilightforest/ironwood/rapier", "knavesneeds:twilightforest/ironwood/sai", "knavesneeds:twilightforest/ironwood/scythe", "knavesneeds:twilightforest/ironwood/spear", "knavesneeds:twilightforest/ironwood/twinblade", "knavesneeds:twilightforest/ironwood/warglaive",
                "knavesneeds:twilightforest/steeleaf/chakram", "knavesneeds:twilightforest/steeleaf/claymore", "knavesneeds:twilightforest/steeleaf/cutlass", "knavesneeds:twilightforest/steeleaf/glaive", "knavesneeds:twilightforest/steeleaf/greataxe", "knavesneeds:twilightforest/steeleaf/greathammer", "knavesneeds:twilightforest/steeleaf/halberd", "knavesneeds:twilightforest/steeleaf/katana", "knavesneeds:twilightforest/steeleaf/longsword", "knavesneeds:twilightforest/steeleaf/rapier", "knavesneeds:twilightforest/steeleaf/sai", "knavesneeds:twilightforest/steeleaf/scythe", "knavesneeds:twilightforest/steeleaf/spear", "knavesneeds:twilightforest/steeleaf/twinblade", "knavesneeds:twilightforest/steeleaf/warglaive",
                "knavesneeds:twilightforest/knightmetal/chakram", "knavesneeds:twilightforest/knightmetal/claymore", "knavesneeds:twilightforest/knightmetal/cutlass", "knavesneeds:twilightforest/knightmetal/glaive", "knavesneeds:twilightforest/knightmetal/greataxe", "knavesneeds:twilightforest/knightmetal/greathammer", "knavesneeds:twilightforest/knightmetal/halberd", "knavesneeds:twilightforest/knightmetal/katana", "knavesneeds:twilightforest/knightmetal/longsword", "knavesneeds:twilightforest/knightmetal/rapier", "knavesneeds:twilightforest/knightmetal/sai", "knavesneeds:twilightforest/knightmetal/scythe", "knavesneeds:twilightforest/knightmetal/spear", "knavesneeds:twilightforest/knightmetal/twinblade", "knavesneeds:twilightforest/knightmetal/warglaive",
                "knavesneeds:twilightforest/fiery/chakram", "knavesneeds:twilightforest/fiery/claymore", "knavesneeds:twilightforest/fiery/cutlass", "knavesneeds:twilightforest/fiery/glaive", "knavesneeds:twilightforest/fiery/greataxe", "knavesneeds:twilightforest/fiery/greathammer", "knavesneeds:twilightforest/fiery/halberd", "knavesneeds:twilightforest/fiery/katana", "knavesneeds:twilightforest/fiery/longsword", "knavesneeds:twilightforest/fiery/rapier", "knavesneeds:twilightforest/fiery/sai", "knavesneeds:twilightforest/fiery/scythe", "knavesneeds:twilightforest/fiery/spear", "knavesneeds:twilightforest/fiery/twinblade", "knavesneeds:twilightforest/fiery/warglaive",
                // L2Weaponry 暮色 4 材质全套冷兵器 + 圆盾
                "l2weaponry:ironwood_battle_axe", "l2weaponry:ironwood_claw", "l2weaponry:ironwood_dagger", "l2weaponry:ironwood_hammer", "l2weaponry:ironwood_javelin", "l2weaponry:ironwood_machete", "l2weaponry:ironwood_nunchaku", "l2weaponry:ironwood_plate_shield", "l2weaponry:ironwood_round_shield", "l2weaponry:ironwood_scythe", "l2weaponry:ironwood_spear", "l2weaponry:ironwood_throwing_axe",
                "l2weaponry:steeleaf_battle_axe", "l2weaponry:steeleaf_claw", "l2weaponry:steeleaf_dagger", "l2weaponry:steeleaf_hammer", "l2weaponry:steeleaf_javelin", "l2weaponry:steeleaf_machete", "l2weaponry:steeleaf_nunchaku", "l2weaponry:steeleaf_plate_shield", "l2weaponry:steeleaf_round_shield", "l2weaponry:steeleaf_scythe", "l2weaponry:steeleaf_spear", "l2weaponry:steeleaf_throwing_axe",
                "l2weaponry:knightmetal_battle_axe", "l2weaponry:knightmetal_claw", "l2weaponry:knightmetal_dagger", "l2weaponry:knightmetal_hammer", "l2weaponry:knightmetal_javelin", "l2weaponry:knightmetal_machete", "l2weaponry:knightmetal_nunchaku", "l2weaponry:knightmetal_plate_shield", "l2weaponry:knightmetal_round_shield", "l2weaponry:knightmetal_scythe", "l2weaponry:knightmetal_spear", "l2weaponry:knightmetal_throwing_axe",
                "l2weaponry:fiery_battle_axe", "l2weaponry:fiery_claw", "l2weaponry:fiery_dagger", "l2weaponry:fiery_hammer", "l2weaponry:fiery_javelin", "l2weaponry:fiery_machete", "l2weaponry:fiery_nunchaku", "l2weaponry:fiery_plate_shield", "l2weaponry:fiery_round_shield", "l2weaponry:fiery_scythe", "l2weaponry:fiery_spear", "l2weaponry:fiery_throwing_axe",
                // Cataclysm 灾变防具套装 (咒魂、腾炎、骸龙)
                "cataclysm:cursium_helmet", "cataclysm:cursium_chestplate", "cataclysm:cursium_leggings", "cataclysm:cursium_boots",
                "cataclysm:ignitium_helmet", "cataclysm:ignitium_chestplate", "cataclysm:ignitium_elytra_chestplate", "cataclysm:ignitium_leggings", "cataclysm:ignitium_boots",
                "cataclysm:bone_reptile_helmet", "cataclysm:bone_reptile_chestplate",
                // Dungeon Now Loading 刷怪笼套装
                "dungeonnowloading:spawner_helmet", "dungeonnowloading:spawner_chestplate", "dungeonnowloading:spawner_leggings", "dungeonnowloading:spawner_boots",
                // Iron's Spells 套装大剑 & 流派法师法袍套装
                "irons_spellbooks:keeper_flamberge", "irons_spellbooks:legionnaire_flamberge",
                "irons_spellbooks:archevoker_helmet", "irons_spellbooks:archevoker_chestplate", "irons_spellbooks:archevoker_leggings", "irons_spellbooks:archevoker_boots",
                "irons_spellbooks:cryomancer_helmet", "irons_spellbooks:cryomancer_chestplate", "irons_spellbooks:cryomancer_leggings", "irons_spellbooks:cryomancer_boots",
                "irons_spellbooks:cultist_helmet", "irons_spellbooks:cultist_chestplate", "irons_spellbooks:cultist_leggings", "irons_spellbooks:cultist_boots",
                "irons_spellbooks:electromancer_helmet", "irons_spellbooks:electromancer_chestplate", "irons_spellbooks:electromancer_leggings", "irons_spellbooks:electromancer_boots",
                "irons_spellbooks:plagued_helmet", "irons_spellbooks:plagued_chestplate", "irons_spellbooks:plagued_leggings", "irons_spellbooks:plagued_boots",
                "irons_spellbooks:priest_helmet", "irons_spellbooks:priest_chestplate", "irons_spellbooks:priest_leggings", "irons_spellbooks:priest_boots",
                "irons_spellbooks:pyromancer_helmet", "irons_spellbooks:pyromancer_chestplate", "irons_spellbooks:pyromancer_leggings", "irons_spellbooks:pyromancer_boots",
                "irons_spellbooks:shadowwalker_helmet", "irons_spellbooks:shadowwalker_chestplate", "irons_spellbooks:shadowwalker_leggings", "irons_spellbooks:shadowwalker_boots",
                "irons_spellbooks:wizard_hat", "irons_spellbooks:wizard_helmet", "irons_spellbooks:wizard_chestplate", "irons_spellbooks:wizard_leggings", "irons_spellbooks:wizard_boots",
                // ISS 东方魔法套装
                "iss_magicfromtheeast:jade_pagoda_helmet", "iss_magicfromtheeast:jade_pagoda_chestplate", "iss_magicfromtheeast:jade_pagoda_leggings", "iss_magicfromtheeast:jade_pagoda_boots",
                "iss_magicfromtheeast:onmyoji_helmet", "iss_magicfromtheeast:onmyoji_chestplate", "iss_magicfromtheeast:onmyoji_leggings", "iss_magicfromtheeast:onmyoji_boots",
                "iss_magicfromtheeast:taoist_helmet", "iss_magicfromtheeast:taoist_chestplate", "iss_magicfromtheeast:taoist_leggings", "iss_magicfromtheeast:taoist_boots",
                // Tunes 'n Tomes 吟游诗人套装
                "tunes_n_tomes:bard_helmet", "tunes_n_tomes:bard_chestplate", "tunes_n_tomes:bard_leggings", "tunes_n_tomes:bard_boots",
                "tunes_n_tomes:melodic_mastery_helmet", "tunes_n_tomes:melodic_mastery_chestplate", "tunes_n_tomes:melodic_mastery_leggings", "tunes_n_tomes:melodic_mastery_boots",
                // Mowzie's Mobs 地卜师套装
                "mowziesmobs:geomancer_beads", "mowziesmobs:geomancer_robe", "mowziesmobs:geomancer_belt", "mowziesmobs:geomancer_sandals"
        ));

        // ==================== 3. 黄色品质 (Rare / 稀有) ====================
        writeTag(new File(qDir, "rare.json"), List.of(
                // L2Weaponry 终极高阶合金 (咒魂、腾炎、凋零、幽匿、潜影、永恒)
                "l2weaponry:cursium_battle_axe", "l2weaponry:cursium_claw", "l2weaponry:cursium_dagger", "l2weaponry:cursium_javelin", "l2weaponry:cursium_machete", "l2weaponry:cursium_nunchaku", "l2weaponry:cursium_scythe", "l2weaponry:cursium_spear", "l2weaponry:cursium_throwing_axe",
                "l2weaponry:ignitium_battle_axe", "l2weaponry:ignitium_claw", "l2weaponry:ignitium_dagger", "l2weaponry:ignitium_javelin", "l2weaponry:ignitium_machete", "l2weaponry:ignitium_nunchaku", "l2weaponry:ignitium_scythe", "l2weaponry:ignitium_spear", "l2weaponry:ignitium_throwing_axe",
                "l2weaponry:witherite_battle_axe", "l2weaponry:witherite_claw", "l2weaponry:witherite_dagger", "l2weaponry:witherite_javelin", "l2weaponry:witherite_machete", "l2weaponry:witherite_nunchaku", "l2weaponry:witherite_scythe", "l2weaponry:witherite_spear", "l2weaponry:witherite_throwing_axe",
                "l2weaponry:sculkium_battle_axe", "l2weaponry:sculkium_claw", "l2weaponry:sculkium_dagger", "l2weaponry:sculkium_hammer", "l2weaponry:sculkium_javelin", "l2weaponry:sculkium_machete", "l2weaponry:sculkium_nunchaku", "l2weaponry:sculkium_plate_shield", "l2weaponry:sculkium_round_shield", "l2weaponry:sculkium_scythe", "l2weaponry:sculkium_spear", "l2weaponry:sculkium_throwing_axe",
                "l2weaponry:shulkerate_battle_axe", "l2weaponry:shulkerate_claw", "l2weaponry:shulkerate_dagger", "l2weaponry:shulkerate_hammer", "l2weaponry:shulkerate_javelin", "l2weaponry:shulkerate_machete", "l2weaponry:shulkerate_nunchaku", "l2weaponry:shulkerate_plate_shield", "l2weaponry:shulkerate_round_shield", "l2weaponry:shulkerate_scythe", "l2weaponry:shulkerate_spear", "l2weaponry:shulkerate_throwing_axe",
                "l2weaponry:eternium_battle_axe", "l2weaponry:eternium_claw", "l2weaponry:eternium_dagger", "l2weaponry:eternium_hammer", "l2weaponry:eternium_javelin", "l2weaponry:eternium_machete", "l2weaponry:eternium_nunchaku", "l2weaponry:eternium_plate_shield", "l2weaponry:eternium_round_shield", "l2weaponry:eternium_scythe", "l2weaponry:eternium_spear", "l2weaponry:eternium_throwing_axe",
                // L2Complements 高阶合金武器与防具
                "l2complements:sculkium_sword", "l2complements:sculkium_axe", "l2complements:shulkerate_sword", "l2complements:shulkerate_axe",
                "l2complements:sculkium_helmet", "l2complements:sculkium_chestplate", "l2complements:sculkium_leggings", "l2complements:sculkium_boots",
                "l2complements:shulkerate_helmet", "l2complements:shulkerate_chestplate", "l2complements:shulkerate_leggings", "l2complements:shulkerate_boots",
                // L2Archery 终极神弓
                "l2archery:eagle_bow", "l2archery:earth_bow", "l2archery:ender_aim_bow", "l2archery:explosion_bow", "l2archery:flame_bow", "l2archery:froze_bow", "l2archery:gaia_bow", "l2archery:magnify_bow", "l2archery:storm_bow", "l2archery:sun_bow", "l2archery:turtle_bow", "l2archery:winter_bow",
                // Simply Swords 遗物系列
                "simplyswords:decaying_relic", "simplyswords:dormant_relic", "simplyswords:righteous_relic", "simplyswords:tainted_relic",
                // 灾变精良武器/盾牌与地牢稀有物
                "cataclysm:black_steel_sword", "cataclysm:black_steel_axe", "cataclysm:black_steel_targe", "cataclysm:azure_sea_shield", "cataclysm:coral_bardiche", "cataclysm:coral_spear", "cataclysm:khopesh",
                "dungeonnowloading:spawner_sword", "dungeonnowloading:life_stealer", "dungeonnowloading:vertex_bow",
                // 铁魔法下界合金战法袍 & 黯淡皇冠
                "irons_spellbooks:netherite_mage_helmet", "irons_spellbooks:netherite_mage_chestplate", "irons_spellbooks:netherite_mage_leggings", "irons_spellbooks:netherite_mage_boots",
                "irons_spellbooks:tarnished_helmet",
                // 暮色稀有弓与特种斧
                "twilightforest:ice_sword", "twilightforest:ice_bow", "twilightforest:ender_bow", "twilightforest:seeker_bow", "twilightforest:triple_bow", "twilightforest:diamond_minotaur_axe", "twilightforest:gold_minotaur_axe",
                // 原版稀有武器 (不需要可删)
                "minecraft:trident", "minecraft:mace"
        ));

        // ==================== 4. 蓝色品质 (Magic / 魔法) ====================
        writeTag(new File(qDir, "magic.json"), List.of(
                // 农夫乐事与暮色乐事刀具/煎锅/战斧牛排
                "farmersdelight:flint_knife", "farmersdelight:iron_knife", "farmersdelight:golden_knife", "farmersdelight:diamond_knife", "farmersdelight:netherite_knife", "farmersdelight:skillet",
                "twilightdelight:ironwood_knife", "twilightdelight:knightmetal_knife", "twilightdelight:steeleaf_knife", "twilightdelight:fiery_knife", "twilightdelight:ice_knife", "twilightdelight:helmet_crab_claw", "twilightdelight:wrought_iron_sword",
                "twilightdelight:raw_tomahawk_smeaxe", "twilightdelight:cooked_tomahawk_smeaxe",
                // 铁魔法基础法杖、武器与入门法袍
                "irons_spellbooks:graybeard_staff", "irons_spellbooks:artificer_cane", "irons_spellbooks:ice_staff", "irons_spellbooks:lightning_rod", "irons_spellbooks:blood_staff", "irons_spellbooks:pyrium_staff",
                "irons_spellbooks:autoloader_crossbow", "irons_spellbooks:magehunter", "irons_spellbooks:decrepit_scythe",
                "irons_spellbooks:wandering_magician_helmet", "irons_spellbooks:wandering_magician_chestplate", "irons_spellbooks:wandering_magician_leggings", "irons_spellbooks:wandering_magician_boots",
                "irons_spellbooks:pumpkin_helmet", "irons_spellbooks:pumpkin_chestplate", "irons_spellbooks:pumpkin_leggings", "irons_spellbooks:pumpkin_boots",
                // L2Complements 基础进阶武器与防具
                "l2complements:eternium_sword", "l2complements:eternium_axe", "l2complements:poseidite_sword", "l2complements:poseidite_axe", "l2complements:totemic_gold_sword", "l2complements:totemic_gold_axe",
                "l2complements:eternium_helmet", "l2complements:eternium_chestplate", "l2complements:eternium_leggings", "l2complements:eternium_boots",
                "l2complements:poseidite_helmet", "l2complements:poseidite_chestplate", "l2complements:poseidite_leggings", "l2complements:poseidite_boots",
                "l2complements:totemic_gold_helmet", "l2complements:totemic_gold_chestplate", "l2complements:totemic_gold_leggings", "l2complements:totemic_gold_boots",
                // L2Weaponry 入门进阶金属 (远古金属、黑钢、海神、生命黄金)
                "l2weaponry:ancient_metal_battle_axe", "l2weaponry:ancient_metal_claw", "l2weaponry:ancient_metal_dagger", "l2weaponry:ancient_metal_javelin", "l2weaponry:ancient_metal_machete", "l2weaponry:ancient_metal_nunchaku", "l2weaponry:ancient_metal_scythe", "l2weaponry:ancient_metal_spear", "l2weaponry:ancient_metal_throwing_axe",
                "l2weaponry:black_steel_battle_axe", "l2weaponry:black_steel_claw", "l2weaponry:black_steel_dagger", "l2weaponry:black_steel_javelin", "l2weaponry:black_steel_machete", "l2weaponry:black_steel_nunchaku", "l2weaponry:black_steel_scythe", "l2weaponry:black_steel_spear", "l2weaponry:black_steel_throwing_axe",
                "l2weaponry:poseidite_battle_axe", "l2weaponry:poseidite_claw", "l2weaponry:poseidite_dagger", "l2weaponry:poseidite_hammer", "l2weaponry:poseidite_javelin", "l2weaponry:poseidite_machete", "l2weaponry:poseidite_nunchaku", "l2weaponry:poseidite_plate_shield", "l2weaponry:poseidite_round_shield", "l2weaponry:poseidite_scythe", "l2weaponry:poseidite_spear", "l2weaponry:poseidite_throwing_axe",
                "l2weaponry:totemic_gold_battle_axe", "l2weaponry:totemic_gold_claw", "l2weaponry:totemic_gold_dagger", "l2weaponry:totemic_gold_hammer", "l2weaponry:totemic_gold_javelin", "l2weaponry:totemic_gold_machete", "l2weaponry:totemic_gold_nunchaku", "l2weaponry:totemic_gold_plate_shield", "l2weaponry:totemic_gold_round_shield", "l2weaponry:totemic_gold_scythe", "l2weaponry:totemic_gold_spear", "l2weaponry:totemic_gold_throwing_axe",
                // Simply Swords 符文武器 & 棍剑
                "simplyswords:runic_chakram", "simplyswords:runic_claymore", "simplyswords:runic_cutlass", "simplyswords:runic_glaive", "simplyswords:runic_greataxe", "simplyswords:runic_greathammer", "simplyswords:runic_halberd", "simplyswords:runic_katana", "simplyswords:runic_longsword", "simplyswords:runic_rapier", "simplyswords:runic_sai", "simplyswords:runic_scythe", "simplyswords:runic_spear", "simplyswords:runic_twinblade", "simplyswords:runic_warglaive",
                "simplyswords:sword_on_a_stick",
                // L2Archery 进阶弓
                "l2archery:iron_bow", "l2archery:master_bow", "l2archery:glow_aim_bow", "l2archery:slow_bow", "l2archery:wind_bow",
                // 暮色旅行者防具 & 莫西长矛/面具 & 东方魔法 & 末影兜帽
                "twilightforest:travellers_goggles", "twilightforest:travellers_vest", "twilightforest:travellers_wings", "twilightforest:travellers_boots",
                "mowziesmobs:spear", "mowziesmobs:umvuthana_mask_bliss", "mowziesmobs:umvuthana_mask_faith", "mowziesmobs:umvuthana_mask_fear", "mowziesmobs:umvuthana_mask_fury", "mowziesmobs:umvuthana_mask_misery", "mowziesmobs:umvuthana_mask_rage",
                "iss_magicfromtheeast:repeating_crossbow", "iss_magicfromtheeast:taiji_sword", "iss_magicfromtheeast:jiangshi_hat",
                "endermanoverhaul:badlands_hood", "endermanoverhaul:savanna_hood", "endermanoverhaul:snowy_hood",
                "twilightdelight:antler_bow", "examplemod:turtle_shield"
        ));

        System.out.println(">>> 成功生成 4 种衍生品质 Tags (Unique, Set, Rare, Magic)，白色底材直接沿用 base/all！");
    }
    private static String getRuneGemPath(int runeId) {
        return RUNES.get(runeId).stat.slug;
    }

    private static void generateRuneUpgradeRecipe(String root) {
        File recipeFile = new File(root, "data/examplemod/recipe/rune_upgrade.json");
        writeFile(recipeFile, """
                {
                  "type": "examplemod:rune_upgrade",
                  "category": "misc"
                }
                """);
    }

    private static void generateGemFiles(String root) {
        for (RuneInfo r : RUNES.values()) {
            File out = new File(root, "data/examplemod/gems/" + r.stat.slug + ".json");
            writeFile(out, serializeThreeSlotGemJson(r.stat));
        }
        for (GemStat gem : D2_GEMS) {
            File out = new File(root, "data/examplemod/gems/d2_gems/" + gem.slug + ".json");
            writeFile(out, serializeThreeSlotGemJson(gem));
        }
        System.out.println(">>> 成功生成 33 颗符文 + 7 颗 D2 宝石 JSON！");
    }

    private static String serializeThreeSlotGemJson(GemStat g) {
        int wHaven, wFrontier, wAscent, wSummit, wPinnacle;
        switch (g.tierGroup) {
            case 1 -> { wHaven = 22; wFrontier = 18; wAscent = 12; wSummit = 8;  wPinnacle = 5;  }
            case 2 -> { wHaven = 0;  wFrontier = 12; wAscent = 16; wSummit = 14; wPinnacle = 10; }
            case 3 -> { wHaven = 0;  wFrontier = 0;  wAscent = 4;  wSummit = 8;  wPinnacle = 10; }
            default ->{ wHaven = 25; wFrontier = 25; wAscent = 20; wSummit = 15; wPinnacle = 10; }
        }

        return """
            {
              "type": "apotheosis:gem",
              "min_purity": "cracked",
              "unique": false,
              "weights": {
                "haven":    { "weight": %d, "quality": 0.0 },
                "frontier": { "weight": %d, "quality": 1.0 },
                "ascent":   { "weight": %d, "quality": 2.0 },
                "summit":   { "weight": %d, "quality": 3.5 },
                "pinnacle": { "weight": %d, "quality": 5.0 }
              },
              "bonuses": [
                {
                  "type": "apotheosis:attribute",
                  "gem_class": {
                    "key": "weapons",
                    "types": [
                      "apotheosis:melee_weapon",
                      "apotheosis:trident",
                      "apotheosis:bow"
                    ]
                  },
                  "attribute": "%s",
                  "operation": "%s",
                  "values": %s
                },
                {
                  "type": "apotheosis:attribute",
                  "gem_class": {
                    "key": "armor",
                    "types": [
                      "apotheosis:helmet",
                      "apotheosis:chestplate",
                      "apotheosis:leggings",
                      "apotheosis:boots"
                    ]
                  },
                  "attribute": "%s",
                  "operation": "%s",
                  "values": %s
                },
                {
                  "type": "apotheosis:attribute",
                  "gem_class": "apotheosis:shield",
                  "attribute": "%s",
                  "operation": "%s",
                  "values": %s
                }
              ]
            }
            """.formatted(
                wHaven, wFrontier, wAscent, wSummit, wPinnacle,
                g.weaponAttr, g.weaponOp, formatSixPurities(g.weaponVal),
                g.armorAttr,  g.armorOp,  formatSixPurities(g.armorVal),
                g.shieldAttr, g.shieldOp, formatSixPurities(g.shieldVal)
        );
    }

    private static void generateGemModels(String root) {
        for (RuneInfo r : RUNES.values()) {
            String tex = "examplemod:item/runes/" + r.stat.slug.substring("runes/".length());
            File modelFile = new File(root, "assets/apotheosis/models/item/gems/" + r.stat.slug + ".json");
            writeFile(modelFile, """
                    {
                      "parent": "minecraft:item/generated",
                      "textures": {
                        "layer0": "%s"
                      }
                    }
                    """.formatted(tex));
        }

        List<String> tiers = List.of("chipped", "flawed", "normal", "flawless", "perfect");

        for (GemStat gem : D2_GEMS) {
            for (String tier : tiers) {
                File subModel = new File(root, "assets/examplemod/models/item/d2_gems/" + gem.slug + "_" + tier + ".json");
                writeFile(subModel, """
                        {
                          "parent": "minecraft:item/generated",
                          "textures": {
                            "layer0": "examplemod:item/d2_gems/%s_%s"
                          }
                        }
                        """.formatted(gem.slug, tier));
            }

            File mainModel = new File(root, "assets/apotheosis/models/item/gems/d2_gems/" + gem.slug + ".json");
            writeFile(mainModel, """
                    {
                      "parent": "minecraft:item/generated",
                      "textures": {
                        "layer0": "examplemod:item/d2_gems/%s_chipped"
                      },
                      "overrides": [
                        { "predicate": { "apotheosis:purity": 1.0 }, "model": "examplemod:item/d2_gems/%s_chipped" },
                        { "predicate": { "apotheosis:purity": 2.0 }, "model": "examplemod:item/d2_gems/%s_flawed" },
                        { "predicate": { "apotheosis:purity": 3.0 }, "model": "examplemod:item/d2_gems/%s_normal" },
                        { "predicate": { "apotheosis:purity": 4.0 }, "model": "examplemod:item/d2_gems/%s_flawless" },
                        { "predicate": { "apotheosis:purity": 5.0 }, "model": "examplemod:item/d2_gems/%s_perfect" }
                      ]
                    }
                    """.formatted(gem.slug, gem.slug, gem.slug, gem.slug, gem.slug, gem.slug));
        }
    }

    private static String formatSixPurities(double base) {
        return String.format(Locale.ROOT,
                "{ \"cracked\": %.3f, \"chipped\": %.3f, \"flawed\": %.3f, \"normal\": %.3f, \"flawless\": %.3f, \"perfect\": %.3f }",
                base * 0.40, base * 0.55, base * 0.75, base * 1.00, base * 1.25, base * 1.50
        );
    }

    private static void generateLanguageFiles(String root) {
        File langDir = new File(root, "assets/examplemod/lang");
        langDir.mkdirs();

        List<Runeword> list = buildAllRunewords();

        StringBuilder zh = new StringBuilder("{\n");
        zh.append("  \"runeword.examplemod.title_format\": \"%s · %s\",\n");
        zh.append("  \"tooltip.examplemod.runeword_tier\": \"符文之语品质: %s (%s%%)\",\n");
        zh.append("  \"tooltip.examplemod.all_spell_levels\": \"+%s 所有法术等级\",\n");
        zh.append("  \"tooltip.examplemod.available_runewords\": \"提示: 可用符文之语 (%s孔):\",\n");
        zh.append("  \"tooltip.examplemod.mixed_purity_warning\": \"警告: 混插了不同纯度的符文，将无法激活符文之语！\",\n\n");

        for (RuneInfo r : RUNES.values()) {
            String key = "item.apotheosis.gem.examplemod:" + r.stat.slug;
            zh.append("  \"").append(key).append("\": \"").append(r.cnName).append("符文 (#").append(r.id).append(")\",\n");
        }
        zh.append("\n");
        for (GemStat gem : D2_GEMS) {
            String key = "item.apotheosis.gem.examplemod:d2_gems/" + gem.slug;
            zh.append("  \"").append(key).append("\": \"").append(gem.nameCn).append("\",\n");
        }
        zh.append("\n");
        for (int i = 0; i < list.size(); i++) {
            Runeword rw = list.get(i);
            zh.append("  \"runeword.examplemod.").append(rw.id()).append("\": \"").append(rw.nameCn()).append("\"");
            if (i < list.size() - 1) zh.append(",");
            zh.append("\n");
        }
        zh.append("}\n");
        writeFile(new File(langDir, "zh_cn.json"), zh.toString());

        StringBuilder en = new StringBuilder("{\n");
        en.append("  \"runeword.examplemod.title_format\": \"%s %s\",\n");
        en.append("  \"tooltip.examplemod.runeword_tier\": \"Runeword Tier: %s (%s%%)\",\n");
        en.append("  \"tooltip.examplemod.all_spell_levels\": \"+%s to All Spell Levels\",\n");
        en.append("  \"tooltip.examplemod.available_runewords\": \"Tip: Available Runewords (%s Sockets):\",\n");
        en.append("  \"tooltip.examplemod.mixed_purity_warning\": \"Warning: Mixed rune purities will NOT activate a Runeword!\",\n\n");

        for (RuneInfo r : RUNES.values()) {
            String key = "item.apotheosis.gem.examplemod:" + r.stat.slug;
            en.append("  \"").append(key).append("\": \"").append(r.name).append(" Rune (#").append(r.id).append(")\",\n");
        }
        en.append("\n");
        for (GemStat gem : D2_GEMS) {
            String key = "item.apotheosis.gem.examplemod:d2_gems/" + gem.slug;
            en.append("  \"").append(key).append("\": \"").append(gem.nameEn).append(" (D2)\",\n");
        }
        en.append("\n");
        for (int i = 0; i < list.size(); i++) {
            Runeword rw = list.get(i);
            en.append("  \"runeword.examplemod.").append(rw.id()).append("\": \"").append(rw.nameEn()).append("\"");
            if (i < list.size() - 1) en.append(",");
            en.append("\n");
        }
        en.append("}\n");
        writeFile(new File(langDir, "en_us.json"), en.toString());
    }

    private static void generateRunewordData(String root) {
        File wordsDir = new File(root, "data/examplemod/words");
        wordsDir.mkdirs();

        for (Runeword rw : buildAllRunewords()) {
            writeFile(new File(wordsDir, rw.id + ".json"), serializeRunewordToJson(rw));
        }
    }

    private static String serializeRunewordToJson(Runeword rw) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(rw.id).append("\",\n");
        sb.append("  \"name_en\": \"").append(rw.nameEn).append("\",\n");
        sb.append("  \"name_cn\": \"").append(rw.nameCn).append("\",\n");
        sb.append("  \"sockets_required\": ").append(rw.runeSequence.size()).append(",\n");
        sb.append("  \"all_spell_levels\": ").append(rw.allSpellLevels).append(",\n");

        sb.append("  \"valid_base_tags\": [\n");
        for (int i = 0; i < rw.validBaseTags.size(); i++) {
            sb.append("    \"").append(rw.validBaseTags.get(i)).append("\"");
            if (i < rw.validBaseTags.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"sequence\": [\n");
        for (int i = 0; i < rw.runeSequence.size(); i++) {
            sb.append("    \"examplemod:").append(getRuneGemPath(rw.runeSequence.get(i))).append("\"");
            if (i < rw.runeSequence.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"attributes\": [\n");
        for (int i = 0; i < rw.attributes.size(); i++) {
            AttributeMod attr = rw.attributes.get(i);
            sb.append("    {\n");
            sb.append("      \"attribute\": \"").append(attr.attribute).append("\",\n");
            sb.append("      \"amount\": ").append(attr.amount).append(",\n");
            sb.append("      \"operation\": \"").append(attr.operation).append("\",\n");
            sb.append("      \"description\": \"").append(attr.desc).append("\"\n");
            sb.append("    }");
            if (i < rw.attributes.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void createPackMcmeta(String root) {
        writeFile(new File(root, "pack.mcmeta"), """
                {
                  "pack": {
                    "pack_format": 48,
                    "description": "Diablo II Gem Runeword System"
                  }
                }
                """);
    }

    private static void generateItemTags(String root) {
        File tagDir = new File(root, "data/examplemod/tags/item/base");
        tagDir.mkdirs();

        // 1. 单手剑 / 武士刀 / 细剑 / 弯刀 (Swords)
        writeTag(new File(tagDir, "swords.json"), List.of(
                // Minecraft 原版
                "minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword",
                "minecraft:golden_sword", "minecraft:diamond_sword", "minecraft:netherite_sword",
                // Cataclysm 基础剑
                "cataclysm:black_steel_sword", "cataclysm:khopesh",
                // Dungeon Now Loading
                "dungeonnowloading:spawner_sword", "dungeonnowloading:life_stealer",
                // L2Complements
                "l2complements:eternium_sword", "l2complements:poseidite_sword", "l2complements:sculkium_sword",
                "l2complements:shulkerate_sword", "l2complements:totemic_gold_sword",
                // L2Weaponry 弯刀系列
                "l2weaponry:ancient_metal_machete", "l2weaponry:black_steel_machete", "l2weaponry:cursium_machete",
                "l2weaponry:diamond_machete", "l2weaponry:eternium_machete", "l2weaponry:fiery_machete",
                "l2weaponry:gold_machete", "l2weaponry:ignitium_machete", "l2weaponry:iron_machete",
                "l2weaponry:ironwood_machete", "l2weaponry:knightmetal_machete", "l2weaponry:netherite_machete",
                "l2weaponry:poseidite_machete", "l2weaponry:sculkium_machete", "l2weaponry:shulkerate_machete",
                "l2weaponry:steeleaf_machete", "l2weaponry:totemic_gold_machete", "l2weaponry:witherite_machete",
                // Simply Swords (铁/金/钻/下界合金/符文: 弯刀、长剑、细剑、武士刀、双头剑、战刃、环刃)
                "simplyswords:iron_cutlass", "simplyswords:iron_longsword", "simplyswords:iron_rapier", "simplyswords:iron_katana", "simplyswords:iron_twinblade", "simplyswords:iron_warglaive", "simplyswords:iron_chakram",
                "simplyswords:gold_cutlass", "simplyswords:gold_longsword", "simplyswords:gold_rapier", "simplyswords:gold_katana", "simplyswords:gold_twinblade", "simplyswords:gold_warglaive", "simplyswords:gold_chakram",
                "simplyswords:diamond_cutlass", "simplyswords:diamond_longsword", "simplyswords:diamond_rapier", "simplyswords:diamond_katana", "simplyswords:diamond_twinblade", "simplyswords:diamond_warglaive", "simplyswords:diamond_chakram",
                "simplyswords:netherite_cutlass", "simplyswords:netherite_longsword", "simplyswords:netherite_rapier", "simplyswords:netherite_katana", "simplyswords:netherite_twinblade", "simplyswords:netherite_warglaive", "simplyswords:netherite_chakram",
                "simplyswords:runic_cutlass", "simplyswords:runic_longsword", "simplyswords:runic_rapier", "simplyswords:runic_katana", "simplyswords:runic_twinblade", "simplyswords:runic_warglaive", "simplyswords:runic_chakram",
                // KnavesNeeds (暮色材质: 弯刀、长剑、细剑、武士刀、双头剑、战刃、环刃)
                "knavesneeds:twilightforest/ironwood/cutlass", "knavesneeds:twilightforest/ironwood/longsword", "knavesneeds:twilightforest/ironwood/rapier", "knavesneeds:twilightforest/ironwood/katana", "knavesneeds:twilightforest/ironwood/twinblade", "knavesneeds:twilightforest/ironwood/warglaive", "knavesneeds:twilightforest/ironwood/chakram",
                "knavesneeds:twilightforest/steeleaf/cutlass", "knavesneeds:twilightforest/steeleaf/longsword", "knavesneeds:twilightforest/steeleaf/rapier", "knavesneeds:twilightforest/steeleaf/katana", "knavesneeds:twilightforest/steeleaf/twinblade", "knavesneeds:twilightforest/steeleaf/warglaive", "knavesneeds:twilightforest/steeleaf/chakram",
                "knavesneeds:twilightforest/knightmetal/cutlass", "knavesneeds:twilightforest/knightmetal/longsword", "knavesneeds:twilightforest/knightmetal/rapier", "knavesneeds:twilightforest/knightmetal/katana", "knavesneeds:twilightforest/knightmetal/twinblade", "knavesneeds:twilightforest/knightmetal/warglaive", "knavesneeds:twilightforest/knightmetal/chakram",
                "knavesneeds:twilightforest/fiery/cutlass", "knavesneeds:twilightforest/fiery/longsword", "knavesneeds:twilightforest/fiery/rapier", "knavesneeds:twilightforest/fiery/katana", "knavesneeds:twilightforest/fiery/twinblade", "knavesneeds:twilightforest/fiery/warglaive", "knavesneeds:twilightforest/fiery/chakram",
                // 暮色森林 原版剑
                "twilightforest:ironwood_sword", "twilightforest:knightmetal_sword", "twilightforest:steeleaf_sword",
                "twilightforest:fiery_sword", "twilightforest:ice_sword",
                // 其他模组单手剑
                "twilightdelight:wrought_iron_sword", "irons_spellbooks:magehunter", "iss_magicfromtheeast:taiji_sword"
        ));

        // 2. 双手巨剑 (Two-Handed Swords / Claymores)
        writeTag(new File(tagDir, "two_handed_swords.json"), List.of(
                "irons_spellbooks:claymore", "irons_spellbooks:keeper_flamberge", "irons_spellbooks:legionnaire_flamberge",
                "simplyswords:iron_claymore", "simplyswords:gold_claymore", "simplyswords:diamond_claymore",
                "simplyswords:netherite_claymore", "simplyswords:runic_claymore", "simplyswords:sword_on_a_stick",
                "simplyswords:decaying_relic", "simplyswords:dormant_relic", "simplyswords:righteous_relic", "simplyswords:tainted_relic",
                "knavesneeds:twilightforest/ironwood/claymore", "knavesneeds:twilightforest/steeleaf/claymore",
                "knavesneeds:twilightforest/knightmetal/claymore", "knavesneeds:twilightforest/fiery/claymore"
        ));

        // 3. 斧类 (Axes / Battle Axes)
        writeTag(new File(tagDir, "axes.json"), List.of(
                // Minecraft 原版斧
                "minecraft:wooden_axe", "minecraft:stone_axe", "minecraft:iron_axe",
                "minecraft:golden_axe", "minecraft:diamond_axe", "minecraft:netherite_axe",
                // Cataclysm 基础斧
                "cataclysm:black_steel_axe",
                // Twilight Delight 战斧牛排
                "twilightdelight:raw_tomahawk_smeaxe", "twilightdelight:cooked_tomahawk_smeaxe",
                // L2Complements
                "l2complements:eternium_axe", "l2complements:poseidite_axe", "l2complements:sculkium_axe",
                "l2complements:shulkerate_axe", "l2complements:totemic_gold_axe",
                // L2Weaponry 战斧
                "l2weaponry:ancient_metal_battle_axe", "l2weaponry:black_steel_battle_axe", "l2weaponry:cursium_battle_axe",
                "l2weaponry:diamond_battle_axe", "l2weaponry:eternium_battle_axe", "l2weaponry:fiery_battle_axe",
                "l2weaponry:gold_battle_axe", "l2weaponry:ignitium_battle_axe", "l2weaponry:iron_battle_axe",
                "l2weaponry:ironwood_battle_axe", "l2weaponry:knightmetal_battle_axe", "l2weaponry:netherite_battle_axe",
                "l2weaponry:poseidite_battle_axe", "l2weaponry:sculkium_battle_axe", "l2weaponry:shulkerate_battle_axe",
                "l2weaponry:steeleaf_battle_axe", "l2weaponry:totemic_gold_battle_axe", "l2weaponry:witherite_battle_axe",
                // L2Weaponry 短柄飞斧
                "l2weaponry:ancient_metal_throwing_axe", "l2weaponry:black_steel_throwing_axe", "l2weaponry:cursium_throwing_axe",
                "l2weaponry:diamond_throwing_axe", "l2weaponry:eternium_throwing_axe", "l2weaponry:fiery_throwing_axe",
                "l2weaponry:gold_throwing_axe", "l2weaponry:ignitium_throwing_axe", "l2weaponry:iron_throwing_axe",
                "l2weaponry:ironwood_throwing_axe", "l2weaponry:knightmetal_throwing_axe", "l2weaponry:netherite_throwing_axe",
                "l2weaponry:poseidite_throwing_axe", "l2weaponry:sculkium_throwing_axe", "l2weaponry:shulkerate_throwing_axe",
                "l2weaponry:steeleaf_throwing_axe", "l2weaponry:totemic_gold_throwing_axe", "l2weaponry:witherite_throwing_axe",
                // Simply Swords 巨斧
                "simplyswords:iron_greataxe", "simplyswords:gold_greataxe", "simplyswords:diamond_greataxe",
                "simplyswords:netherite_greataxe", "simplyswords:runic_greataxe",
                // KnavesNeeds 巨斧
                "knavesneeds:twilightforest/ironwood/greataxe", "knavesneeds:twilightforest/steeleaf/greataxe",
                "knavesneeds:twilightforest/knightmetal/greataxe", "knavesneeds:twilightforest/fiery/greataxe",
                // 暮色森林 斧
                "twilightforest:ironwood_axe", "twilightforest:knightmetal_axe", "twilightforest:steeleaf_axe",
                "twilightforest:diamond_minotaur_axe", "twilightforest:gold_minotaur_axe"
        ));

        // 4. 钝击武器 / 战锤 / 煎锅 (Blunt / Hammers / Maces)
        writeTag(new File(tagDir, "blunt.json"), List.of(
                // 原版 & 农夫乐事
                "minecraft:mace", "farmersdelight:skillet",
                // L2Weaponry 战锤
                "l2weaponry:iron_hammer", "l2weaponry:gold_hammer", "l2weaponry:diamond_hammer",
                "l2weaponry:netherite_hammer", "l2weaponry:ironwood_hammer", "l2weaponry:knightmetal_hammer",
                "l2weaponry:steeleaf_hammer", "l2weaponry:fiery_hammer", "l2weaponry:totemic_gold_hammer",
                "l2weaponry:eternium_hammer", "l2weaponry:poseidite_hammer", "l2weaponry:sculkium_hammer",
                "l2weaponry:shulkerate_hammer",
                // L2Weaponry 双节棍
                "l2weaponry:ancient_metal_nunchaku", "l2weaponry:black_steel_nunchaku", "l2weaponry:cursium_nunchaku",
                "l2weaponry:diamond_nunchaku", "l2weaponry:eternium_nunchaku", "l2weaponry:fiery_nunchaku",
                "l2weaponry:gold_nunchaku", "l2weaponry:ignitium_nunchaku", "l2weaponry:iron_nunchaku",
                "l2weaponry:ironwood_nunchaku", "l2weaponry:knightmetal_nunchaku", "l2weaponry:netherite_nunchaku",
                "l2weaponry:poseidite_nunchaku", "l2weaponry:sculkium_nunchaku", "l2weaponry:shulkerate_nunchaku",
                "l2weaponry:steeleaf_nunchaku", "l2weaponry:totemic_gold_nunchaku", "l2weaponry:witherite_nunchaku",
                // Simply Swords 巨锤
                "simplyswords:iron_greathammer", "simplyswords:gold_greathammer", "simplyswords:diamond_greathammer",
                "simplyswords:netherite_greathammer", "simplyswords:runic_greathammer",
                // KnavesNeeds 巨锤
                "knavesneeds:twilightforest/ironwood/greathammer", "knavesneeds:twilightforest/steeleaf/greathammer",
                "knavesneeds:twilightforest/knightmetal/greathammer", "knavesneeds:twilightforest/fiery/greathammer"
        ));

        // 5. 长柄 / 长枪 / 标枪 / 戟 / 镰刀 (Polearms & Spears & Scythes & Javelins)
        writeTag(new File(tagDir, "polearms_spears.json"), List.of(
                // 原版 & 莫西 & 铁魔法 & 灾变
                "minecraft:trident", "mowziesmobs:spear", "irons_spellbooks:decrepit_scythe",
                "cataclysm:coral_bardiche", "cataclysm:coral_spear",
                // L2Weaponry 标枪 (Javelins)
                "l2weaponry:ancient_metal_javelin", "l2weaponry:black_steel_javelin", "l2weaponry:cursium_javelin",
                "l2weaponry:diamond_javelin", "l2weaponry:eternium_javelin", "l2weaponry:fiery_javelin",
                "l2weaponry:gold_javelin", "l2weaponry:ignitium_javelin", "l2weaponry:iron_javelin",
                "l2weaponry:ironwood_javelin", "l2weaponry:knightmetal_javelin", "l2weaponry:netherite_javelin",
                "l2weaponry:poseidite_javelin", "l2weaponry:sculkium_javelin", "l2weaponry:shulkerate_javelin",
                "l2weaponry:steeleaf_javelin", "l2weaponry:totemic_gold_javelin", "l2weaponry:witherite_javelin",
                // L2Weaponry 长枪 (Spears)
                "l2weaponry:ancient_metal_spear", "l2weaponry:black_steel_spear", "l2weaponry:cursium_spear",
                "l2weaponry:diamond_spear", "l2weaponry:eternium_spear", "l2weaponry:fiery_spear",
                "l2weaponry:gold_spear", "l2weaponry:ignitium_spear", "l2weaponry:iron_spear",
                "l2weaponry:ironwood_spear", "l2weaponry:knightmetal_spear", "l2weaponry:netherite_spear",
                "l2weaponry:poseidite_spear", "l2weaponry:sculkium_spear", "l2weaponry:shulkerate_spear",
                "l2weaponry:steeleaf_spear", "l2weaponry:totemic_gold_spear", "l2weaponry:witherite_spear",
                // L2Weaponry 战镰 (Scythes)
                "l2weaponry:ancient_metal_scythe", "l2weaponry:black_steel_scythe", "l2weaponry:cursium_scythe",
                "l2weaponry:diamond_scythe", "l2weaponry:eternium_scythe", "l2weaponry:fiery_scythe",
                "l2weaponry:gold_scythe", "l2weaponry:ignitium_scythe", "l2weaponry:iron_scythe",
                "l2weaponry:ironwood_scythe", "l2weaponry:knightmetal_scythe", "l2weaponry:netherite_scythe",
                "l2weaponry:poseidite_scythe", "l2weaponry:sculkium_scythe", "l2weaponry:shulkerate_scythe",
                "l2weaponry:steeleaf_scythe", "l2weaponry:totemic_gold_scythe", "l2weaponry:witherite_scythe",
                // Simply Swords (长矛、长柄刀、戟、镰刀)
                "simplyswords:iron_spear", "simplyswords:iron_glaive", "simplyswords:iron_halberd", "simplyswords:iron_scythe",
                "simplyswords:gold_spear", "simplyswords:gold_glaive", "simplyswords:gold_halberd", "simplyswords:gold_scythe",
                "simplyswords:diamond_spear", "simplyswords:diamond_glaive", "simplyswords:diamond_halberd", "simplyswords:diamond_scythe",
                "simplyswords:netherite_spear", "simplyswords:netherite_glaive", "simplyswords:netherite_halberd", "simplyswords:netherite_scythe",
                "simplyswords:runic_spear", "simplyswords:runic_glaive", "simplyswords:runic_halberd", "simplyswords:runic_scythe",
                // KnavesNeeds (长矛、长柄刀、戟、镰刀)
                "knavesneeds:twilightforest/ironwood/spear", "knavesneeds:twilightforest/ironwood/glaive", "knavesneeds:twilightforest/ironwood/halberd", "knavesneeds:twilightforest/ironwood/scythe",
                "knavesneeds:twilightforest/steeleaf/spear", "knavesneeds:twilightforest/steeleaf/glaive", "knavesneeds:twilightforest/steeleaf/halberd", "knavesneeds:twilightforest/steeleaf/scythe",
                "knavesneeds:twilightforest/knightmetal/spear", "knavesneeds:twilightforest/knightmetal/glaive", "knavesneeds:twilightforest/knightmetal/halberd", "knavesneeds:twilightforest/knightmetal/scythe",
                "knavesneeds:twilightforest/fiery/spear", "knavesneeds:twilightforest/fiery/glaive", "knavesneeds:twilightforest/fiery/halberd", "knavesneeds:twilightforest/fiery/scythe"
        ));

        // 6. 匕首 / 刀具 / 刺 (Daggers & Knives & Sais)
        writeTag(new File(tagDir, "daggers.json"), List.of(
                // Farmer's Delight 刀具
                "farmersdelight:flint_knife", "farmersdelight:iron_knife", "farmersdelight:golden_knife",
                "farmersdelight:diamond_knife", "farmersdelight:netherite_knife",
                // Twilight Delight 刀具
                "twilightdelight:ironwood_knife", "twilightdelight:knightmetal_knife", "twilightdelight:steeleaf_knife",
                "twilightdelight:fiery_knife", "twilightdelight:ice_knife",
                // L2Weaponry 匕首
                "l2weaponry:ancient_metal_dagger", "l2weaponry:black_steel_dagger", "l2weaponry:cursium_dagger",
                "l2weaponry:diamond_dagger", "l2weaponry:eternium_dagger", "l2weaponry:fiery_dagger",
                "l2weaponry:gold_dagger", "l2weaponry:ignitium_dagger", "l2weaponry:iron_dagger",
                "l2weaponry:ironwood_dagger", "l2weaponry:knightmetal_dagger", "l2weaponry:netherite_dagger",
                "l2weaponry:poseidite_dagger", "l2weaponry:sculkium_dagger", "l2weaponry:shulkerate_dagger",
                "l2weaponry:steeleaf_dagger", "l2weaponry:totemic_gold_dagger", "l2weaponry:witherite_dagger",
                // Simply Swords 三叉刺
                "simplyswords:iron_sai", "simplyswords:gold_sai", "simplyswords:diamond_sai",
                "simplyswords:netherite_sai", "simplyswords:runic_sai",
                // KnavesNeeds 三叉刺
                "knavesneeds:twilightforest/ironwood/sai", "knavesneeds:twilightforest/steeleaf/sai",
                "knavesneeds:twilightforest/knightmetal/sai", "knavesneeds:twilightforest/fiery/sai"
        ));

        // 7. 刃爪类 (Claws)
        writeTag(new File(tagDir, "claws.json"), List.of(
                "twilightdelight:helmet_crab_claw",
                "l2weaponry:ancient_metal_claw", "l2weaponry:black_steel_claw", "l2weaponry:cursium_claw",
                "l2weaponry:diamond_claw", "l2weaponry:eternium_claw", "l2weaponry:fiery_claw",
                "l2weaponry:gold_claw", "l2weaponry:ignitium_claw", "l2weaponry:iron_claw",
                "l2weaponry:ironwood_claw", "l2weaponry:knightmetal_claw", "l2weaponry:netherite_claw",
                "l2weaponry:poseidite_claw", "l2weaponry:sculkium_claw", "l2weaponry:shulkerate_claw",
                "l2weaponry:steeleaf_claw", "l2weaponry:totemic_gold_claw", "l2weaponry:witherite_claw"
        ));

        // 8. 法杖类 (Staves)
        writeTag(new File(tagDir, "staves.json"), List.of(
                "irons_spellbooks:graybeard_staff", "irons_spellbooks:artificer_cane", "irons_spellbooks:ice_staff",
                "irons_spellbooks:lightning_rod", "irons_spellbooks:blood_staff", "irons_spellbooks:pyrium_staff"
        ));

        // 9. 弓弩类 (Bows & Crossbows)
        writeTag(new File(tagDir, "bows.json"), List.of(
                // 原版 & 地牢
                "minecraft:bow", "minecraft:crossbow", "dungeonnowloading:vertex_bow",
                // Iron's Spells & 东方魔法
                "irons_spellbooks:autoloader_crossbow", "iss_magicfromtheeast:repeating_crossbow",
                // 暮色森林 & 暮色乐事
                "twilightforest:ice_bow", "twilightforest:ender_bow", "twilightforest:seeker_bow",
                "twilightforest:triple_bow", "twilightdelight:antler_bow",
                // L2Archery 弓箭体系
                "l2archery:starter_bow", "l2archery:iron_bow", "l2archery:master_bow",
                "l2archery:eagle_bow", "l2archery:earth_bow", "l2archery:ender_aim_bow",
                "l2archery:explosion_bow", "l2archery:flame_bow", "l2archery:froze_bow",
                "l2archery:gaia_bow", "l2archery:glow_aim_bow", "l2archery:magnify_bow",
                "l2archery:slow_bow", "l2archery:storm_bow", "l2archery:sun_bow",
                "l2archery:turtle_bow", "l2archery:wind_bow", "l2archery:winter_bow"
        ));

        // 10. 盾牌 (Shields - 补全全部 L2Weaponry 圆盾 + Cataclysm + ExampleMod)
        writeTag(new File(tagDir, "shields.json"), List.of(
                "minecraft:shield",
                "examplemod:turtle_shield",
                "twilightforest:knightmetal_shield",
                "cataclysm:azure_sea_shield", "cataclysm:black_steel_targe",
                // L2Weaponry 重盾
                "l2weaponry:iron_plate_shield", "l2weaponry:gold_plate_shield", "l2weaponry:diamond_plate_shield", "l2weaponry:netherite_plate_shield",
                "l2weaponry:ironwood_plate_shield", "l2weaponry:knightmetal_plate_shield", "l2weaponry:steeleaf_plate_shield", "l2weaponry:fiery_plate_shield",
                "l2weaponry:totemic_gold_plate_shield", "l2weaponry:eternium_plate_shield", "l2weaponry:poseidite_plate_shield", "l2weaponry:sculkium_plate_shield",
                "l2weaponry:shulkerate_plate_shield",
                // L2Weaponry 圆盾
                "l2weaponry:iron_round_shield", "l2weaponry:gold_round_shield", "l2weaponry:diamond_round_shield", "l2weaponry:netherite_round_shield",
                "l2weaponry:ironwood_round_shield", "l2weaponry:knightmetal_round_shield", "l2weaponry:steeleaf_round_shield", "l2weaponry:fiery_round_shield",
                "l2weaponry:totemic_gold_round_shield", "l2weaponry:eternium_round_shield", "l2weaponry:poseidite_round_shield", "l2weaponry:sculkium_round_shield",
                "l2weaponry:shulkerate_round_shield"
        ));

        // 11. 防具（头盔 / 胸甲 / 护腿 / 靴子）
        writeTag(new File(tagDir, "helmets.json"), List.of(
                "minecraft:leather_helmet", "minecraft:chainmail_helmet", "minecraft:iron_helmet",
                "minecraft:golden_helmet", "minecraft:diamond_helmet", "minecraft:netherite_helmet", "minecraft:turtle_helmet",
                "cataclysm:bone_reptile_helmet", "cataclysm:cursium_helmet", "cataclysm:ignitium_helmet",
                "dungeonnowloading:spawner_helmet",
                "endermanoverhaul:badlands_hood", "endermanoverhaul:savanna_hood", "endermanoverhaul:snowy_hood",
                "irons_spellbooks:archevoker_helmet", "irons_spellbooks:cryomancer_helmet", "irons_spellbooks:cultist_helmet",
                "irons_spellbooks:electromancer_helmet", "irons_spellbooks:netherite_mage_helmet", "irons_spellbooks:plagued_helmet",
                "irons_spellbooks:priest_helmet", "irons_spellbooks:pumpkin_helmet", "irons_spellbooks:pyromancer_helmet",
                "irons_spellbooks:shadowwalker_helmet", "irons_spellbooks:tarnished_helmet", "irons_spellbooks:wandering_magician_helmet",
                "irons_spellbooks:wizard_hat", "irons_spellbooks:wizard_helmet",
                "iss_magicfromtheeast:jade_pagoda_helmet", "iss_magicfromtheeast:jiangshi_hat",
                "iss_magicfromtheeast:onmyoji_helmet", "iss_magicfromtheeast:taoist_helmet",
                "l2complements:eternium_helmet", "l2complements:poseidite_helmet", "l2complements:sculkium_helmet",
                "l2complements:shulkerate_helmet", "l2complements:totemic_gold_helmet",
                "mowziesmobs:geomancer_beads", "mowziesmobs:umvuthana_mask_bliss", "mowziesmobs:umvuthana_mask_faith",
                "mowziesmobs:umvuthana_mask_fear", "mowziesmobs:umvuthana_mask_fury", "mowziesmobs:umvuthana_mask_misery", "mowziesmobs:umvuthana_mask_rage",
                "tunes_n_tomes:bard_helmet", "tunes_n_tomes:melodic_mastery_helmet",
                "twilightforest:arctic_helmet", "twilightforest:fiery_helmet", "twilightforest:ironwood_helmet",
                "twilightforest:knightmetal_helmet", "twilightforest:phantom_helmet", "twilightforest:steeleaf_helmet",
                "twilightforest:travellers_goggles", "twilightforest:yeti_helmet"
        ));

        writeTag(new File(tagDir, "chestplates.json"), List.of(
                "minecraft:leather_chestplate", "minecraft:chainmail_chestplate", "minecraft:iron_chestplate",
                "minecraft:golden_chestplate", "minecraft:diamond_chestplate", "minecraft:netherite_chestplate",
                "cataclysm:bone_reptile_chestplate", "cataclysm:cursium_chestplate",
                "cataclysm:ignitium_chestplate", "cataclysm:ignitium_elytra_chestplate",
                "dungeonnowloading:spawner_chestplate",
                "irons_spellbooks:archevoker_chestplate", "irons_spellbooks:cryomancer_chestplate", "irons_spellbooks:cultist_chestplate",
                "irons_spellbooks:electromancer_chestplate", "irons_spellbooks:netherite_mage_chestplate", "irons_spellbooks:plagued_chestplate",
                "irons_spellbooks:priest_chestplate", "irons_spellbooks:pumpkin_chestplate", "irons_spellbooks:pyromancer_chestplate",
                "irons_spellbooks:shadowwalker_chestplate", "irons_spellbooks:wandering_magician_chestplate", "irons_spellbooks:wizard_chestplate",
                "iss_magicfromtheeast:jade_pagoda_chestplate", "iss_magicfromtheeast:onmyoji_chestplate", "iss_magicfromtheeast:taoist_chestplate",
                "l2complements:eternium_chestplate", "l2complements:poseidite_chestplate", "l2complements:sculkium_chestplate",
                "l2complements:shulkerate_chestplate", "l2complements:totemic_gold_chestplate",
                "mowziesmobs:geomancer_robe",
                "tunes_n_tomes:bard_chestplate", "tunes_n_tomes:melodic_mastery_chestplate",
                "twilightforest:arctic_chestplate", "twilightforest:fiery_chestplate", "twilightforest:ironwood_chestplate",
                "twilightforest:knightmetal_chestplate", "twilightforest:naga_chestplate", "twilightforest:phantom_chestplate",
                "twilightforest:steeleaf_chestplate", "twilightforest:travellers_vest", "twilightforest:yeti_chestplate"
        ));

        writeTag(new File(tagDir, "leggings.json"), List.of(
                "minecraft:leather_leggings", "minecraft:chainmail_leggings", "minecraft:iron_leggings",
                "minecraft:golden_leggings", "minecraft:diamond_leggings", "minecraft:netherite_leggings",
                "cataclysm:cursium_leggings", "cataclysm:ignitium_leggings",
                "dungeonnowloading:spawner_leggings",
                "irons_spellbooks:archevoker_leggings", "irons_spellbooks:cryomancer_leggings", "irons_spellbooks:cultist_leggings",
                "irons_spellbooks:electromancer_leggings", "irons_spellbooks:netherite_mage_leggings", "irons_spellbooks:plagued_leggings",
                "irons_spellbooks:priest_leggings", "irons_spellbooks:pumpkin_leggings", "irons_spellbooks:pyromancer_leggings",
                "irons_spellbooks:shadowwalker_leggings", "irons_spellbooks:wandering_magician_leggings", "irons_spellbooks:wizard_leggings",
                "iss_magicfromtheeast:jade_pagoda_leggings", "iss_magicfromtheeast:onmyoji_leggings", "iss_magicfromtheeast:taoist_leggings",
                "l2complements:eternium_leggings", "l2complements:poseidite_leggings", "l2complements:sculkium_leggings",
                "l2complements:shulkerate_leggings", "l2complements:totemic_gold_leggings",
                "mowziesmobs:geomancer_belt",
                "tunes_n_tomes:bard_leggings", "tunes_n_tomes:melodic_mastery_leggings",
                "twilightforest:arctic_leggings", "twilightforest:fiery_leggings", "twilightforest:ironwood_leggings",
                "twilightforest:knightmetal_leggings", "twilightforest:naga_leggings", "twilightforest:steeleaf_leggings",
                "twilightforest:travellers_wings", "twilightforest:yeti_leggings"
        ));

        writeTag(new File(tagDir, "boots.json"), List.of(
                "minecraft:leather_boots", "minecraft:chainmail_boots", "minecraft:iron_boots",
                "minecraft:golden_boots", "minecraft:diamond_boots", "minecraft:netherite_boots",
                "cataclysm:cursium_boots", "cataclysm:ignitium_boots",
                "dungeonnowloading:spawner_boots",
                "irons_spellbooks:archevoker_boots", "irons_spellbooks:cryomancer_boots", "irons_spellbooks:cultist_boots",
                "irons_spellbooks:electromancer_boots", "irons_spellbooks:netherite_mage_boots", "irons_spellbooks:plagued_boots",
                "irons_spellbooks:priest_boots", "irons_spellbooks:pumpkin_boots", "irons_spellbooks:pyromancer_boots",
                "irons_spellbooks:shadowwalker_boots", "irons_spellbooks:wandering_magician_boots", "irons_spellbooks:wizard_boots",
                "iss_magicfromtheeast:jade_pagoda_boots", "iss_magicfromtheeast:onmyoji_boots", "iss_magicfromtheeast:taoist_boots",
                "l2complements:eternium_boots", "l2complements:poseidite_boots", "l2complements:sculkium_boots",
                "l2complements:shulkerate_boots", "l2complements:totemic_gold_boots",
                "mowziesmobs:geomancer_sandals",
                "tunes_n_tomes:bard_boots", "tunes_n_tomes:melodic_mastery_boots",
                "twilightforest:arctic_boots", "twilightforest:fiery_boots", "twilightforest:ironwood_boots",
                "twilightforest:knightmetal_boots", "twilightforest:steeleaf_boots",
                "twilightforest:travellers_boots", "twilightforest:yeti_boots"
        ));

        // 12. 聚合标签 (Melee / All Weapons / All)
        writeTag(new File(tagDir, "melee.json"), List.of(
                "#examplemod:base/swords",
                "#examplemod:base/axes",
                "#examplemod:base/blunt",
                "#examplemod:base/polearms_spears",
                "#examplemod:base/daggers",
                "#examplemod:base/claws",
                "#examplemod:base/two_handed_swords"
        ));

        writeTag(new File(tagDir, "all_weapons.json"), List.of(
                "#examplemod:base/melee",
                "#examplemod:base/staves",
                "#examplemod:base/bows"
        ));

        writeTag(new File(tagDir, "all.json"), List.of(
                "#examplemod:base/all_weapons",
                "#examplemod:base/helmets",
                "#examplemod:base/chestplates",
                "#examplemod:base/leggings",
                "#examplemod:base/boots",
                "#examplemod:base/shields"
        ));
    }
    private static void writeTag(File file, List<String> entries) {
        StringBuilder sb = new StringBuilder("{\n  \"replace\": false,\n  \"values\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            sb.append("    \"").append(entries.get(i)).append("\"");
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        writeFile(file, sb.toString());
    }

    private static void writeFile(File file, String content) {
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteDirectory(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }

    private static List<Runeword> buildAllRunewords() {
        List<Runeword> list = new ArrayList<>();

        // ==================== 一、头盔 (Helms - 9个) ====================
        list.add(new Runeword("nadir", "Nadir", "天底", 0,
                List.of("#examplemod:base/helmets"), List.of(4, 3),
                List.of(new AttributeMod("minecraft:generic.armor", 3.0, "ADD_VALUE", "+3 护甲"),
                        new AttributeMod("minecraft:generic.knockback_resistance", 0.15, "ADD_VALUE", "+15% 防击退"),
                        new AttributeMod("examplemod:mana_on_kill", 5.0, "ADD_VALUE", "+5 击杀回蓝(x4)"),
                        new AttributeMod("apothic_attributes:experience_gained", -0.20, "MULTIPLY_BASE", "-20% 经验获取"))));

        list.add(new Runeword("radiance", "Radiance", "光辉", 0,
                List.of("#examplemod:base/helmets"), List.of(4, 12, 6),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 2.0, "ADD_VALUE", "+2 物理增伤"),
                        new AttributeMod("minecraft:generic.armor", 4.0, "ADD_VALUE", "+4 护甲"),
                        new AttributeMod("irons_spellbooks:max_mana", 60.0, "ADD_VALUE", "+60 法力上限"),
                        new AttributeMod("examplemod:damage_to_mana", 15.0, "ADD_VALUE", "15% 受损生命转法力"),
                        new AttributeMod("l2damagetracker:damage_absorption", 4.0, "ADD_VALUE", "+4 固定减伤"))));

        list.add(new Runeword("lore", "Lore", "学识", 1,
                List.of("#examplemod:base/helmets"), List.of(9, 12),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.10, "MULTIPLY_BASE", "+10% 法强"),
                        new AttributeMod("l2damagetracker:damage_absorption", 4.0, "ADD_VALUE", "+4 固定减伤"),
                        new AttributeMod("irons_spellbooks:lightning_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 雷抗"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("deception", "Deception", "欺瞒", 0,
                List.of("#examplemod:base/helmets"), List.of(19, 11, 15),
                List.of(new AttributeMod("apothic_attributes:crit_damage", 0.25, "MULTIPLY_BASE", "+25% 暴击伤害"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.10, "ADD_VALUE", "+10% 暴击率"),
                        new AttributeMod("minecraft:generic.armor", 5.0, "ADD_VALUE", "+5 护甲"),
                        new AttributeMod("minecraft:generic.max_health", -4.0, "ADD_VALUE", "-4 生命惩罚"))));

        list.add(new Runeword("wisdom", "Wisdom", "智慧", 0,
                List.of("#examplemod:base/helmets"), List.of(21, 6, 2),
                List.of(new AttributeMod("apothic_attributes:arrow_damage", 0.30, "MULTIPLY_BASE", "+30% 箭矢伤害"),
                        new AttributeMod("apothic_attributes:projectile_damage", 0.20, "MULTIPLY_BASE", "+20% 投射物穿透"),
                        new AttributeMod("examplemod:mana_steal", 6.0, "ADD_VALUE", "6% 击中偷蓝"),
                        new AttributeMod("examplemod:mana_on_kill", 5.0, "ADD_VALUE", "+5 击杀回蓝(x4)"),
                        new AttributeMod("examplemod:damage_to_mana", 15.0, "ADD_VALUE", "15% 受损生命转法力"),
                        new AttributeMod("examplemod:cannot_be_frozen", 1.0, "ADD_VALUE", "无法冰冻"))));

        list.add(new Runeword("delirium", "Delirium", "谵妄", 2,
                List.of("#examplemod:base/helmets"), List.of(20, 24, 16),
                List.of(new AttributeMod("minecraft:generic.armor", 7.0, "ADD_VALUE", "+7 护甲"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.18, "ADD_VALUE", "+18% 闪避"),
                        new AttributeMod("minecraft:generic.luck", 2.0, "ADD_VALUE", "+2 幸运(寻宝)"),
                        new AttributeMod("apothic_attributes:experience_gained", 0.45, "MULTIPLY_BASE", "+45% 经验获取"))));

        list.add(new Runeword("flickering_flame", "Flickering Flame", "摇曳焰火", 0,
                List.of("#examplemod:base/helmets"), List.of(4, 21, 26),
                List.of(new AttributeMod("irons_spellbooks:fire_spell_power", 0.40, "MULTIPLY_BASE", "+40% 火焰法强"),
                        new AttributeMod("examplemod:fire_penetration", 10.0, "ADD_VALUE", "-10% 敌人火抗"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.60, "MULTIPLY_BASE", "+60% 抗火光环"),
                        new AttributeMod("irons_spellbooks:max_mana", 75.0, "ADD_VALUE", "+75 法力上限"))));

        list.add(new Runeword("dream_helm", "Dream (Helm)", "梦境 (头盔)", 0,
                List.of("#examplemod:base/helmets"), List.of(16, 31, 21),
                List.of(new AttributeMod("examplemod:holy_shock_aura", 1.0, "ADD_VALUE", "神圣冲击雷暴灵气"),
                        new AttributeMod("irons_spellbooks:lightning_spell_power", 0.35, "MULTIPLY_BASE", "+35% 闪电法强"),
                        new AttributeMod("minecraft:generic.max_health", 0.05, "MULTIPLY_BASE", "+5% 生命上限"),
                        new AttributeMod("irons_spellbooks:max_mana", 60.0, "ADD_VALUE", "+60 法力上限"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.20, "MULTIPLY_BASE", "+20% 全抗"),
                        new AttributeMod("minecraft:generic.luck", 2.0, "ADD_VALUE", "+2 幸运"))));

        list.add(new Runeword("ferocity", "Ferocity", "凶猛", 0,
                List.of("#examplemod:base/helmets"), List.of(33, 32, 13),
                List.of(new AttributeMod("apothic_attributes:life_steal", 0.12, "ADD_VALUE", "12% 生命偷取"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻击速度"),
                        new AttributeMod("l2damagetracker:damage_absorption", 6.0, "ADD_VALUE", "+6 固定减伤"),
                        new AttributeMod("examplemod:cannot_be_frozen", 1.0, "ADD_VALUE", "无法冰冻"),
                        new AttributeMod("examplemod:thorns_damage", 15.0, "ADD_VALUE", "攻击者受到反伤"))));

        // ==================== 二、胸甲 (Chests - 23个) ====================
        list.add(new Runeword("stealth", "Stealth", "隐秘", 0,
                List.of("#examplemod:base/chestplates"), List.of(7, 5),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.12, "MULTIPLY_BASE", "+12% 跑速"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.20, "MULTIPLY_BASE", "+20% 快速施法"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.15, "MULTIPLY_BASE", "+15% 回蓝"),
                        new AttributeMod("irons_spellbooks:nature_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 毒抗"),
                        new AttributeMod("l2damagetracker:damage_absorption", 2.0, "ADD_VALUE", "+2 固定减伤"))));

        list.add(new Runeword("peace", "Peace", "和平", 0,
                List.of("#examplemod:base/chestplates"), List.of(13, 10, 11),
                List.of(new AttributeMod("apothic_attributes:crit_chance", 0.15, "ADD_VALUE", "+15% 致命一击"),
                        new AttributeMod("irons_spellbooks:summon_damage", 0.30, "MULTIPLY_BASE", "+30% 召唤物伤害"),
                        new AttributeMod("twilightforest:minion_count", 1.0, "ADD_VALUE", "+1 最大召唤数"),
                        new AttributeMod("irons_spellbooks:ice_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 冰抗"),
                        new AttributeMod("examplemod:thorns_damage", 8.0, "ADD_VALUE", "攻击者受到反伤"))));

        list.add(new Runeword("myth", "Myth", "神话", 0,
                List.of("#examplemod:base/chestplates"), List.of(15, 11, 4),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 3.0, "ADD_VALUE", "+3 攻击力"),
                        new AttributeMod("examplemod:mana_on_kill", 4.0, "ADD_VALUE", "+4 击杀回蓝(x4)"),
                        new AttributeMod("l2damagetracker:regen", 0.20, "MULTIPLY_BASE", "+20% 生命回复"),
                        new AttributeMod("examplemod:thorns_damage", 10.0, "ADD_VALUE", "攻击者受到反伤"))));

        list.add(new Runeword("finesse", "Finesse", "灵巧", 0,
                List.of("#examplemod:base/chestplates"), List.of(4, 8, 14),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.20, "MULTIPLY_BASE", "+20% 攻速"),
                        new AttributeMod("apothic_attributes:projectile_damage", 0.25, "MULTIPLY_BASE", "+25% 穿透伤害"),
                        new AttributeMod("l2damagetracker:regen", 0.25, "MULTIPLY_BASE", "+25% 生命回复"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 火抗"))));

        list.add(new Runeword("smoke", "Smoke", "烟雾", 0,
                List.of("#examplemod:base/chestplates"), List.of(4, 17),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.45, "MULTIPLY_BASE", "+45% 全抗"),
                        new AttributeMod("minecraft:generic.armor", 6.0, "ADD_VALUE", "+6 护甲"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.10, "ADD_VALUE", "+10% 闪避"))));

        list.add(new Runeword("hustle", "Hustle", "奔忙", 0,
                List.of("#examplemod:base/chestplates"), List.of(13, 18, 2),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.30, "MULTIPLY_BASE", "+30% 速度爆发"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.20, "MULTIPLY_BASE", "+20% 攻速"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.12, "ADD_VALUE", "+12% 闪避"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.10, "MULTIPLY_BASE", "+10% 全抗"))));

        list.add(new Runeword("lionheart", "Lionheart", "狮心", 0,
                List.of("#examplemod:base/chestplates"), List.of(15, 17, 19),
                List.of(new AttributeMod("minecraft:generic.max_health", 10.0, "ADD_VALUE", "+10 生命上限"),
                        new AttributeMod("minecraft:generic.attack_damage", 4.5, "ADD_VALUE", "+4.5 增强伤害"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.30, "MULTIPLY_BASE", "+30% 全抗"))));

        list.add(new Runeword("treachery", "Treachery", "背叛", 0,
                List.of("#examplemod:base/chestplates"), List.of(13, 10, 20),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.45, "MULTIPLY_BASE", "+45% 极速攻速"),
                        new AttributeMod("artifacts:generic.invincibility_ticks", 10.0, "ADD_VALUE", "延长无敌帧"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.10, "MULTIPLY_BASE", "+10% 减伤"),
                        new AttributeMod("irons_spellbooks:ice_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 冰抗"))));

        list.add(new Runeword("wealth", "Wealth", "财富", 0,
                List.of("#examplemod:base/chestplates"), List.of(20, 18, 3),
                List.of(new AttributeMod("minecraft:generic.luck", 4.0, "ADD_VALUE", "+4 极品寻宝幸运"),
                        new AttributeMod("examplemod:mana_on_kill", 4.0, "ADD_VALUE", "+4 击杀回蓝(x4)"),
                        new AttributeMod("apothic_attributes:experience_gained", 0.80, "MULTIPLY_BASE", "+80% 经验获取"),
                        new AttributeMod("l2hostility:extra_difficulty", 5.0, "ADD_VALUE", "高难高爆率"))));

        list.add(new Runeword("enlightenment", "Enlightenment", "启蒙", 0,
                List.of("#examplemod:base/chestplates"), List.of(21, 8, 12),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.20, "MULTIPLY_BASE", "+20% 法术强度"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.20, "MULTIPLY_BASE", "+20% 快速施法"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.35, "MULTIPLY_BASE", "+35% 回蓝"),
                        new AttributeMod("l2damagetracker:damage_absorption", 4.0, "ADD_VALUE", "+4 固定减伤"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 火抗"))));

        list.add(new Runeword("duress", "Duress", "强制", 0,
                List.of("#examplemod:base/chestplates"), List.of(13, 22, 10),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADD_VALUE", "15% 压碎打击"),
                        new AttributeMod("examplemod:open_wounds", 33.0, "ADD_VALUE", "33% 撕开伤口"),
                        new AttributeMod("minecraft:generic.attack_damage", 3.5, "ADD_VALUE", "+3.5 增强伤害"),
                        new AttributeMod("minecraft:generic.armor", 7.0, "ADD_VALUE", "+7 护甲"),
                        new AttributeMod("irons_spellbooks:ice_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 冰抗"))));

        list.add(new Runeword("stone", "Stone", "石块", 1,
                List.of("#examplemod:base/chestplates"), List.of(13, 22, 21, 17),
                List.of(new AttributeMod("minecraft:generic.armor", 14.0, "ADD_VALUE", "+14 钢铁重甲"),
                        new AttributeMod("minecraft:generic.armor_toughness", 8.0, "ADD_VALUE", "+8 盔甲韧性"),
                        new AttributeMod("minecraft:generic.knockback_resistance", 0.40, "ADD_VALUE", "+40% 防退"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.15, "MULTIPLY_BASE", "+15% 全抗"))));

        list.add(new Runeword("gloom", "Gloom", "幽暗", 0,
                List.of("#examplemod:base/chestplates"), List.of(19, 22, 21),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.45, "MULTIPLY_BASE", "+45% 全抗"),
                        new AttributeMod("minecraft:generic.armor", 9.0, "ADD_VALUE", "+9 护甲"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.15, "ADD_VALUE", "+15% 闪避"),
                        new AttributeMod("examplemod:damage_to_mana", 5.0, "ADD_VALUE", "5% 受损生命转法力"))));

        list.add(new Runeword("bone", "Bone", "白骨", 0,
                List.of("#examplemod:base/chestplates"), List.of(12, 22, 22),
                List.of(new AttributeMod("irons_spellbooks:eldritch_spell_power", 0.35, "MULTIPLY_BASE", "+35% 幽冥法强"),
                        new AttributeMod("minecraft:generic.max_absorption", 8.0, "ADD_VALUE", "骨甲吸收盾"),
                        new AttributeMod("irons_spellbooks:max_mana", 120.0, "ADD_VALUE", "+120 法力上限"),
                        new AttributeMod("l2damagetracker:damage_absorption", 4.0, "ADD_VALUE", "+4 固定减伤"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.30, "MULTIPLY_BASE", "+30% 全抗"))));

        list.add(new Runeword("prudence", "Prudence", "审慎", 0,
                List.of("#examplemod:base/chestplates"), List.of(23, 3),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.40, "MULTIPLY_BASE", "+40% 快速施法"),
                        new AttributeMod("minecraft:generic.armor", 8.0, "ADD_VALUE", "+8 护甲"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.35, "MULTIPLY_BASE", "+35% 全抗"),
                        new AttributeMod("l2damagetracker:damage_absorption", 6.0, "ADD_VALUE", "+6 固定减伤"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("rain", "Rain", "雨云", 0,
                List.of("#examplemod:base/chestplates"), List.of(9, 23, 6),
                List.of(new AttributeMod("irons_spellbooks:nature_spell_power", 0.35, "MULTIPLY_BASE", "+35% 自然法强"),
                        new AttributeMod("irons_spellbooks:max_mana", 130.0, "ADD_VALUE", "+130 法力上限"),
                        new AttributeMod("examplemod:damage_to_mana", 15.0, "ADD_VALUE", "15% 受损生命转法力"),
                        new AttributeMod("irons_spellbooks:lightning_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 雷抗"))));

        list.add(new Runeword("principle", "Principle", "原则", 0,
                List.of("#examplemod:base/chestplates"), List.of(8, 25, 2),
                List.of(new AttributeMod("irons_spellbooks:holy_spell_power", 0.35, "MULTIPLY_BASE", "+35% 神圣法强"),
                        new AttributeMod("examplemod:damage_to_demons", 125.0, "ADD_VALUE", "+125% 对恶魔伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 125.0, "ADD_VALUE", "+125% 对不死伤害"),
                        new AttributeMod("minecraft:generic.max_health", 8.0, "ADD_VALUE", "+8 生命上限"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 火抗"))));

        list.add(new Runeword("fortitude_armor", "Fortitude (Armor)", "刚毅 (防具)", 0,
                List.of("#examplemod:base/chestplates"), List.of(1, 12, 14, 28),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 8.0, "ADD_VALUE", "+8 物理增伤"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.25, "MULTIPLY_BASE", "+25% 快速施法"),
                        new AttributeMod("minecraft:generic.armor", 12.0, "ADD_VALUE", "+12 护甲"),
                        new AttributeMod("minecraft:generic.max_health", 15.0, "ADD_VALUE", "+15 生命"),
                        new AttributeMod("examplemod:damage_to_mana", 12.0, "ADD_VALUE", "12% 受损生命转法力"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.30, "MULTIPLY_BASE", "+30% 全抗"))));

        list.add(new Runeword("bramble", "Bramble", "荆棘", 0,
                List.of("#examplemod:base/chestplates"), List.of(8, 27, 29, 5),
                List.of(new AttributeMod("irons_spellbooks:nature_spell_power", 0.50, "MULTIPLY_BASE", "+50% 毒素/自然法强"),
                        new AttributeMod("examplemod:thorns_damage", 18.0, "ADD_VALUE", "荆棘灵气反伤"),
                        new AttributeMod("examplemod:life_on_kill", 2.0, "ADD_VALUE", "+2 击杀回血"),
                        new AttributeMod("irons_spellbooks:max_mana", 0.05, "MULTIPLY_BASE", "+5% 法力上限"),
                        new AttributeMod("irons_spellbooks:nature_magic_resist", 0.70, "MULTIPLY_BASE", "+70% 毒抗"))));

        list.add(new Runeword("dragon_armor", "Dragon (Armor)", "飞龙 (防具)", 0,
                List.of("#examplemod:base/chestplates"), List.of(29, 28, 12),
                List.of(new AttributeMod("examplemod:holy_fire_aura", 1.0, "ADD_VALUE", "圣火燃烧灵气"),
                        new AttributeMod("irons_spellbooks:fire_spell_power", 0.40, "MULTIPLY_BASE", "+40% 火焰法强"),
                        new AttributeMod("irons_spellbooks:max_mana", 0.05, "MULTIPLY_BASE", "+5% 法力上限"),
                        new AttributeMod("minecraft:generic.armor", 10.0, "ADD_VALUE", "+10 护甲"),
                        new AttributeMod("irons_spellbooks:lightning_magic_resist", 0.35, "MULTIPLY_BASE", "+35% 雷抗"))));

        list.add(new Runeword("chains_of_honor", "Chains of Honor", "荣耀之链", 2,
                List.of("#examplemod:base/chestplates"), List.of(14, 22, 30, 24),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.60, "MULTIPLY_BASE", "+60% 极高全抗"),
                        new AttributeMod("examplemod:damage_to_demons", 200.0, "ADD_VALUE", "+200% 对恶魔伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 200.0, "ADD_VALUE", "+200% 对不死伤害"),
                        new AttributeMod("apothic_attributes:life_steal", 0.10, "ADD_VALUE", "10% 生命偷取"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.06, "MULTIPLY_BASE", "+6% 减伤"),
                        new AttributeMod("minecraft:generic.luck", 2.0, "ADD_VALUE", "+2 幸运"))));

        list.add(new Runeword("enigma", "Enigma", "谜团", 2,
                List.of("#examplemod:base/chestplates"), List.of(31, 6, 30),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.25, "MULTIPLY_BASE", "+25% 跑速"),
                        new AttributeMod("irons_spellbooks:cooldown_reduction", 0.25, "MULTIPLY_BASE", "闪烁冷却缩减"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.06, "MULTIPLY_BASE", "+6% 物理硬减伤"),
                        new AttributeMod("minecraft:generic.max_health", 0.05, "MULTIPLY_BASE", "+5% 生命上限"),
                        new AttributeMod("examplemod:damage_to_mana", 15.0, "ADD_VALUE", "15% 受损生命转法力"),
                        new AttributeMod("minecraft:generic.luck", 3.0, "ADD_VALUE", "+3 幸运(MF)"))));

        list.add(new Runeword("innocence", "Innocence", "纯真", 2,
                List.of("#examplemod:base/chestplates"), List.of(25, 20, 33, 15),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.06, "ADD_VALUE", "+20% 压碎打击"),
                        new AttributeMod("minecraft:player.sweeping_damage_ratio", 0.50, "ADD_VALUE", "刃之盾群体切割"),
                        new AttributeMod("examplemod:thorns_damage", 30.0, "ADD_VALUE", "高额反伤"),
                        new AttributeMod("minecraft:generic.armor", 12.0, "ADD_VALUE", "+12 护甲"),
                        new AttributeMod("irons_spellbooks:nature_magic_resist", 0.45, "MULTIPLY_BASE", "+45% 毒抗"))));

        // ==================== 三、盾牌 (Shields - 11个) ====================
        list.add(new Runeword("ancients_pledge", "Ancients' Pledge", "古代人的誓约", 0,
                List.of("#examplemod:base/shields"), List.of(8, 9, 7),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.45, "MULTIPLY_BASE", "+45% 全抗"),
                        new AttributeMod("l2weaponry:shield_defense", 10.0, "ADD_VALUE", "+10 盾防"),
                        new AttributeMod("examplemod:damage_to_mana", 10.0, "ADD_VALUE", "10% 受损生命转法力"))));

        list.add(new Runeword("spirit_shield", "Spirit (Shield)", "精神 (盾牌)", 1,
                List.of("#examplemod:base/shields"), List.of(7, 10, 9, 11),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.30, "MULTIPLY_BASE", "+30% 快速施法"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.35, "MULTIPLY_BASE", "+35% 冰雷毒抗性"),
                        new AttributeMod("irons_spellbooks:max_mana", 75.0, "ADD_VALUE", "+75 法力上限"),
                        new AttributeMod("examplemod:thorns_damage", 10.0, "ADD_VALUE", "攻击者受到反伤"))));

        list.add(new Runeword("rhyme", "Rhyme", "韵律", 0,
                List.of("#examplemod:base/shields"), List.of(13, 5),
                List.of(new AttributeMod("l2weaponry:reflect_time", 4.0, "ADD_VALUE", "+40% 格挡提速"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.25, "MULTIPLY_BASE", "+25% 全抗"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.15, "MULTIPLY_BASE", "+15% 回蓝"),
                        new AttributeMod("examplemod:cannot_be_frozen", 1.0, "ADD_VALUE", "无法冰冻"),
                        new AttributeMod("minecraft:generic.luck", 1.5, "ADD_VALUE", "+1.5 幸运"))));

        list.add(new Runeword("splendor", "Splendor", "灿烂", 1,
                List.of("#examplemod:base/shields"), List.of(5, 17),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.25, "MULTIPLY_BASE", "+25% 快速施法"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.20, "MULTIPLY_BASE", "+20% 回蓝"),
                        new AttributeMod("l2weaponry:reflect_time", 2.0, "ADD_VALUE", "+20% 格挡提速"),
                        new AttributeMod("minecraft:generic.luck", 1.5, "ADD_VALUE", "+1.5 幸运"))));

        list.add(new Runeword("sanctuary", "Sanctuary", "圣堂", 0,
                List.of("#examplemod:base/shields"), List.of(18, 18, 23),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.45, "MULTIPLY_BASE", "+45% 全抗"),
                        new AttributeMod("l2weaponry:shield_defense", 16.0, "ADD_VALUE", "+16 盾防"),
                        new AttributeMod("examplemod:cannot_be_frozen", 1.0, "ADD_VALUE", "无法冰冻"),
                        new AttributeMod("l2damagetracker:damage_absorption", 4.0, "ADD_VALUE", "+4 固定减伤"))));

// 流亡 (Exile) - 盾牌
        list.add(new Runeword("exile", "Exile", "流亡", 0,
                List.of("#examplemod:base/shields"), List.of(26, 27, 24, 14),
                List.of(new AttributeMod("minecraft:generic.armor", 12.0, "ADD_VALUE", "反抗光环 +12 护甲"),
                        new AttributeMod("l2weaponry:shield_defense", 22.0, "ADD_VALUE", "+22 盾防"),
                        new AttributeMod("examplemod:freeze_target", 200.0, "ADD_VALUE", "+200 冰冷值"), // 原 1.0 改为 200.0
                        new AttributeMod("l2damagetracker:regen", 0.35, "MULTIPLY_BASE", "+35% 生命回复"))));

        list.add(new Runeword("epiphany", "Epiphany", "顿悟", 0,
                List.of("#examplemod:base/shields"), List.of(28, 6, 28, 24),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.25, "MULTIPLY_BASE", "+25% 快速施法"),
                        new AttributeMod("irons_spellbooks:lightning_spell_power", 0.40, "MULTIPLY_BASE", "+40% 雷电威力"),
                        new AttributeMod("minecraft:generic.max_absorption", 10.0, "ADD_VALUE", "能量护盾吸收"),
                        new AttributeMod("irons_spellbooks:max_mana", 0.30, "MULTIPLY_BASE", "+30% 法力上限"),
                        new AttributeMod("examplemod:damage_to_mana", 15.0, "ADD_VALUE", "15% 受损生命转法力"))));

        list.add(new Runeword("dragon_shield", "Dragon (Shield)", "飞龙 (盾牌)", 0,
                List.of("#examplemod:base/shields"), List.of(29, 28, 12),
                List.of(new AttributeMod("examplemod:holy_fire_aura", 1.0, "ADD_VALUE", "圣火燃烧灵气"),
                        new AttributeMod("irons_spellbooks:fire_spell_power", 0.35, "MULTIPLY_BASE", "+35% 火焰法强"),
                        new AttributeMod("irons_spellbooks:max_mana", 75.0, "ADD_VALUE", "+75 法力上限"),
                        new AttributeMod("l2damagetracker:damage_absorption", 5.0, "ADD_VALUE", "+5 固定减伤"))));

        list.add(new Runeword("dream_shield", "Dream (Shield)", "梦境 (盾牌)", 0,
                List.of("#examplemod:base/shields"), List.of(16, 31, 21),
                List.of(new AttributeMod("examplemod:holy_shock_aura", 1.0, "ADD_VALUE", "神圣冲击雷暴灵气"),
                        new AttributeMod("irons_spellbooks:lightning_spell_power", 0.35, "MULTIPLY_BASE", "+35% 闪电法强"),
                        new AttributeMod("minecraft:generic.max_health", 10.0, "ADD_VALUE", "+10 生命上限"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.20, "MULTIPLY_BASE", "+20% 全抗"))));

        list.add(new Runeword("phoenix_shield", "Phoenix (Shield)", "凤凰 (盾牌)", 0,
                List.of("#examplemod:base/shields"), List.of(26, 26, 28, 31),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 10.0, "ADD_VALUE", "+10 物理增伤"),
                        new AttributeMod("examplemod:fire_penetration", 28.0, "ADD_VALUE", "-28% 敌人火抗"),
                        new AttributeMod("apothic_attributes:healing_received", 0.40, "MULTIPLY_BASE", "救赎光环回复"),
                        new AttributeMod("minecraft:generic.max_health", 10.0, "ADD_VALUE", "+10 生命"))));

        list.add(new Runeword("shattered_wall", "Shattered Wall", "碎裂之墙", 0,
                List.of("#examplemod:base/shields"), List.of(33, 31, 8, 32),
                List.of(new AttributeMod("examplemod:holy_freeze_aura", 1.0, "ADD_VALUE", "神圣冰冻灵气"),
                        new AttributeMod("l2damagetracker:freezing_damage", 0.30, "MULTIPLY_BASE", "+30% 冰冻伤害"),
                        new AttributeMod("l2weaponry:shield_defense", 25.0, "ADD_VALUE", "+25 绝对盾防"),
                        new AttributeMod("examplemod:cannot_be_frozen", 1.0, "ADD_VALUE", "无法冰冻"),
                        new AttributeMod("examplemod:mana_on_kill", 5.0, "ADD_VALUE", "+5 击杀回蓝(x4)"))));

        // ==================== 四、武器 (Weapons - 65个) ====================
        list.add(new Runeword("steel", "Steel", "钢铁", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/axes", "#examplemod:base/blunt", "#examplemod:base/claws", "#examplemod:base/polearms_spears"),
                List.of(3, 1),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 2.0, "ADD_VALUE", "+2 伤害"),
                        new AttributeMod("examplemod:open_wounds", 50.0, "ADD_VALUE", "50% 撕开伤口"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("malice", "Malice", "怨恨", 0,
                List.of("#examplemod:base/melee"), List.of(6, 1, 5),
                List.of(new AttributeMod("apothic_attributes:armor_shred", 0.25, "ADD_VALUE", "-25% 目标防御"),
                        new AttributeMod("examplemod:open_wounds", 100.0, "ADD_VALUE", "100% 撕开伤口"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"))));

        list.add(new Runeword("leaf", "Leaf", "叶子", 0,
                List.of("#examplemod:base/staves"), List.of(3, 8),
                List.of(new AttributeMod("irons_spellbooks:fire_spell_power", 0.40, "MULTIPLY_BASE", "+40% 火系法强"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"),
                        new AttributeMod("irons_spellbooks:ice_magic_resist", 0.33, "MULTIPLY_BASE", "+33% 冰抗"))));

        list.add(new Runeword("pattern", "Pattern", "花样", 0,
                List.of("#examplemod:base/claws", "#examplemod:base/daggers"), List.of(7, 9, 10),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 4.0, "ADD_VALUE", "+4 增强伤害"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.12, "ADD_VALUE", "+12% 暴击"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.15, "MULTIPLY_BASE", "+15% 全抗"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("zephyr", "Zephyr", "和风", 0,
                List.of("#examplemod:base/bows", "#examplemod:base/polearms_spears"), List.of(9, 5),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.25, "MULTIPLY_BASE", "+25% 移速"),
                        new AttributeMod("apothic_attributes:arrow_velocity", 0.30, "MULTIPLY_BASE", "+30% 箭矢速度"),
                        new AttributeMod("apothic_attributes:draw_speed", 0.25, "MULTIPLY_BASE", "+25% 攻速"))));

        list.add(new Runeword("holy_thunder", "Holy Thunder", "神圣雷击", 0,
                List.of("#examplemod:base/staves", "#examplemod:base/blunt"), List.of(5, 8, 9, 7),
                List.of(new AttributeMod("irons_spellbooks:lightning_spell_power", 0.35, "MULTIPLY_BASE", "+35% 闪电法强"),
                        new AttributeMod("l2damagetracker:lightning_damage", 0.25, "MULTIPLY_BASE", "+25% 闪电伤害"),
                        new AttributeMod("irons_spellbooks:lightning_magic_resist", 0.60, "MULTIPLY_BASE", "+60% 雷抗"))));

        list.add(new Runeword("neophyte", "Neophyte", "新秀", 0,
                List.of("#examplemod:base/staves", "#examplemod:base/claws", "#examplemod:base/blunt"), List.of(10, 3),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.35, "MULTIPLY_BASE", "+35% 快速施法"),
                        new AttributeMod("apothic_attributes:cold_damage", 3.0, "ADD_VALUE", "+3 冰冷伤害"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("strength", "Strength", "力量", 0,
                List.of("#examplemod:base/melee"), List.of(11, 3),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.05, "ADD_VALUE", "25% 压碎打击"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"),
                        new AttributeMod("minecraft:generic.attack_damage", 3.5, "ADD_VALUE", "+3.5 伤害"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("edge", "Edge", "边缘", 0,
                List.of("#examplemod:base/bows"), List.of(3, 7, 11),
                List.of(new AttributeMod("apothic_attributes:draw_speed", 0.35, "MULTIPLY_BASE", "+35% 拉弓提速"),
                        new AttributeMod("examplemod:damage_to_demons", 350.0, "ADD_VALUE", "+350% 对恶魔伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 280.0, "ADD_VALUE", "+280% 对不死伤害"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("kings_grace", "King's Grace", "国王的恩典", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/claws", "#examplemod:base/blunt"), List.of(11, 8, 10),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 4.5, "ADD_VALUE", "+4.5 攻击伤害"),
                        new AttributeMod("examplemod:damage_to_demons", 100.0, "ADD_VALUE", "+100% 对恶魔伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 50.0, "ADD_VALUE", "+50% 对不死伤害"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"))));

        list.add(new Runeword("spirit_weapon", "Spirit (Weapon)", "精神 (武器)", 2,
                List.of("#examplemod:base/swords", "#examplemod:base/staves"), List.of(7, 10, 9, 11),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.30, "MULTIPLY_BASE", "+30% 快速施法"),
                        new AttributeMod("irons_spellbooks:max_mana", 80.0, "ADD_VALUE", "+80 法力上限"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"))));

        list.add(new Runeword("purity", "Purity", "纯净", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/staves", "#examplemod:base/bows"), List.of(11, 5, 10),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.45, "MULTIPLY_BASE", "+45% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 5.0, "ADD_VALUE", "+5 攻击伤害"),
                        new AttributeMod("examplemod:damage_to_demons", 100.0, "ADD_VALUE", "+100% 对恶魔伤害"),
                        new AttributeMod("apothic_attributes:healing_received", 0.25, "MULTIPLY_BASE", "祈祷光环回复"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"))));

        list.add(new Runeword("insight", "Insight", "眼光", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/staves"), List.of(8, 3, 7, 12),
                List.of(new AttributeMod("irons_spellbooks:mana_regen", 0.85, "MULTIPLY_BASE", "+85% 冥思回蓝"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.35, "MULTIPLY_BASE", "+35% 快速施法"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADD_VALUE", "+20% 暴击率"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("honor", "Honor", "荣耀", 1,
                List.of("#examplemod:base/melee"), List.of(11, 1, 6, 3, 12),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 5.5, "ADD_VALUE", "+5.5 物理伤害"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.25, "ADD_VALUE", "+25% 致命一击"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("rampage", "Rampage", "狂暴突进", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/staves"), List.of(13, 1, 6, 5),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.65, "MULTIPLY_BASE", "+65% 极速攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 5.5, "ADD_VALUE", "+5.5 物理伤害"),
                        new AttributeMod("apothic_attributes:armor_shred", 0.25, "ADD_VALUE", "25% 破甲"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.25, "MULTIPLY_BASE", "+25% 火抗"))));

        list.add(new Runeword("echo", "Echo", "回响", 0,
                List.of("#examplemod:base/daggers", "#examplemod:base/polearms_spears", "#examplemod:base/bows"), List.of(12, 14),
                List.of(new AttributeMod("apothic_attributes:projectile_damage", 0.30, "MULTIPLY_BASE", "+30% 穿透投射伤"),
                        new AttributeMod("minecraft:generic.attack_damage", 4.5, "ADD_VALUE", "+4.5 伤害"),
                        new AttributeMod("examplemod:life_on_kill", 2.0, "ADD_VALUE", "+2 击杀回血"))));

        list.add(new Runeword("black", "Black", "黑色", 0,
                List.of("#examplemod:base/blunt", "#examplemod:base/claws"), List.of(10, 16, 4),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.08, "ADD_VALUE", "40% 压碎打击"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.15, "MULTIPLY_BASE", "+15% 攻速"),
                        new AttributeMod("minecraft:generic.attack_knockback", 1.5, "ADD_VALUE", "+1.5 击退"))));

        list.add(new Runeword("white", "White", "白色", 0,
                List.of("#examplemod:base/staves", "#examplemod:base/daggers"), List.of(14, 16),
                List.of(new AttributeMod("irons_spellbooks:eldritch_spell_power", 0.40, "MULTIPLY_BASE", "+40% 幽冥/死灵法强"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.20, "MULTIPLY_BASE", "+20% 快速施法"),
                        new AttributeMod("irons_spellbooks:max_mana", 80.0, "ADD_VALUE", "+80 法力上限"))));

        list.add(new Runeword("memory", "Memory", "回忆", 4,
                List.of("#examplemod:base/staves"), List.of(17, 16, 12, 5),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.33, "MULTIPLY_BASE", "+33% 快速施法"),
                        new AttributeMod("irons_spellbooks:max_mana", 0.30, "MULTIPLY_BASE", "+30% 法力上限"),
                        new AttributeMod("minecraft:generic.max_absorption", 6.0, "ADD_VALUE", "能量护盾吸收"))));

        list.add(new Runeword("harmony", "Harmony", "和谐", 0,
                List.of("#examplemod:base/bows"), List.of(3, 6, 12, 18),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.30, "MULTIPLY_BASE", "10级活力光环跑速"),
                        new AttributeMod("apothic_attributes:arrow_damage", 0.45, "MULTIPLY_BASE", "+45% 箭矢增伤"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.20, "MULTIPLY_BASE", "+20% 回蓝"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("melody", "Melody", "旋律", 0,
                List.of("#examplemod:base/bows"), List.of(13, 18, 4),
                List.of(new AttributeMod("apothic_attributes:arrow_damage", 0.40, "MULTIPLY_BASE", "+40% 弓弩增伤"),
                        new AttributeMod("examplemod:damage_to_undead", 300.0, "ADD_VALUE", "+300% 对不死伤害"),
                        new AttributeMod("apothic_attributes:draw_speed", 0.20, "MULTIPLY_BASE", "+20% 攻速"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.10, "ADD_VALUE", "+10% 闪避"))));

        list.add(new Runeword("unbending_will", "Unbending Will", "不屈意志", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/two_handed_swords"), List.of(19, 16, 6, 2, 1, 15),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 8.0, "ADD_VALUE", "+8 伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 75.0, "ADD_VALUE", "+75% 对不死伤害"),
                        new AttributeMod("apothic_attributes:life_steal", 0.10, "ADD_VALUE", "10% 吸血"),
                        new AttributeMod("l2damagetracker:damage_absorption", 5.0, "ADD_VALUE", "+5 固定减伤"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻速"))));

        list.add(new Runeword("obedience", "Obedience", "顺从", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/blunt"), List.of(15, 18, 10, 5, 19),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.08, "ADD_VALUE", "40% 压碎打击"),
                        new AttributeMod("examplemod:fire_penetration", 25.0, "ADD_VALUE", "-25% 敌人火抗"),
                        new AttributeMod("apothic_attributes:armor_shred", 0.25, "ADD_VALUE", "25% 破甲"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.25, "MULTIPLY_BASE", "+25% 全抗"))));

        list.add(new Runeword("passion", "Passion", "热情", 0,
                List.of("#examplemod:base/all_weapons"), List.of(14, 9, 2, 20),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 狂热攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 6.0, "ADD_VALUE", "+6 伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 75.0, "ADD_VALUE", "+75% 对不死伤害"),
                        new AttributeMod("examplemod:hit_blind", 25.0, "ADD_VALUE", "击中致盲目标"))));

        list.add(new Runeword("voice_of_reason", "Voice of Reason", "理性之声", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/blunt", "#examplemod:base/bows"), List.of(20, 18, 1, 2),
                List.of(new AttributeMod("examplemod:ice_penetration", 24.0, "ADD_VALUE", "-24% 敌人冰抗"),
                        new AttributeMod("apothic_attributes:cold_damage", 8.0, "ADD_VALUE", "+8 冰冷附加伤"),
                        new AttributeMod("examplemod:damage_to_demons", 270.0, "ADD_VALUE", "+270% 对恶魔伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 300.0, "ADD_VALUE", "+300% 对不死伤害"),
                        new AttributeMod("examplemod:cannot_be_frozen", 1.0, "ADD_VALUE", "无法冰冻"))));

        list.add(new Runeword("lawbringer", "Lawbringer", "执法者", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/blunt", "#examplemod:base/claws"), List.of(11, 20, 18),
                List.of(new AttributeMod("apothic_attributes:armor_shred", 0.45, "ADD_VALUE", "衰老破甲45%"),
                        new AttributeMod("irons_spellbooks:holy_spell_power", 0.35, "MULTIPLY_BASE", "+35% 神圣威力"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"))));

        list.add(new Runeword("loyalty", "Loyalty", "忠诚", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/bows"), List.of(21, 3, 14),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.50, "MULTIPLY_BASE", "+50% 快速施法"),
                        new AttributeMod("minecraft:generic.attack_damage", 7.0, "ADD_VALUE", "+7 伤害"),
                        new AttributeMod("examplemod:damage_to_demons", 75.0, "ADD_VALUE", "+75% 对恶魔伤害"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADD_VALUE", "20% 压碎打击"),
                        new AttributeMod("examplemod:mana_on_kill", 4.0, "ADD_VALUE", "+4 击杀回蓝(x4)"))));

        list.add(new Runeword("crescent_moon", "Crescent Moon", "新月", 0,
                List.of("#examplemod:base/axes", "#examplemod:base/swords", "#examplemod:base/polearms_spears", "#examplemod:base/claws"), List.of(13, 22, 3),
                List.of(new AttributeMod("examplemod:lightning_penetration", 15.0, "ADD_VALUE", "-15% 敌人电抗"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻速"),
                        new AttributeMod("examplemod:open_wounds", 10.0, "ADD_VALUE", "10% 撕开伤口"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("venom", "Venom", "毒液", 0,
                List.of("#examplemod:base/all_weapons"), List.of(7, 14, 23),
                List.of(new AttributeMod("irons_spellbooks:nature_spell_power", 0.45, "MULTIPLY_BASE", "+45% 毒素法强"),
                        new AttributeMod("examplemod:poison_penetration", 25.0, "ADD_VALUE", "-25% 敌人毒抗"),
                        new AttributeMod("examplemod:mana_steal", 7.0, "ADD_VALUE", "7% 击中偷蓝"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"))));

        list.add(new Runeword("oath", "Oath", "誓约", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/axes", "#examplemod:base/blunt"), List.of(13, 21, 23, 17),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.50, "MULTIPLY_BASE", "+50% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 8.5, "ADD_VALUE", "+8.5 伤害"),
                        new AttributeMod("examplemod:damage_to_demons", 75.0, "ADD_VALUE", "+75% 对恶魔伤害"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"))));

        list.add(new Runeword("rift", "Rift", "裂隙", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/swords", "#examplemod:base/staves"), List.of(15, 18, 20, 25),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.55, "MULTIPLY_BASE", "+55% 攻速"),
                        new AttributeMod("irons_spellbooks:ice_spell_power", 0.40, "MULTIPLY_BASE", "冰封球触发"),
                        new AttributeMod("irons_spellbooks:fire_spell_power", 0.35, "MULTIPLY_BASE", "+35% 火强"),
                        new AttributeMod("examplemod:damage_to_mana", 38.0, "ADD_VALUE", "38% 受损生命转法力"))));

        list.add(new Runeword("kingslayer", "Kingslayer", "弑君者", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/axes"), List.of(23, 22, 25, 19),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.07, "ADD_VALUE", "33% 压碎打击"),
                        new AttributeMod("examplemod:open_wounds", 10.0, "ADD_VALUE", "10% 撕开伤口"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("apothic_attributes:cold_damage", 7.0, "ADD_VALUE", "+7 冰伤"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.30, "MULTIPLY_BASE", "+30% 攻速"))));

        list.add(new Runeword("heart_of_the_oak", "Heart of the Oak", "橡树之心", 3,
                List.of("#examplemod:base/staves", "#examplemod:base/blunt"), List.of(18, 26, 21, 10),
                List.of(new AttributeMod("irons_spellbooks:cast_time_reduction", 0.30, "MULTIPLY_BASE", "+30% 快速施法"),
                        new AttributeMod("examplemod:damage_to_demons", 75.0, "ADD_VALUE", "+75% 对恶魔伤害"),
                        new AttributeMod("examplemod:mana_steal", 7.0, "ADD_VALUE", "7% 击中偷蓝"),
                        new AttributeMod("irons_spellbooks:max_mana", 0.15, "MULTIPLY_BASE", "+15% 法力上限"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.25, "MULTIPLY_BASE", "+25% 全抗"))));

        list.add(new Runeword("silence", "Silence", "寂静", 2,
                List.of("#examplemod:base/all_weapons"), List.of(14, 2, 15, 24, 3, 26),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.50, "MULTIPLY_BASE", "+50% 全抗"),
                        new AttributeMod("examplemod:thorns_damage", 24.0, "ADD_VALUE", "24级荆棘灵气"),
                        new AttributeMod("examplemod:damage_to_undead", 75.0, "ADD_VALUE", "+75% 对不死伤害"),
                        new AttributeMod("examplemod:mana_steal", 11.0, "ADD_VALUE", "11% 击中偷蓝"),
                        new AttributeMod("examplemod:hit_blind", 33.0, "ADD_VALUE", "击中致盲目标"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("death", "Death", "死亡", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/axes"), List.of(15, 1, 26, 9, 25),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.10, "ADD_VALUE", "50% 压碎打击"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.25, "ADD_VALUE", "+25% 致命一击"),
                        new AttributeMod("apothic_attributes:crit_damage", 0.60, "MULTIPLY_BASE", "+60% 暴击伤害"),
                        new AttributeMod("examplemod:mana_steal", 7.0, "ADD_VALUE", "7% 击中偷蓝"),
                        new AttributeMod("minecraft:generic.attack_damage", 9.0, "ADD_VALUE", "+9 物理伤害"))));

        list.add(new Runeword("chaos", "Chaos", "混沌", 0,
                List.of("#examplemod:base/claws"), List.of(19, 27, 22),
                List.of(new AttributeMod("minecraft:player.sweeping_damage_ratio", 0.65, "ADD_VALUE", "剑刃之舞旋风斩"),
                        new AttributeMod("l2damagetracker:magic_damage", 0.25, "MULTIPLY_BASE", "+25% 魔法附加伤"),
                        new AttributeMod("examplemod:open_wounds", 10.0, "ADD_VALUE", "10% 撕开伤口"),
                        new AttributeMod("examplemod:life_on_kill", 3.0, "ADD_VALUE", "+3 击杀回血"))));

        list.add(new Runeword("call_to_arms", "Call to Arms", "战争召唤", 1,
                List.of("#examplemod:base/melee"), List.of(11, 8, 23, 24, 27),
                List.of(new AttributeMod("minecraft:generic.max_health", 12.0, "ADD_VALUE", "战斗体制 +12 生命"),
                        new AttributeMod("irons_spellbooks:max_mana", 120.0, "ADD_VALUE", "+120 最大法力"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.40, "MULTIPLY_BASE", "+40% 攻速"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"))));

        list.add(new Runeword("fortitude_weapon", "Fortitude (Weapon)", "刚毅 (武器)", 0,
                List.of("#examplemod:base/all_weapons"), List.of(1, 12, 14, 28),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 13.0, "ADD_VALUE", "+13 物理增伤"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.25, "MULTIPLY_BASE", "+25% 快速施法"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADD_VALUE", "+20% 致命一击"),
                        new AttributeMod("examplemod:damage_to_mana", 12.0, "ADD_VALUE", "12% 受损生命转法力"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.30, "MULTIPLY_BASE", "+30% 全抗"))));

        list.add(new Runeword("grief", "Grief", "悔恨", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/axes", "#examplemod:base/polearms_spears"), List.of(5, 3, 28, 23, 8),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 17.0, "ADD_VALUE", "+17 绝对点伤"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.35, "MULTIPLY_BASE", "+35% 攻速"),
                        new AttributeMod("examplemod:damage_to_demons", 150.0, "ADD_VALUE", "+150% 对恶魔伤害"),
                        new AttributeMod("examplemod:poison_penetration", 18.0, "ADD_VALUE", "-18% 敌人毒抗"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"),
                        new AttributeMod("examplemod:life_on_kill", 3.0, "ADD_VALUE", "+3 击杀回血"))));

        list.add(new Runeword("jealousy", "Jealousy", "嫉妒", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/daggers"), List.of(28, 25, 10),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 10.0, "ADD_VALUE", "+10 伤害"),
                        new AttributeMod("apothic_attributes:projectile_damage", 0.35, "MULTIPLY_BASE", "+35% 投射分裂增伤"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADD_VALUE", "+20% 致命一击"),
                        new AttributeMod("apothic_attributes:life_steal", 0.05, "ADD_VALUE", "5% 吸血"))));

        list.add(new Runeword("wind", "Wind", "疾风", 0,
                List.of("#examplemod:base/melee", "#examplemod:base/staves"), List.of(29, 1),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.25, "MULTIPLY_BASE", "+25% 跑速"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.40, "MULTIPLY_BASE", "+40% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 10.0, "ADD_VALUE", "+10 伤害"),
                        new AttributeMod("irons_spellbooks:evocation_spell_power", 0.40, "MULTIPLY_BASE", "龙卷风法强"),
                        new AttributeMod("examplemod:life_on_kill", 2.0, "ADD_VALUE", "+2 击杀回血"))));

        list.add(new Runeword("madness", "Madness", "疯狂", 0,
                List.of("#examplemod:base/all_weapons"), List.of(29, 23, 26, 13),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.45, "MULTIPLY_BASE", "+45% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 11.0, "ADD_VALUE", "+11 伤害"),
                        new AttributeMod("apothic_attributes:prot_shred", 0.35, "ADD_VALUE", "降低抗性诅咒"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.25, "MULTIPLY_BASE", "+25% 全抗"))));

        list.add(new Runeword("wrath", "Wrath", "愤怒", 0,
                List.of("#examplemod:base/bows"), List.of(21, 17, 30, 23),
                List.of(new AttributeMod("apothic_attributes:draw_speed", 0.40, "MULTIPLY_BASE", "+40% 拉弓提速"),
                        new AttributeMod("examplemod:damage_to_demons", 375.0, "ADD_VALUE", "+375% 对恶魔伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 275.0, "ADD_VALUE", "+275% 对不死伤害"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADD_VALUE", "20% 压碎打击"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("examplemod:cannot_be_frozen", 1.0, "ADD_VALUE", "无法冰冻"))));

        list.add(new Runeword("beast", "Beast", "狂暴", 0,
                List.of("#examplemod:base/axes", "#examplemod:base/blunt", "#examplemod:base/staves"), List.of(30, 3, 22, 23, 17),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.40, "MULTIPLY_BASE", "狂热光环 +40% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 9.0, "ADD_VALUE", "+9 物理伤害"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADD_VALUE", "20% 压碎打击"),
                        new AttributeMod("examplemod:open_wounds", 10.0, "ADD_VALUE", "10% 撕开伤口"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("examplemod:mana_on_kill", 2.0, "ADD_VALUE", "+2 击杀回蓝(x4)"))));

        list.add(new Runeword("eternity", "Eternity", "永恒", 0,
                List.of("#examplemod:base/melee"), List.of(11, 30, 24, 12, 29),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 10.0, "ADD_VALUE", "+10 伤害"),
                        new AttributeMod("twilightforest:minion_count", 6.0, "ADD_VALUE", "+6 重生仆从上限"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADD_VALUE", "20% 压碎打击"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"),
                        new AttributeMod("examplemod:life_on_kill", 2.0, "ADD_VALUE", "+2 击杀回血"),
                        new AttributeMod("examplemod:cannot_be_frozen", 1.0, "ADD_VALUE", "无法冰冻"))));

        list.add(new Runeword("infinity", "Infinity", "无限", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/staves"), List.of(30, 23, 30, 24),
                List.of(new AttributeMod("examplemod:lightning_penetration", 45.0, "ADD_VALUE", "12级信念 -45%电抗"),
                        new AttributeMod("examplemod:fire_penetration", 35.0, "ADD_VALUE", "12级信念 -35%火抗"),
                        new AttributeMod("examplemod:ice_penetration", 35.0, "ADD_VALUE", "12级信念 -35%冰抗"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.08, "ADD_VALUE", "40% 压碎打击"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("minecraft:generic.movement_speed", 0.35, "MULTIPLY_BASE", "+35% 跑速"))));

        list.add(new Runeword("fury", "Fury", "狂怒", 0,
                List.of("#examplemod:base/melee"), List.of(31, 25, 5),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.40, "MULTIPLY_BASE", "+40% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 10.0, "ADD_VALUE", "+10 伤害"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.50, "ADD_VALUE", "+50% 致命一击"),
                        new AttributeMod("apothic_attributes:life_steal", 0.06, "ADD_VALUE", "6% 吸血"),
                        new AttributeMod("apothic_attributes:armor_pierce", 6.0, "ADD_VALUE", "+6 穿甲"))));

        list.add(new Runeword("famine", "Famine", "饥荒", 0,
                List.of("#examplemod:base/axes", "#examplemod:base/blunt", "#examplemod:base/swords"), List.of(19, 27, 9, 31),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 12.0, "ADD_VALUE", "+12 物理伤害"),
                        new AttributeMod("examplemod:fire_penetration", 15.0, "ADD_VALUE", "-15% 敌人火抗"),
                        new AttributeMod("examplemod:lightning_penetration", 15.0, "ADD_VALUE", "-15% 敌人电抗"),
                        new AttributeMod("examplemod:ice_penetration", 15.0, "ADD_VALUE", "-15% 敌人冰抗"),
                        new AttributeMod("apothic_attributes:fire_damage", 6.0, "ADD_VALUE", "+6 火焰伤害"),
                        new AttributeMod("apothic_attributes:cold_damage", 6.0, "ADD_VALUE", "+6 冰冷伤害"))));

        list.add(new Runeword("faith", "Faith", "信心", 0,
                List.of("#examplemod:base/bows"), List.of(27, 31, 20, 2),
                List.of(new AttributeMod("apothic_attributes:draw_speed", 0.45, "MULTIPLY_BASE", "15级狂热光环弓速"),
                        new AttributeMod("apothic_attributes:arrow_damage", 0.65, "MULTIPLY_BASE", "+65% 远程增伤"),
                        new AttributeMod("examplemod:damage_to_undead", 75.0, "ADD_VALUE", "+75% 对不死伤害"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.15, "MULTIPLY_BASE", "+15% 全抗"))));

        list.add(new Runeword("ice", "Ice", "冰冻", 0,
                List.of("#examplemod:base/bows"), List.of(11, 13, 31, 28),
                List.of(new AttributeMod("examplemod:holy_freeze_aura", 1.0, "ADD_VALUE", "神圣冰冻光环"),
                        new AttributeMod("examplemod:ice_penetration", 20.0, "ADD_VALUE", "-20% 敌人冰抗"),
                        new AttributeMod("irons_spellbooks:ice_spell_power", 0.30, "MULTIPLY_BASE", "+30% 冰系技能伤害"),
                        new AttributeMod("apothic_attributes:arrow_damage", 0.45, "MULTIPLY_BASE", "+45% 箭矢增伤"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADD_VALUE", "+20% 致命一击"))));

        list.add(new Runeword("brand", "Brand", "品牌", 0,
                List.of("#examplemod:base/bows"), List.of(31, 28, 23, 25),
                List.of(new AttributeMod("apothic_attributes:arrow_damage", 0.60, "MULTIPLY_BASE", "+60% 爆炸箭增伤"),
                        new AttributeMod("examplemod:damage_to_demons", 300.0, "ADD_VALUE", "+300% 对恶魔伤害"),
                        new AttributeMod("twilightforest:minion_count", 4.0, "ADD_VALUE", "+4 召唤骷髅弓箭手"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADD_VALUE", "+20% 致命一击"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"))));

        list.add(new Runeword("phoenix_weapon", "Phoenix (Weapon)", "凤凰 (武器)", 0,
                List.of("#examplemod:base/all_weapons"), List.of(26, 26, 28, 31),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 14.0, "ADD_VALUE", "+14 物理增伤"),
                        new AttributeMod("examplemod:fire_penetration", 28.0, "ADD_VALUE", "-28% 敌人火抗"),
                        new AttributeMod("examplemod:mana_steal", 14.0, "ADD_VALUE", "14% 击中偷蓝"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADD_VALUE", "+20% 致命一击"),
                        new AttributeMod("apothic_attributes:healing_received", 0.40, "MULTIPLY_BASE", "救赎光环恢复"))));

        list.add(new Runeword("destruction", "Destruction", "毁灭", 0,
                List.of("#examplemod:base/swords", "#examplemod:base/polearms_spears", "#examplemod:base/staves"), List.of(26, 28, 30, 31, 18),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.35, "MULTIPLY_BASE", "+35% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 11.0, "ADD_VALUE", "+11 伤害"),
                        new AttributeMod("l2damagetracker:explosion_damage", 0.35, "MULTIPLY_BASE", "陨石/熔岩爆裂威力"),
                        new AttributeMod("examplemod:mana_steal", 7.0, "ADD_VALUE", "7% 击中偷蓝"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADD_VALUE", "20% 压碎打击"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADD_VALUE", "+20% 暴击"))));

        list.add(new Runeword("last_wish", "Last Wish", "最后希望", 0,
                List.of("#examplemod:base/axes", "#examplemod:base/blunt", "#examplemod:base/swords", "#examplemod:base/staves"), List.of(31, 23, 31, 29, 31, 30),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 15.0, "ADD_VALUE", "20级力量光环 +15 伤害"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.14, "ADD_VALUE", "65% 压碎打击"),
                        new AttributeMod("apothic_attributes:life_steal", 0.12, "ADD_VALUE", "偷取生命触发"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("examplemod:life_on_kill", 2.0, "ADD_VALUE", "+2 击杀回血"),
                        new AttributeMod("minecraft:generic.luck", 3.0, "ADD_VALUE", "+3 幸运"))));

        list.add(new Runeword("rapture", "Rapture", "狂喜", 0,
                List.of("#examplemod:base/blunt"), List.of(26, 31, 28, 8, 18, 23),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 12.0, "ADD_VALUE", "+12 伤害"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.40, "ADD_VALUE", "+40% 致命一击"),
                        new AttributeMod("examplemod:mana_steal", 7.0, "ADD_VALUE", "7% 击中偷蓝"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("apothic_attributes:prot_shred", 0.35, "ADD_VALUE", "降低抵抗诅咒"),
                        new AttributeMod("irons_spellbooks:ice_magic_resist", 0.50, "MULTIPLY_BASE", "+50% 冰抗"))));

// 瘟疫 (Plague) - 武器
        list.add(new Runeword("plague", "Plague", "瘟疫", 2,
                List.of("#examplemod:base/all_weapons"), List.of(32, 19, 22),
                List.of(new AttributeMod("examplemod:damage_to_demons", 360.0, "ADD_VALUE", "+360% 对恶魔伤害"),
                        new AttributeMod("examplemod:poison_penetration", 20.0, "ADD_VALUE", "-20% 敌人毒抗"),
                        new AttributeMod("examplemod:open_wounds", 10.0, "ADD_VALUE", "10% 撕开伤口"),
                        new AttributeMod("examplemod:freeze_target", 200.0, "ADD_VALUE", "+200 冰冷值"), // 原 3.0 改为 200.0
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.20, "MULTIPLY_BASE", "+20% 快速施法"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.25, "ADD_VALUE", "+25% 致命一击"))));

// 迷雾 (Mist) - 弓
        list.add(new Runeword("mist", "Mist", "迷雾", 2,
                List.of("#examplemod:base/bows"), List.of(32, 13, 25, 10, 6),
                List.of(new AttributeMod("apothic_attributes:arrow_damage", 0.65, "MULTIPLY_BASE", "专注光环远程增伤"),
                        new AttributeMod("apothic_attributes:projectile_damage", 0.45, "MULTIPLY_BASE", "100% 穿透箭"),
                        new AttributeMod("examplemod:freeze_target", 200.0, "ADD_VALUE", "+200 冰冷值"), // 原 3.0 改为 200.0
                        new AttributeMod("irons_spellbooks:spell_resist", 0.40, "MULTIPLY_BASE", "+40% 全抗"))));

// 支配 (Dominion) - 法杖/匕首
        list.add(new Runeword("dominion", "Dominion", "支配", 0,
                List.of("#examplemod:base/staves", "#examplemod:base/daggers"), List.of(32, 27),
                List.of(new AttributeMod("irons_spellbooks:eldritch_spell_power", 0.45, "MULTIPLY_BASE", "+45% 幽冥/魔像精通"),
                        new AttributeMod("twilightforest:minion_count", 1.0, "ADD_VALUE", "+1 最大魔像"),
                        new AttributeMod("examplemod:freeze_target", 200.0, "ADD_VALUE", "+200 冰冷值"), // 原 3.0 改为 200.0
                        new AttributeMod("examplemod:damage_to_undead", 50.0, "ADD_VALUE", "+50% 对不死伤害"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.25, "MULTIPLY_BASE", "+25% 快速施法"))));


// 末日 (Doom) - 斧/锤/长柄/法杖/剑
        list.add(new Runeword("doom", "Doom", "末日", 2,
                List.of("#examplemod:base/axes", "#examplemod:base/blunt", "#examplemod:base/polearms_spears", "#examplemod:base/staves", "#examplemod:base/swords"), List.of(15, 27, 22, 28, 32),
                List.of(new AttributeMod("examplemod:holy_freeze_aura", 1.0, "ADD_VALUE", "16级神圣冰冻光环"),
                        new AttributeMod("examplemod:ice_penetration", 40.0, "ADD_VALUE", "-40% 敌人冰抗"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.45, "MULTIPLY_BASE", "+45% 攻速"),
                        new AttributeMod("examplemod:open_wounds", 10.0, "ADD_VALUE", "10% 撕开伤口"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"),
                        new AttributeMod("examplemod:freeze_target", 200.0, "ADD_VALUE", "+200 冰冷值")))); // 原 3.0 改为 200.0

// 正义之手 (Hand of Justice)
        list.add(new Runeword("hand_of_justice", "Hand of Justice", "正义之手", 0,
                List.of("#examplemod:base/all_weapons"), List.of(29, 32, 11, 28),
                List.of(new AttributeMod("examplemod:holy_fire_aura", 1.0, "ADD_VALUE", "12级圣火光环"),
                        new AttributeMod("examplemod:fire_penetration", 20.0, "ADD_VALUE", "-20% 敌人火抗"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.33, "MULTIPLY_BASE", "+33% 攻速"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADD_VALUE", "7% 吸血"),
                        new AttributeMod("examplemod:life_on_kill", 2.0, "ADD_VALUE", "+2 击杀回血"),
                        new AttributeMod("examplemod:freeze_target", 200.0, "ADD_VALUE", "+200 冰冷值")))); // 原 3.0 改为 200.0

// 骄傲 (Pride)
        list.add(new Runeword("pride", "Pride", "骄傲", 0,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/staves"), List.of(32, 29, 16, 28),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 15.0, "ADD_VALUE", "20级专注光环增伤"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADD_VALUE", "+20% 致命一击"),
                        new AttributeMod("examplemod:life_on_kill", 2.0, "ADD_VALUE", "+2 击杀回血"),
                        new AttributeMod("examplemod:freeze_target", 200.0, "ADD_VALUE", "+200 冰冷值"), // 原 3.0 改为 200.0
                        new AttributeMod("l2damagetracker:lightning_damage", 0.20, "MULTIPLY_BASE", "+20% 闪电附加"))));

// 庇护所 (Asylum)
        list.add(new Runeword("asylum", "Asylum", "庇护所", 4,
                List.of("#examplemod:base/blunt", "#examplemod:base/two_handed_swords", "#examplemod:base/polearms_spears", "#examplemod:base/staves", "#examplemod:base/bows"),
                List.of(25, 32, 15, 31, 14, 22),
                List.of(new AttributeMod("irons_spellbooks:holy_spell_power", 0.65, "MULTIPLY_BASE", "庇护所光环+天堂之拳"),
                        new AttributeMod("minecraft:generic.attack_damage", 13.0, "ADD_VALUE", "+13 伤害"),
                        new AttributeMod("examplemod:open_wounds", 30.0, "ADD_VALUE", "30% 撕开伤口"),
                        new AttributeMod("examplemod:freeze_target", 200.0, "ADD_VALUE", "+200 冰冷值")))); // 原 3.0 改为 200.0


        list.add(new Runeword("obsession", "Obsession", "执念", 4,
                List.of("#examplemod:base/staves"), List.of(33, 24, 20, 17, 16, 4),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.35, "MULTIPLY_BASE", "+35% 终极法强"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.30, "MULTIPLY_BASE", "+30% 回蓝"),
                        new AttributeMod("minecraft:generic.max_health", 0.25, "MULTIPLY_BASE", "+25% 生命上限"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.65, "MULTIPLY_BASE", "+65% 全抗"),
                        new AttributeMod("minecraft:generic.luck", 2.0, "ADD_VALUE", "+2 幸运"))));

        list.add(new Runeword("breath_of_the_dying", "Breath of the Dying", "死亡呼吸", 0,
                List.of("#examplemod:base/all_weapons"), List.of(26, 15, 1, 2, 33, 5),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.60, "MULTIPLY_BASE", "+60% 巅峰攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 16.0, "ADD_VALUE", "+16 物理伤害"),
                        new AttributeMod("examplemod:damage_to_undead", 200.0, "ADD_VALUE", "+200% 对不死伤害"),
                        new AttributeMod("examplemod:mana_steal", 7.0, "ADD_VALUE", "7% 击中偷蓝"),
                        new AttributeMod("apothic_attributes:life_steal", 0.12, "ADD_VALUE", "12% 生命偷取"),
                        new AttributeMod("examplemod:prevent_heal", 1.0, "ADD_VALUE", "防止怪物自疗"))));

        list.add(new Runeword("zenith", "Zenith", "巅峰", 3,
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/staves", "#examplemod:base/bows", "#examplemod:base/two_handed_swords"),
                List.of(31, 33, 9, 10, 8),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.65, "MULTIPLY_BASE", "12级救助全抗光环"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.40, "MULTIPLY_BASE", "+40% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 13.0, "ADD_VALUE", "+13 物理伤害"),
                        new AttributeMod("apothic_attributes:fire_damage", 8.0, "ADD_VALUE", "+8 火焰伤害"),
                        new AttributeMod("apothic_attributes:cold_damage", 8.0, "ADD_VALUE", "+8 寒冰伤害"),
                        new AttributeMod("minecraft:generic.movement_speed", 0.20, "MULTIPLY_BASE", "+20% 跑速"))));

        return list;
    }
}