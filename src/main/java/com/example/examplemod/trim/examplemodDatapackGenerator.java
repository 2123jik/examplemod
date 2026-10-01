package com.example.examplemod.trim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class examplemodDatapackGenerator {

    // 符文数据结构
    record Rune(int id, String name, String cnName, String pattern, String material) {}

    // 属性修改项
    record AttributeMod(String attribute, double amount, String operation, String desc) {}

    // 纹饰之语定义
    record Runeword(
            String id,
            String nameEn,
            String nameCn,
            List<String> validBaseTags,
            List<Integer> runeSequence,
            List<AttributeMod> attributes
    ) {}

    private static final Map<Integer, Rune> RUNES = new HashMap<>();

    static {
        // =========================================================================
        // 【核心修正】：原版物品全称必须带 _ingot (如 copper_ingot, iron_ingot, gold_ingot)
        // =========================================================================
        RUNES.put(1,  new Rune(1,  "El",    "艾尔",   "minecraft:sentry",        "minecraft:iron_ingot"));
        RUNES.put(2,  new Rune(2,  "Eld",   "艾德",   "minecraft:raiser",        "minecraft:iron_ingot"));
        RUNES.put(3,  new Rune(3,  "Tir",   "特尔",   "minecraft:wayfinder",     "minecraft:copper_ingot"));
        RUNES.put(4,  new Rune(4,  "Nef",   "那夫",   "minecraft:shaper",        "minecraft:copper_ingot"));
        RUNES.put(5,  new Rune(5,  "Eth",   "爱斯",   "minecraft:flow",          "minecraft:copper_ingot"));
        RUNES.put(6,  new Rune(6,  "Ith",   "伊司",   "minecraft:rib",           "minecraft:iron_ingot"));
        RUNES.put(7,  new Rune(7,  "Tal",   "塔尔",   "minecraft:wild",          "minecraft:emerald"));
        RUNES.put(8,  new Rune(8,  "Ral",   "拉尔",   "more_armor_trims:fever",  "minecraft:redstone"));
        RUNES.put(9,  new Rune(9,  "Ort",   "欧特",   "minecraft:bolt",          "minecraft:copper_ingot"));
        RUNES.put(10, new Rune(10, "Thul",  "书尔",   "minecraft:coast",         "minecraft:lapis_lazuli"));
        RUNES.put(11, new Rune(11, "Amn",   "安姆",   "more_armor_trims:ram",    "minecraft:iron_ingot"));
        RUNES.put(12, new Rune(12, "Sol",   "索尔",   "minecraft:snout",         "minecraft:gold_ingot"));
        RUNES.put(13, new Rune(13, "Shael", "夏勒",   "minecraft:flow",          "minecraft:amethyst_shard"));
        RUNES.put(14, new Rune(14, "Dol",   "多尔",   "more_armor_trims:wraith", "minecraft:quartz"));
        RUNES.put(15, new Rune(15, "Hel",   "海尔",   "more_armor_trims:origin", "minecraft:quartz"));
        RUNES.put(16, new Rune(16, "Io",    "艾欧",   "more_armor_trims:horizon","minecraft:amethyst_shard"));
        RUNES.put(17, new Rune(17, "Lum",   "卢姆",   "minecraft:eye",           "minecraft:lapis_lazuli"));
        RUNES.put(18, new Rune(18, "Ko",    "科",     "minecraft:spire",         "minecraft:emerald"));
        RUNES.put(19, new Rune(19, "Fal",   "法尔",   "more_armor_trims:beast",  "twilightforest:ironwood_ingot"));
        RUNES.put(20, new Rune(20, "Lem",   "蓝姆",   "more_armor_trims:greed",  "minecraft:gold_ingot"));
        RUNES.put(21, new Rune(21, "Pul",   "普尔",   "minecraft:ward",          "twilightforest:knightmetal_ingot"));
        RUNES.put(22, new Rune(22, "Um",    "乌姆",   "more_armor_trims:storm",  "twilightforest:steeleaf_ingot"));
        RUNES.put(23, new Rune(23, "Mal",   "马尔",   "minecraft:dune",          "twilightforest:steeleaf_ingot"));
        RUNES.put(24, new Rune(24, "Ist",   "伊斯特", "more_armor_trims:witness","minecraft:diamond"));
        RUNES.put(25, new Rune(25, "Gul",   "古尔",   "minecraft:tide",          "minecraft:diamond"));
        RUNES.put(26, new Rune(26, "Vex",   "伐克斯", "minecraft:vex",           "twilightforest:fiery_ingot"));
        RUNES.put(27, new Rune(27, "Ohm",   "欧姆",   "more_armor_trims:myth",   "cataclysm:ancient_metal_ingot"));
        RUNES.put(28, new Rune(28, "Lo",    "罗",     "minecraft:bolt",          "cataclysm:ignitium_ingot"));
        RUNES.put(29, new Rune(29, "Sur",   "瑟",     "minecraft:silence",       "cataclysm:cursium_ingot"));
        RUNES.put(30, new Rune(30, "Ber",   "贝",     "alexscaves:polarity",     "cataclysm:witherite_ingot"));
        RUNES.put(31, new Rune(31, "Jah",   "乔",     "more_armor_trims:nihility","l2complements:shulkerate_ingot"));
        RUNES.put(32, new Rune(32, "Cham",  "查姆",   "more_armor_trims:twilight","l2complements:poseidite_ingot"));
        RUNES.put(33, new Rune(33, "Zod",   "佐德",   "more_armor_trims:origin", "l2complements:eternium_ingot"));
    }

    public static void main(String[] args) {
        String rootDir = "examplemod_Datapack";
        System.out.println(">>> 正在生成纹饰之语数据包目录架构...");

        // 建议在写入前清理旧文件夹，防止残留
        deleteDirectory(new File(rootDir));
        generateLanguageFiles(rootDir); // 【新增】：全自动导出全量 78 个语言键值对
        createPackMcmeta(rootDir);
        generateItemTags(rootDir);
        generateexamplemodData(rootDir);
        generateSmithingRecipe(rootDir);

        System.out.println(">>> 数据包全部构建成功！请将 [" + rootDir + "] 文件夹置入存档 datapacks 目录测试。");
    }
    private static void generateLanguageFiles(String root) {
        File langDir = new File(root, "assets/examplemod/lang");
        langDir.mkdirs();

        List<Runeword> list = buildAllexamplemod();

        // 1. 生成 zh_cn.json (中文全套)
        StringBuilder zh = new StringBuilder("{\n");
        zh.append("  \"runeword.examplemod.title_format\": \"%s · %s\",\n");
        zh.append("  \"tooltip.examplemod.sequence_prefix\": \"时序: \",\n");
        zh.append("  \"tooltip.examplemod.progress\": \"刻印进度 (%s):\",\n\n");

        for (int i = 0; i < list.size(); i++) {
            Runeword rw = list.get(i);
            zh.append("  \"runeword.examplemod.").append(rw.id()).append("\": \"").append(rw.nameCn()).append("\"");
            if (i < list.size() - 1) zh.append(",");
            zh.append("\n");
        }
        zh.append("}\n");
        writeFile(new File(langDir, "zh_cn.json"), zh.toString());

        // 2. 生成 en_us.json (英文全套)
        StringBuilder en = new StringBuilder("{\n");
        en.append("  \"runeword.examplemod.title_format\": \"%s %s\",\n");
        en.append("  \"tooltip.examplemod.sequence_prefix\": \"Sequence: \",\n");
        en.append("  \"tooltip.examplemod.progress\": \"Engraving Progress (%s):\",\n\n");

        for (int i = 0; i < list.size(); i++) {
            Runeword rw = list.get(i);
            en.append("  \"runeword.examplemod.").append(rw.id()).append("\": \"").append(rw.nameEn()).append("\"");
            if (i < list.size() - 1) en.append(",");
            en.append("\n");
        }
        en.append("}\n");
        writeFile(new File(langDir, "en_us.json"), en.toString());

        System.out.println(">>> 成功导出全量 78 组中英文国际化语言包至 assets/examplemod/lang/！");
    }
    private static void generateSmithingRecipe(String root) {
        File recipeDir = new File(root, "data/examplemod/recipe/runes");
        recipeDir.mkdirs();

        System.out.println(">>> 正在为 33 种标准符文生成锻造台刻印配方...");

        for (Rune rune : RUNES.values()) {
            String recipeJson = serializeRuneRecipeToJson(rune);
            String fileName = String.format("rune_%02d_%s.json", rune.id(), rune.name().toLowerCase(Locale.ROOT));
            writeFile(new File(recipeDir, fileName), recipeJson);
        }

        System.out.println(">>> 成功生成全套 33 种符文工序锻造配方！");
    }

    private static String serializeRuneRecipeToJson(Rune rune) {
        // 确保模板 ID 对应真实的物理物品
        String itemTemplateId = rune.pattern();
        if (!itemTemplateId.endsWith("_armor_trim_smithing_template") && !itemTemplateId.endsWith("_smithing_template")) {
            itemTemplateId += "_armor_trim_smithing_template";
        }

        // 使用合法的单对象 base 引用 #examplemod:base/all，杜绝数组解析异常
        return """
            {
              "type": "examplemod:runeword_smithing",
              "template": {
                "item": "%s"
              },
              "base": {
                "tag": "examplemod:base/all"
              },
              "addition": {
                "item": "%s"
              }
            }
            """.formatted(itemTemplateId, rune.material());
    }

    private static void createPackMcmeta(String root) {
        String mcmeta = """
                {
                  "pack": {
                    "pack_format": 48,
                    "description": "Diablo II examplemod Trim System - 78 Full Sequences"
                  }
                }
                """;
        writeFile(new File(root, "pack.mcmeta"), mcmeta);
    }

    private static void generateItemTags(String root) {
        File tagDir = new File(root, "data/examplemod/tags/item/base");
        tagDir.mkdirs();

        // 1. 头盔
        writeTag(new File(tagDir, "helmets.json"), List.of(
                "#c:armor/helmets",
                "#irons_spellbooks:armors/helmets",
                "#minecraft:head_armor",
                "#minecraft:enchantable/head_armor",
                "#iss_magicfromtheeast:armors/helmets"
        ));

        // 2. 胸甲
        writeTag(new File(tagDir, "chestplates.json"), List.of(
                "#c:armor/chestplates",
                "#irons_spellbooks:armors/chestplates",
                "#minecraft:chest_armor",
                "#minecraft:enchantable/chest_armor",
                "#iss_magicfromtheeast:armors/chestplates"
        ));

        // 3. 盾牌
        writeTag(new File(tagDir, "shields.json"), List.of(
                "#c:tools/shield",
                "twilightforest:knightmetal_shield",
                "cataclysm:black_steel_targe",
                "cataclysm:azure_sea_shield"
        ));

        // 4. 剑
        writeTag(new File(tagDir, "swords.json"), List.of(
                "#c:tools/swords",
                "#minecraft:swords",
                "#simplyswords:diamond_gear",
                "#simplyswords:netherite_gear",
                "#simplyswords:iron_gear",
                "#simplyswords:gold_gear"
        ));

        // 5. 斧
        writeTag(new File(tagDir, "axes.json"), List.of(
                "#c:tools/axes",
                "#minecraft:axes",
                "#l2weaponry:battle_axe"
        ));

        // 6. 钝器
        writeTag(new File(tagDir, "blunt.json"), List.of(
                "#c:tools/mace",
                "#l2weaponry:hammer",
                "alexscaves:primitive_club"
        ));

        // 7. 长柄与矛
        writeTag(new File(tagDir, "polearms_spears.json"), List.of(
                "#c:tools/spear",
                "#c:tools/tridents",
                "#l2weaponry:spear",
                "#l2weaponry:javelin",
                "#simplyswords:spears"
        ));

        // 8. 匕首
        writeTag(new File(tagDir, "daggers.json"), List.of(
                "#l2weaponry:dagger",
                "alexscaves:desolate_dagger",
                "mowziesmobs:naga_fang_dagger"
        ));

        // 9. 拳爪
        writeTag(new File(tagDir, "claws.json"), List.of(
                "#l2weaponry:claw"
        ));

        // 10. 法杖/权杖
        writeTag(new File(tagDir, "staves.json"), List.of(
                "#irons_spellbooks:staff",
                "#iss_magicfromtheeast:staff",
                "alexscaves:sea_staff",
                "alexscaves:sugar_staff"
        ));

        // 11. 弓弩
        writeTag(new File(tagDir, "bows.json"), List.of(
                "#c:tools/bow",
                "#c:tools/crossbow",
                "#l2archery:bows",
                "#minecraft:bows"
        ));

        // 12. 双手巨剑
        writeTag(new File(tagDir, "two_handed_swords.json"), List.of(
                "#simplyswords:heavy_weapons",
                "irons_spellbooks:legionnaire_flamberge",
                "irons_spellbooks:keeper_flamberge"
        ));

        // 13. 近战武器合集
        writeTag(new File(tagDir, "melee.json"), List.of(
                "#examplemod:base/swords",
                "#examplemod:base/axes",
                "#examplemod:base/blunt",
                "#examplemod:base/polearms_spears",
                "#examplemod:base/daggers",
                "#examplemod:base/claws",
                "#examplemod:base/two_handed_swords"
        ));

        // 14. 武器全集
        writeTag(new File(tagDir, "all_weapons.json"), List.of(
                "#examplemod:base/melee",
                "#examplemod:base/staves",
                "#examplemod:base/bows"
        ));

        // 15. 【新增】：所有可用底板总合集，供通用锻造配方单一引用
        writeTag(new File(tagDir, "all.json"), List.of(
                "#examplemod:base/all_weapons",
                "#examplemod:base/helmets",
                "#examplemod:base/chestplates",
                "#examplemod:base/shields"
        ));
    }

    private static void generateexamplemodData(String root) {
        File wordsDir = new File(root, "data/examplemod/words");
        wordsDir.mkdirs();

        List<Runeword> list = buildAllexamplemod();
        for (Runeword rw : list) {
            String json = serializeRunewordToJson(rw);
            writeFile(new File(wordsDir, rw.id + ".json"), json);
        }
        System.out.println(">>> 成功导出全量 78 个纹饰之语 JSON 文件至 data/examplemod/words/");
    }

    private static String serializeRunewordToJson(Runeword rw) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(rw.id).append("\",\n");
        sb.append("  \"name_en\": \"").append(rw.nameEn).append("\",\n");
        sb.append("  \"name_cn\": \"").append(rw.nameCn).append("\",\n");
        sb.append("  \"sockets_required\": ").append(rw.runeSequence.size()).append(",\n");

        sb.append("  \"valid_base_tags\": [\n");
        for (int i = 0; i < rw.validBaseTags.size(); i++) {
            sb.append("    \"").append(rw.validBaseTags.get(i)).append("\"");
            if (i < rw.validBaseTags.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"sequence\": [\n");
        for (int i = 0; i < rw.runeSequence.size(); i++) {
            int runeId = rw.runeSequence.get(i);
            Rune r = RUNES.get(runeId);
            sb.append("    {\n");
            sb.append("      \"step\": ").append(i + 1).append(",\n");
            sb.append("      \"rune_id\": ").append(runeId).append(",\n");
            sb.append("      \"rune_name\": \"").append(r.name).append(" (").append(r.cnName).append(")\",\n");
            sb.append("      \"pattern\": \"").append(r.pattern).append("\",\n");
            sb.append("      \"material\": \"").append(r.material).append("\"\n");
            sb.append("    }");
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

    private static void writeTag(File file, List<String> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"replace\": false,\n  \"values\": [\n");
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

    // 组装全部 78 个纹饰之语数据库
    private static List<Runeword> buildAllexamplemod() {
        List<Runeword> list = new ArrayList<>();

        list.add(new Runeword("steel", "Steel", "钢铁",
                List.of("#examplemod:base/swords", "#examplemod:base/axes", "#examplemod:base/blunt"),
                List.of(3, 1),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 2.0, "ADDITION", "+2 物理点伤"),
                        new AttributeMod("apothic_attributes:armor_pierce", 3.0, "ADDITION", "撕裂流血"))));

        list.add(new Runeword("nadir", "Nadir", "天底",
                List.of("#examplemod:base/helmets"),
                List.of(4, 3),
                List.of(new AttributeMod("minecraft:generic.armor", 3.0, "ADDITION", "+3 护甲"),
                        new AttributeMod("minecraft:generic.knockback_resistance", 0.15, "ADDITION", "+15% 防退"),
                        new AttributeMod("apothic_attributes:experience_gained", -0.20, "MULTIPLY_BASE", "经验获取惩罚"))));

        list.add(new Runeword("malice", "Malice", "怨恨",
                List.of("#examplemod:base/melee"),
                List.of(6, 1, 5),
                List.of(new AttributeMod("apothic_attributes:armor_shred", 0.30, "ADDITION", "削减30%护甲"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADDITION", "撕裂伤口"),
                        new AttributeMod("l2damagetracker:regen", -0.25, "MULTIPLY_BASE", "抑制恢复"))));

        list.add(new Runeword("stealth", "Stealth", "隐秘",
                List.of("#examplemod:base/chestplates"),
                List.of(7, 5),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.25, "MULTIPLY_BASE", "+25% FRW 跑速"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.25, "MULTIPLY_BASE", "+25% 快速施法"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.20, "MULTIPLY_BASE", "+20% 回蓝"))));

        list.add(new Runeword("leaf", "Leaf", "叶子",
                List.of("#examplemod:base/staves"),
                List.of(3, 8),
                List.of(new AttributeMod("irons_spellbooks:fire_spell_power", 0.35, "MULTIPLY_BASE", "+35% 火焰技能"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.20, "MULTIPLY_BASE", "+20% 回蓝"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.35, "MULTIPLY_BASE", "+35% 火抗"))));

        list.add(new Runeword("ancients_pledge", "Ancients' Pledge", "古代人的誓约",
                List.of("#examplemod:base/shields"),
                List.of(8, 9, 7),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.45, "MULTIPLY_BASE", "+48% 全抗"),
                        new AttributeMod("l2weaponry:shield_defense", 10.0, "ADDITION", "+10 盾防"),
                        new AttributeMod("minecraft:generic.armor", 5.0, "ADDITION", "+5 护甲"))));

        list.add(new Runeword("zephyr", "Zephyr", "和风",
                List.of("#examplemod:base/bows"),
                List.of(9, 5),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.25, "MULTIPLY_BASE", "+25% 移速"),
                        new AttributeMod("apothic_attributes:arrow_velocity", 0.30, "MULTIPLY_BASE", "+30% 箭矢初速"),
                        new AttributeMod("apothic_attributes:draw_speed", 0.25, "MULTIPLY_BASE", "+25% 拉弓速度"))));

        list.add(new Runeword("holy_thunder", "Holy Thunder", "神圣雷击",
                List.of("#examplemod:base/staves", "#examplemod:base/blunt"),
                List.of(5, 8, 9, 7),
                List.of(new AttributeMod("irons_spellbooks:lightning_spell_power", 0.30, "MULTIPLY_BASE", "+30% 闪电法强"),
                        new AttributeMod("irons_spellbooks:lightning_magic_resist", 0.50, "MULTIPLY_BASE", "+50% 雷抗"),
                        new AttributeMod("minecraft:generic.attack_damage", 3.0, "ADDITION", "+3 点伤"))));

        list.add(new Runeword("pattern", "Pattern", "花样",
                List.of("#examplemod:base/melee", "#examplemod:base/claws"),
                List.of(7, 9, 10),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.20, "MULTIPLY_BASE", "+20% 攻速"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.12, "ADDITION", "+12% 暴击"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.15, "MULTIPLY_BASE", "+15% 抗性"))));

        list.add(new Runeword("myth", "Myth", "神话",
                List.of("#examplemod:base/chestplates"),
                List.of(15, 11, 4),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 3.0, "ADDITION", "+3 攻击力"),
                        new AttributeMod("minecraft:generic.max_health", 6.0, "ADDITION", "+6 生命"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.05, "MULTIPLY_BASE", "+5% 减伤"))));

        list.add(new Runeword("strength", "Strength", "力量",
                List.of("#examplemod:base/melee"),
                List.of(11, 3),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.05, "ADDITION", "25% 压碎打击"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADDITION", "7% 吸血"),
                        new AttributeMod("minecraft:generic.attack_damage", 3.5, "ADDITION", "+3.5 点伤"))));

        list.add(new Runeword("spirit_weapon", "Spirit (Weapon)", "精神 (武器)",
                List.of("#examplemod:base/swords", "#examplemod:base/staves"),
                List.of(7, 10, 9, 11),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.20, "MULTIPLY_BASE", "全技能+2"),
                        new AttributeMod("irons_spellbooks:cooldown_reduction", 0.25, "MULTIPLY_BASE", "35% 快速施法"),
                        new AttributeMod("irons_spellbooks:max_mana", 100.0, "ADDITION", "+100 法力上限"))));

        list.add(new Runeword("spirit_shield", "Spirit (Shield)", "精神 (盾牌)",
                List.of("#examplemod:base/shields"),
                List.of(7, 10, 9, 11),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.15, "MULTIPLY_BASE", "+15% 法强"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.35, "MULTIPLY_BASE", "冰雷毒抗性+35%"),
                        new AttributeMod("irons_spellbooks:max_mana", 100.0, "ADDITION", "+100 法力上限"))));

        list.add(new Runeword("kings_grace", "King's Grace", "国王的恩典",
                List.of("#examplemod:base/swords", "#examplemod:base/staves"),
                List.of(11, 8, 10),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 4.0, "ADDITION", "+4 攻击伤害"),
                        new AttributeMod("apothic_attributes:crit_damage", 0.35, "MULTIPLY_BASE", "+35% 暴击伤害"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADDITION", "7% 吸血"))));

        list.add(new Runeword("edge", "Edge", "边缘",
                List.of("#examplemod:base/bows"),
                List.of(3, 7, 11),
                List.of(new AttributeMod("apothic_attributes:draw_speed", 0.35, "MULTIPLY_BASE", "+35% 拉弓提速"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADDITION", "创口流血"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.10, "MULTIPLY_BASE", "荆棘光环减免"))));

        list.add(new Runeword("insight", "Insight", "眼光",
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/staves"),
                List.of(8, 3, 7, 12),
                List.of(new AttributeMod("irons_spellbooks:mana_regen", 0.80, "MULTIPLY_BASE", "+80% 冥思光环回蓝"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADDITION", "+20% 暴击率"),
                        new AttributeMod("minecraft:generic.attack_damage", 5.0, "ADDITION", "+5 物理伤害"))));

        list.add(new Runeword("honor", "Honor", "荣耀",
                List.of("#examplemod:base/melee"),
                List.of(11, 1, 6, 3, 12),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 5.0, "ADDITION", "+5 物理伤害"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.25, "ADDITION", "+25% 致命一击"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADDITION", "7% 吸血"),
                        new AttributeMod("l2damagetracker:regen", 0.30, "MULTIPLY_BASE", "+30% 自然恢复"))));

        list.add(new Runeword("radiance", "Radiance", "光辉",
                List.of("#examplemod:base/helmets"),
                List.of(4, 12, 6),
                List.of(new AttributeMod("minecraft:generic.armor", 4.0, "ADDITION", "+4 护甲"),
                        new AttributeMod("irons_spellbooks:max_mana", 50.0, "ADDITION", "+50 法力上限"),
                        new AttributeMod("minecraft:generic.max_absorption", 4.0, "ADDITION", "+4 伤害吸收"))));

        list.add(new Runeword("lore", "Lore", "学识",
                List.of("#examplemod:base/helmets"),
                List.of(9, 12),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.12, "MULTIPLY_BASE", "全技能+1"),
                        new AttributeMod("irons_spellbooks:lightning_magic_resist", 0.30, "MULTIPLY_BASE", "+30% 雷抗"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.15, "MULTIPLY_BASE", "+15% 回蓝"))));

        list.add(new Runeword("peace", "Peace", "和平",
                List.of("#examplemod:base/chestplates"),
                List.of(13, 10, 11),
                List.of(new AttributeMod("apothic_attributes:crit_chance", 0.15, "ADDITION", "+15% 致命一击"),
                        new AttributeMod("irons_spellbooks:summon_damage", 0.25, "MULTIPLY_BASE", "+25% 召唤伤害"),
                        new AttributeMod("minecraft:generic.armor", 4.0, "ADDITION", "+4 护甲"))));

        list.add(new Runeword("rhyme", "Rhyme", "韵律",
                List.of("#examplemod:base/shields"),
                List.of(13, 5),
                List.of(new AttributeMod("l2weaponry:reflect_time", 4.0, "ADDITION", "+40% 格挡提速"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.25, "MULTIPLY_BASE", "+25% 全抗"),
                        new AttributeMod("minecraft:generic.movement_speed", 0.10, "MULTIPLY_BASE", "绝对无法被冻结"))));

        list.add(new Runeword("authority", "Authority", "权威",
                List.of("#examplemod:base/chestplates"),
                List.of(15, 13, 8),
                List.of(new AttributeMod("irons_spellbooks:blood_spell_power", 0.25, "MULTIPLY_BASE", "+25% 鲜血法强"),
                        new AttributeMod("minecraft:generic.armor", 5.0, "ADDITION", "+5 护甲"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.20, "MULTIPLY_BASE", "+20% 火抗"))));

        list.add(new Runeword("white", "White", "白色",
                List.of("#examplemod:base/staves", "#examplemod:base/daggers"),
                List.of(14, 16),
                List.of(new AttributeMod("irons_spellbooks:eldritch_spell_power", 0.40, "MULTIPLY_BASE", "+40% 骨骼法强"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.20, "MULTIPLY_BASE", "+20% 施法提速"),
                        new AttributeMod("irons_spellbooks:max_mana", 80.0, "ADDITION", "+80 法力"))));

        list.add(new Runeword("temper", "Temper", "脾气",
                List.of("#examplemod:base/helmets"),
                List.of(13, 16, 8),
                List.of(new AttributeMod("irons_spellbooks:fire_magic_resist", 0.50, "MULTIPLY_BASE", "+50% 火抗"),
                        new AttributeMod("minecraft:generic.max_health", 5.0, "ADDITION", "+5 生命"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.08, "MULTIPLY_BASE", "+8% 减伤"))));

        list.add(new Runeword("heart", "Heart", "心灵",
                List.of("#examplemod:base/helmets"),
                List.of(13, 16, 10),
                List.of(new AttributeMod("irons_spellbooks:ice_magic_resist", 0.50, "MULTIPLY_BASE", "+50% 冰抗"),
                        new AttributeMod("minecraft:generic.max_health", 5.0, "ADDITION", "+5 生命"),
                        new AttributeMod("minecraft:generic.movement_speed", 0.10, "MULTIPLY_BASE", "无法冻结"))));

        list.add(new Runeword("ground", "Ground", "大地",
                List.of("#examplemod:base/helmets"),
                List.of(13, 16, 9),
                List.of(new AttributeMod("irons_spellbooks:lightning_magic_resist", 0.50, "MULTIPLY_BASE", "+50% 雷抗"),
                        new AttributeMod("minecraft:generic.max_health", 5.0, "ADDITION", "+5 生命"),
                        new AttributeMod("minecraft:generic.knockback_resistance", 0.2, "ADDITION", "+20% 防退"))));

        list.add(new Runeword("cure", "Cure", "治愈",
                List.of("#examplemod:base/helmets"),
                List.of(13, 16, 7),
                List.of(new AttributeMod("apothic_attributes:healing_received", 0.35, "MULTIPLY_BASE", "+35% 受治疗效果"),
                        new AttributeMod("l2damagetracker:regen", 0.40, "MULTIPLY_BASE", "+40% 快速自愈"),
                        new AttributeMod("minecraft:generic.max_health", 5.0, "ADDITION", "+5 生命"))));

        list.add(new Runeword("bulwark", "Bulwark", "壁垒",
                List.of("#examplemod:base/helmets"),
                List.of(13, 16, 12),
                List.of(new AttributeMod("l2damagetracker:damage_reduction", 0.15, "MULTIPLY_BASE", "15% 物理硬免伤"),
                        new AttributeMod("apothic_attributes:life_steal", 0.06, "ADDITION", "6% 击中偷取"),
                        new AttributeMod("minecraft:generic.max_health", 6.0, "ADDITION", "+6 生命"))));

        list.add(new Runeword("black", "Black", "黑色",
                List.of("#examplemod:base/blunt"),
                List.of(10, 16, 4),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.08, "ADDITION", "40% 压碎打击"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.15, "MULTIPLY_BASE", "+15% 攻速"),
                        new AttributeMod("minecraft:generic.attack_knockback", 1.5, "ADDITION", "+1.5 击退"))));

        list.add(new Runeword("smoke", "Smoke", "烟雾",
                List.of("#examplemod:base/chestplates"),
                List.of(4, 17),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.45, "MULTIPLY_BASE", "+50% 全元素抗性"),
                        new AttributeMod("minecraft:generic.armor", 6.0, "ADDITION", "+6 护甲"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.10, "ADDITION", "+10% 闪避率"))));

        list.add(new Runeword("splendor", "Splendor", "灿烂",
                List.of("#examplemod:base/shields"),
                List.of(5, 17),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.10, "MULTIPLY_BASE", "+10% 法强"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.30, "MULTIPLY_BASE", "+30% 回蓝速度"),
                        new AttributeMod("apothic_attributes:experience_gained", 0.30, "MULTIPLY_BASE", "+30% 打宝经验"))));

        list.add(new Runeword("memory", "Memory", "回忆",
                List.of("#examplemod:base/staves"),
                List.of(17, 16, 12, 5),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.25, "MULTIPLY_BASE", "法师全技能+3"),
                        new AttributeMod("irons_spellbooks:max_mana", 150.0, "ADDITION", "+150 法力上限"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.30, "MULTIPLY_BASE", "+30% 快速施法"))));

        list.add(new Runeword("melody", "Melody", "旋律",
                List.of("#examplemod:base/bows"),
                List.of(13, 18, 4),
                List.of(new AttributeMod("apothic_attributes:arrow_damage", 0.35, "MULTIPLY_BASE", "+35% 箭矢伤害"),
                        new AttributeMod("apothic_attributes:draw_speed", 0.25, "MULTIPLY_BASE", "+25% 拉弓提速"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.10, "ADDITION", "+10% 敏捷闪避"))));

        list.add(new Runeword("hysteria", "Hysteria", "歇斯底里",
                List.of("#examplemod:base/chestplates"),
                List.of(13, 18, 2),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.35, "MULTIPLY_BASE", "+35% 跑速爆发"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻击频率"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.15, "ADDITION", "+15% 闪避"))));

        list.add(new Runeword("mania", "Mania", "狂躁",
                List.of("#examplemod:base/melee"),
                List.of(13, 18, 2),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.35, "MULTIPLY_BASE", "+35% 极速连击"),
                        new AttributeMod("minecraft:generic.attack_damage", 4.0, "ADDITION", "+4 物理伤害"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.10, "ADDITION", "+10% 暴击"))));

        list.add(new Runeword("harmony", "Harmony", "和谐",
                List.of("#examplemod:base/bows"),
                List.of(3, 6, 12, 18),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.30, "MULTIPLY_BASE", "10级活力光环 +30% 移速"),
                        new AttributeMod("apothic_attributes:arrow_damage", 0.40, "MULTIPLY_BASE", "+40% 远程增强"),
                        new AttributeMod("apothic_attributes:projectile_damage", 0.25, "MULTIPLY_BASE", "+25% 穿刺"))));

        list.add(new Runeword("lionheart", "Lionheart", "狮心",
                List.of("#examplemod:base/chestplates"),
                List.of(15, 17, 19),
                List.of(new AttributeMod("minecraft:generic.max_health", 10.0, "ADDITION", "+10 生命上限"),
                        new AttributeMod("minecraft:generic.attack_damage", 4.0, "ADDITION", "+4 物理伤害"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.30, "MULTIPLY_BASE", "+30% 全抗"),
                        new AttributeMod("minecraft:generic.armor", 5.0, "ADDITION", "+5 护甲"))));

        list.add(new Runeword("unbending_will", "Unbending Will", "不屈意志",
                List.of("#examplemod:base/two_handed_swords"),
                List.of(19, 16, 6, 2, 1, 15),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 7.0, "ADDITION", "+7 巨剑重创"),
                        new AttributeMod("apothic_attributes:life_steal", 0.10, "ADDITION", "10% 吸血"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.10, "MULTIPLY_BASE", "10% 免伤"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻速"))));

        list.add(new Runeword("obedience", "Obedience", "顺从",
                List.of("#examplemod:base/polearms_spears"),
                List.of(15, 18, 10, 5, 19),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.08, "ADDITION", "40% 压碎打击"),
                        new AttributeMod("irons_spellbooks:fire_spell_power", 0.35, "MULTIPLY_BASE", "+35% 附魔火焰"),
                        new AttributeMod("apothic_attributes:armor_shred", 0.25, "ADDITION", "25% 破甲"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.30, "MULTIPLY_BASE", "+30% 全抗"))));

        list.add(new Runeword("wealth", "Wealth", "财富",
                List.of("#examplemod:base/chestplates"),
                List.of(20, 18, 3),
                List.of(new AttributeMod("apothic_attributes:experience_gained", 0.80, "MULTIPLY_BASE", "300% 极品掉落增益"),
                        new AttributeMod("l2hostility:extra_difficulty", 5.0, "ADDITION", "增加精英高爆率"))));

        list.add(new Runeword("treachery", "Treachery", "背叛",
                List.of("#examplemod:base/chestplates"),
                List.of(13, 10, 20),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.45, "MULTIPLY_BASE", "+45% 极速狂攻"),
                        new AttributeMod("artifacts:generic.invincibility_ticks", 12.0, "ADDITION", "能量消解无敌帧延长"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.15, "MULTIPLY_BASE", "15% 全域减伤"))));

        list.add(new Runeword("voice_of_reason", "Voice of Reason", "理性之声",
                List.of("#examplemod:base/swords", "#examplemod:base/blunt"),
                List.of(20, 18, 1, 2),
                List.of(new AttributeMod("irons_spellbooks:ice_spell_power", 0.35, "MULTIPLY_BASE", "+35% 寒冰法强"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.15, "ADDITION", "+15% 暴击"),
                        new AttributeMod("minecraft:generic.movement_speed", 0.10, "MULTIPLY_BASE", "绝对无法被冻结"))));

        list.add(new Runeword("passion", "Passion", "热情",
                List.of("#examplemod:base/melee"),
                List.of(14, 9, 2, 20),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.30, "MULTIPLY_BASE", "触发热诚连击 +30% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 5.0, "ADDITION", "+5 点伤"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADDITION", "+20% 暴击"))));

        list.add(new Runeword("lawbringer", "Lawbringer", "执法者",
                List.of("#examplemod:base/swords", "#examplemod:base/staves"),
                List.of(11, 20, 18),
                List.of(new AttributeMod("apothic_attributes:armor_shred", 0.45, "ADDITION", "衰老破除45%护甲"),
                        new AttributeMod("irons_spellbooks:holy_spell_power", 0.35, "MULTIPLY_BASE", "庇护所神圣增伤"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADDITION", "7% 吸血"))));

        list.add(new Runeword("enlightenment", "Enlightenment", "启蒙",
                List.of("#examplemod:base/chestplates"),
                List.of(21, 8, 12),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.20, "MULTIPLY_BASE", "+20% 魔法伤害"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.40, "MULTIPLY_BASE", "+40% 回蓝速度"),
                        new AttributeMod("minecraft:generic.armor", 5.0, "ADDITION", "+5 护甲"))));

        list.add(new Runeword("wisdom", "Wisdom", "智慧",
                List.of("#examplemod:base/helmets"),
                List.of(21, 6, 2),
                List.of(new AttributeMod("apothic_attributes:arrow_damage", 0.30, "MULTIPLY_BASE", "33% 穿透箭矢增益"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.35, "MULTIPLY_BASE", "+35% 回蓝"),
                        new AttributeMod("minecraft:generic.movement_speed", 0.10, "MULTIPLY_BASE", "无法冻结"))));

        list.add(new Runeword("stone", "Stone", "石块",
                List.of("#examplemod:base/chestplates"),
                List.of(13, 22, 21, 17),
                List.of(new AttributeMod("minecraft:generic.armor", 14.0, "ADDITION", "+14 钢铁重甲"),
                        new AttributeMod("minecraft:generic.armor_toughness", 8.0, "ADDITION", "+8 盔甲韧性"),
                        new AttributeMod("minecraft:generic.knockback_resistance", 0.4, "ADDITION", "+40% 防击退"))));

        list.add(new Runeword("gloom", "Gloom", "幽暗",
                List.of("#examplemod:base/chestplates"),
                List.of(19, 22, 21),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.45, "MULTIPLY_BASE", "+45% 全抗"),
                        new AttributeMod("minecraft:generic.armor", 9.0, "ADDITION", "+9 护甲"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.15, "ADDITION", "+15% 闪避"))));

        list.add(new Runeword("duress", "Duress", "强制",
                List.of("#examplemod:base/chestplates"),
                List.of(13, 22, 10),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.04, "ADDITION", "15% 压碎打击"),
                        new AttributeMod("apothic_attributes:crit_damage", 0.35, "MULTIPLY_BASE", "+35% 暴击伤害"),
                        new AttributeMod("apothic_attributes:armor_pierce", 4.0, "ADDITION", "33% 撕裂创口"),
                        new AttributeMod("minecraft:generic.armor", 7.0, "ADDITION", "+7 护甲"))));

        list.add(new Runeword("bone", "Bone", "白骨",
                List.of("#examplemod:base/chestplates"),
                List.of(12, 22, 22),
                List.of(new AttributeMod("irons_spellbooks:eldritch_spell_power", 0.35, "MULTIPLY_BASE", "死灵法术+35%"),
                        new AttributeMod("minecraft:generic.max_absorption", 8.0, "ADDITION", "被击触发骨甲吸收盾"),
                        new AttributeMod("irons_spellbooks:max_mana", 120.0, "ADDITION", "+120 法力上限"))));

        list.add(new Runeword("crescent_moon", "Crescent Moon", "新月",
                List.of("#examplemod:base/axes", "#examplemod:base/swords", "#examplemod:base/polearms_spears"),
                List.of(13, 22, 3),
                List.of(new AttributeMod("irons_spellbooks:lightning_spell_power", 0.40, "MULTIPLY_BASE", "削减敌方35%电抗"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻速"),
                        new AttributeMod("apothic_attributes:armor_pierce", 5.0, "ADDITION", "+5 护甲穿透"))));

        list.add(new Runeword("rain", "Rain", "雨云",
                List.of("#examplemod:base/chestplates"),
                List.of(9, 23, 6),
                List.of(new AttributeMod("irons_spellbooks:nature_spell_power", 0.35, "MULTIPLY_BASE", "自然德鲁伊技能+35%"),
                        new AttributeMod("irons_spellbooks:max_mana", 150.0, "ADDITION", "+150 法力"),
                        new AttributeMod("irons_spellbooks:lightning_magic_resist", 0.40, "MULTIPLY_BASE", "+40% 雷抗"))));

        list.add(new Runeword("prudence", "Prudence", "审慎",
                List.of("#examplemod:base/chestplates"),
                List.of(23, 3),
                List.of(new AttributeMod("minecraft:generic.armor", 8.0, "ADDITION", "+8 护甲"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.35, "MULTIPLY_BASE", "+35% 全抗"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.08, "MULTIPLY_BASE", "+8% 纯减伤"))));

        list.add(new Runeword("sanctuary", "Sanctuary", "圣堂",
                List.of("#examplemod:base/shields"),
                List.of(18, 18, 23),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.55, "MULTIPLY_BASE", "+70% 极高全抗"),
                        new AttributeMod("l2weaponry:shield_defense", 16.0, "ADDITION", "+16 盾防"),
                        new AttributeMod("l2weaponry:reflect_time", 3.0, "ADDITION", "+3 弹反窗口"))));

        list.add(new Runeword("venom", "Venom", "毒液",
                List.of("#examplemod:base/all_weapons"),
                List.of(7, 14, 23),
                List.of(new AttributeMod("apothic_attributes:armor_shred", 0.35, "ADDITION", "毒爆无视35%防御"),
                        new AttributeMod("irons_spellbooks:nature_spell_power", 0.40, "MULTIPLY_BASE", "+40% 剧毒伤害"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.05, "ADDITION", "腐蚀毒创"))));

        list.add(new Runeword("oath", "Oath", "誓约",
                List.of("#examplemod:base/swords", "#examplemod:base/axes", "#examplemod:base/blunt"),
                List.of(13, 21, 23, 17),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.45, "MULTIPLY_BASE", "+50% 爆发攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 8.0, "ADDITION", "+8 点伤"),
                        new AttributeMod("apothic_attributes:crit_damage", 0.45, "MULTIPLY_BASE", "+45% 暴击伤害"))));

        list.add(new Runeword("coven", "Coven", "女巫集会",
                List.of("#examplemod:base/helmets"),
                List.of(24, 8, 16),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.20, "MULTIPLY_BASE", "+20% 法术强度"),
                        new AttributeMod("irons_spellbooks:cast_time_reduction", 0.25, "MULTIPLY_BASE", "+25% 快速施法"),
                        new AttributeMod("apothic_attributes:experience_gained", 0.40, "MULTIPLY_BASE", "+40% 经验打宝"))));

        list.add(new Runeword("delirium", "Delirium", "谵妄",
                List.of("#examplemod:base/helmets"),
                List.of(20, 24, 16),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.25, "MULTIPLY_BASE", "全技能+2"),
                        new AttributeMod("apothic_attributes:dodge_chance", 0.20, "ADDITION", "致幻迷乱 +20% 闪避"),
                        new AttributeMod("apothic_attributes:experience_gained", 0.50, "MULTIPLY_BASE", "+50% 寻宝加成"))));

        list.add(new Runeword("vigilance", "Vigilance", "警惕",
                List.of("#examplemod:base/shields"),
                List.of(14, 25),
                List.of(new AttributeMod("minecraft:generic.armor", 7.0, "ADDITION", "+7 护甲"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.35, "MULTIPLY_BASE", "+35% 全抗"),
                        new AttributeMod("minecraft:generic.max_health", 8.0, "ADDITION", "+8 生命"))));

        list.add(new Runeword("principle", "Principle", "原则",
                List.of("#examplemod:base/chestplates"),
                List.of(8, 25, 2),
                List.of(new AttributeMod("irons_spellbooks:holy_spell_power", 0.35, "MULTIPLY_BASE", "圣骑士神圣技能+35%"),
                        new AttributeMod("minecraft:generic.max_health", 6.0, "ADDITION", "+6 生命"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.35, "MULTIPLY_BASE", "+35% 火抗"))));

        list.add(new Runeword("rift", "Rift", "裂隙",
                List.of("#examplemod:base/polearms_spears", "#examplemod:base/staves"),
                List.of(15, 18, 20, 25),
                List.of(new AttributeMod("irons_spellbooks:ice_spell_power", 0.35, "MULTIPLY_BASE", "击中释放冰封球"),
                        new AttributeMod("irons_spellbooks:fire_spell_power", 0.35, "MULTIPLY_BASE", "+35% 火系法强"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADDITION", "+20% 暴击率"))));

        list.add(new Runeword("mosaic", "Mosaic", "马赛克",
                List.of("#examplemod:base/claws"),
                List.of(23, 25, 11),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.30, "MULTIPLY_BASE", "+30% 极速充能攻速"),
                        new AttributeMod("irons_spellbooks:ice_spell_power", 0.25, "MULTIPLY_BASE", "元素积蓄无消耗释放"),
                        new AttributeMod("irons_spellbooks:lightning_spell_power", 0.25, "MULTIPLY_BASE", "+25% 电系法强"),
                        new AttributeMod("irons_spellbooks:fire_spell_power", 0.25, "MULTIPLY_BASE", "+25% 火系法强"),
                        new AttributeMod("apothic_attributes:life_steal", 0.07, "ADDITION", "7% 吸血"))));

        list.add(new Runeword("kingslayer", "Kingslayer", "弑君者",
                List.of("#examplemod:base/swords", "#examplemod:base/axes"),
                List.of(23, 22, 25, 19),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.07, "ADDITION", "33% 压碎打击"),
                        new AttributeMod("apothic_attributes:armor_shred", 0.30, "ADDITION", "30% 剥防撕裂"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADDITION", "+20% 致命一击"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.30, "MULTIPLY_BASE", "+30% 攻速"))));

        list.add(new Runeword("silence", "Silence", "寂静",
                List.of("#examplemod:base/all_weapons"),
                List.of(14, 2, 15, 24, 3, 26),
                List.of(new AttributeMod("irons_spellbooks:spell_resist", 0.65, "MULTIPLY_BASE", "+75% 极高全抗"),
                        new AttributeMod("irons_spellbooks:spell_power", 0.20, "MULTIPLY_BASE", "全技能+2"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.25, "MULTIPLY_BASE", "+25% 攻速"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.45, "MULTIPLY_BASE", "+45% 回蓝"))));

        list.add(new Runeword("heart_of_the_oak", "Heart of the Oak", "橡树之心",
                List.of("#examplemod:base/staves", "#examplemod:base/blunt"),
                List.of(18, 26, 21, 10),
                List.of(new AttributeMod("irons_spellbooks:spell_power", 0.35, "MULTIPLY_BASE", "全法系技能+3"),
                        new AttributeMod("irons_spellbooks:cooldown_reduction", 0.35, "MULTIPLY_BASE", "+40% 快速施法"),
                        new AttributeMod("irons_spellbooks:max_mana", 180.0, "ADDITION", "+180 法力上限"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.40, "MULTIPLY_BASE", "+40% 全抗"))));

        list.add(new Runeword("death", "Death", "死亡",
                List.of("#examplemod:base/swords", "#examplemod:base/axes"),
                List.of(15, 1, 26, 9, 25),
                List.of(new AttributeMod("apothic_attributes:current_hp_damage", 0.10, "ADDITION", "50% 压碎打击"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.35, "ADDITION", "+35% 致命一击"),
                        new AttributeMod("apothic_attributes:crit_damage", 0.60, "MULTIPLY_BASE", "+60% 暴击伤害"),
                        new AttributeMod("minecraft:generic.attack_damage", 8.0, "ADDITION", "+8 物理硬伤害"))));

        list.add(new Runeword("flickering_flame", "Flickering Flame", "摇曳焰火",
                List.of("#examplemod:base/helmets"),
                List.of(4, 21, 26),
                List.of(new AttributeMod("irons_spellbooks:fire_spell_power", 0.45, "MULTIPLY_BASE", "火焰技能+3"),
                        new AttributeMod("irons_spellbooks:fire_magic_resist", 0.65, "MULTIPLY_BASE", "抗火光环 +65% 火抗"),
                        new AttributeMod("irons_spellbooks:mana_cost_reduction", 0.20, "MULTIPLY_BASE", "-20% 火系施法耗蓝"))));

        list.add(new Runeword("ritual", "Ritual", "仪式",
                List.of("#examplemod:base/daggers"),
                List.of(11, 13, 27),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.40, "MULTIPLY_BASE", "+40% 极速刺杀"),
                        new AttributeMod("minecraft:generic.attack_damage", 7.0, "ADDITION", "+7 物理伤害"),
                        new AttributeMod("apothic_attributes:life_steal", 0.10, "ADDITION", "10% 生命吸取"))));

        list.add(new Runeword("exile", "Exile", "流亡",
                List.of("#examplemod:base/shields"),
                List.of(26, 27, 24, 14),
                List.of(new AttributeMod("apothic_attributes:life_steal", 0.15, "ADDITION", "Life Tap 击中15%偷取生命"),
                        new AttributeMod("minecraft:generic.armor", 12.0, "ADDITION", "反抗光环 +12 护甲"),
                        new AttributeMod("l2weaponry:shield_defense", 22.0, "ADDITION", "+22 绝对盾防"),
                        new AttributeMod("minecraft:generic.armor_toughness", 8.0, "ADDITION", "+8 盔甲韧性"))));

        list.add(new Runeword("chaos", "Chaos", "混沌",
                List.of("#examplemod:base/claws"),
                List.of(19, 27, 22),
                List.of(new AttributeMod("minecraft:player.sweeping_damage_ratio", 0.60, "ADDITION", "白嫖刺客旋风斩 Whirlwind"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.35, "MULTIPLY_BASE", "+35% 攻速"),
                        new AttributeMod("apothic_attributes:armor_pierce", 7.0, "ADDITION", "+7 护甲贯通"))));

        list.add(new Runeword("call_to_arms", "Call to Arms", "战争召唤",
                List.of("#examplemod:base/all_weapons"),
                List.of(11, 8, 23, 24, 27),
                List.of(new AttributeMod("minecraft:generic.max_health", 12.0, "ADDITION", "战斗体质 BO +12 生命"),
                        new AttributeMod("irons_spellbooks:max_mana", 120.0, "ADDITION", "+120 最大法力"),
                        new AttributeMod("irons_spellbooks:spell_power", 0.15, "MULTIPLY_BASE", "战斗指挥 全技能+1"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.30, "MULTIPLY_BASE", "+30% 攻击速度"))));

        list.add(new Runeword("fortitude_armor", "Fortitude (Armor)", "刚毅 (防具)",
                List.of("#examplemod:base/chestplates"),
                List.of(1, 12, 14, 28),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 8.0, "ADDITION", "+300% 物理硬增伤"),
                        new AttributeMod("minecraft:generic.armor", 12.0, "ADDITION", "寒冰装甲 +12 护甲"),
                        new AttributeMod("minecraft:generic.max_health", 15.0, "ADDITION", "+15 生命上限"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.35, "MULTIPLY_BASE", "+35% 全抗"))));

        list.add(new Runeword("fortitude_weapon", "Fortitude (Weapon)", "刚毅 (武器)",
                List.of("#examplemod:base/all_weapons"),
                List.of(1, 12, 14, 28),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 14.0, "ADDITION", "+300% 恐怖增伤"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADDITION", "+20% 致命一击"),
                        new AttributeMod("irons_spellbooks:spell_resist", 0.30, "MULTIPLY_BASE", "+30% 全元素抗性"))));

        list.add(new Runeword("grief", "Grief", "悔恨",
                List.of("#examplemod:base/swords", "#examplemod:base/axes"),
                List.of(5, 3, 28, 23, 8),
                List.of(new AttributeMod("minecraft:generic.attack_damage", 18.0, "ADDITION", "+400 点伤之王"),
                        new AttributeMod("minecraft:generic.attack_speed", 0.40, "MULTIPLY_BASE", "+40% 狂暴攻速"),
                        new AttributeMod("apothic_attributes:armor_shred", 0.40, "ADDITION", "无视40%目标护甲"),
                        new AttributeMod("apothic_attributes:crit_chance", 0.20, "ADDITION", "+20% 致命一击"))));

        list.add(new Runeword("bramble", "Bramble", "荆棘",
                List.of("#examplemod:base/chestplates"),
                List.of(8, 27, 29, 5),
                List.of(new AttributeMod("irons_spellbooks:nature_spell_power", 0.55, "MULTIPLY_BASE", "+55% 剧毒法强提升"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.15, "MULTIPLY_BASE", "+15% 反伤免伤"),
                        new AttributeMod("irons_spellbooks:mana_regen", 0.35, "MULTIPLY_BASE", "+35% 回蓝"))));

        list.add(new Runeword("beast", "Beast", "狂暴",
                List.of("#examplemod:base/axes", "#examplemod:base/blunt"),
                List.of(30, 3, 22, 23, 17),
                List.of(new AttributeMod("minecraft:generic.attack_speed", 0.40, "MULTIPLY_BASE", "9级狂热光环 +40% 攻速"),
                        new AttributeMod("minecraft:generic.attack_damage", 8.0, "ADDITION", "+8 物理点伤"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.05, "ADDITION", "20% 压碎打击"),
                        new AttributeMod("minecraft:generic.max_health", 12.0, "ADDITION", "变身熊人 +12 生命"))));

        list.add(new Runeword("infinity", "Infinity", "无限",
                List.of("#examplemod:base/polearms_spears"),
                List.of(30, 23, 30, 24),
                List.of(new AttributeMod("irons_spellbooks:lightning_spell_power", 0.60, "MULTIPLY_BASE", "12级信念破免光环"),
                        new AttributeMod("apothic_attributes:current_hp_damage", 0.08, "ADDITION", "40% 压碎打击"),
                        new AttributeMod("minecraft:generic.movement_speed", 0.25, "MULTIPLY_BASE", "+25% 跑速"),
                        new AttributeMod("apothic_attributes:armor_pierce", 8.0, "ADDITION", "+8 绝对护甲贯通"))));

        list.add(new Runeword("enigma", "Enigma", "谜团",
                List.of("#examplemod:base/chestplates"),
                List.of(31, 6, 30),
                List.of(new AttributeMod("minecraft:generic.movement_speed", 0.45, "MULTIPLY_BASE", "+45% 跑速"),
                        new AttributeMod("irons_spellbooks:spell_power", 0.25, "MULTIPLY_BASE", "全技能+2"),
                        new AttributeMod("l2damagetracker:damage_reduction", 0.08, "MULTIPLY_BASE", "8% 物理硬减伤"),
                        new AttributeMod("minecraft:generic.max_health", 12.0, "ADDITION", "+12 巨额生命"),
                        new AttributeMod("irons_spellbooks:mana_cost_reduction", 0.15, "MULTIPLY_BASE", "传送闪烁能耗降低"),
                        new AttributeMod("apothic_attributes:experience_gained", 0.60, "MULTIPLY_BASE", "+60% 极高寻宝增益"))));

        return list;
    }
}