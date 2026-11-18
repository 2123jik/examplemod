package com.example.examplemod.util;

import com.google.gson.*;
import com.example.examplemod.ExampleMod;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SpellAffixGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * 进游戏后调用此方法，即可导出全部法术词缀 JSON +（可选）中英文翻译文件
     */
    public static void generateAll(Path outputRoot) {
        try {
            // 1. 读取所有相关模组资源里的 en_us.json 和 zh_cn.json（客户端可用）
            Map<String, String> enMap = loadAllLangEntries("en_us");
            Map<String, String> zhMap = loadAllLangEntries("zh_cn");

            JsonObject enAffixLang = new JsonObject();
            JsonObject zhAffixLang = new JsonObject();

            // 写入 pack.mcmeta，方便直接当作数据包丢进存档 datapacks/ 测试
            Path packMetaPath = outputRoot.resolve("pack.mcmeta");
            Files.createDirectories(packMetaPath.getParent());
            JsonObject packMeta = new JsonObject();
            JsonObject pack = new JsonObject();
            pack.addProperty("pack_format", 48); // 1.21.1 数据包格式版本
            pack.addProperty("description", "Generated Spell Level Affixes");
            packMeta.add("pack", pack);
            Files.writeString(packMetaPath, GSON.toJson(packMeta), StandardCharsets.UTF_8);

            // 2. 遍历 SpellRegistry（请在 IDEA 点开 SpellRegistry 核对方法名，通常为 getEnabledSpells() 或 REGISTRY）
            for (AbstractSpell spell : SpellRegistry.getEnabledSpells()) {
                if (spell == null || spell == SpellRegistry.none()) continue;

                ResourceLocation spellId = spell.getSpellResource();
                String langKey = spell.getComponentId(); // "spell.<ns>.<spell_name>"

                // 获取该法术的英文名与中文名（兜底用当前 I18n 或路径名）
                String enName = enMap.getOrDefault(langKey, I18n.get(langKey));
                String zhName = zhMap.getOrDefault(langKey, I18n.get(langKey));

                // 输出路径: data/examplemod/affixes/spell_level/<namespace>/<spell_name>.json
                // 注意：避免不同模组重名，按命名空间分文件夹
                Path affixPath = outputRoot
                        .resolve("data")
                        .resolve(ExampleMod.MODID)
                        .resolve("affixes")
                        .resolve("spell_level")
                        .resolve(spellId.getNamespace())
                        .resolve(spellId.getPath() + ".json");

                Files.createDirectories(affixPath.getParent());
                JsonObject affixJson = buildSpellAffixJson(spellId);
                Files.writeString(affixPath, GSON.toJson(affixJson), StandardCharsets.UTF_8);

                // （可选）如果你也想同时生成每条词缀的独立翻译键来彻底消除开发环境警告：
                String affixId = ExampleMod.MODID + ":spell_level/" + spellId.getNamespace() + "/" + spellId.getPath();
                enAffixLang.addProperty("affix." + affixId, enName + "'s");
                enAffixLang.addProperty("affix." + affixId + ".suffix", "of " + enName);
                enAffixLang.addProperty("affix." + affixId + ".desc", "+%s " + enName + " Level");

                zhAffixLang.addProperty("affix." + affixId, zhName + "之");
                zhAffixLang.addProperty("affix." + affixId + ".suffix", zhName);
                zhAffixLang.addProperty("affix." + affixId + ".desc", "+%s " + zhName + " 等级");
            }

            // 写出可选的独立语言文件到 assets/examplemod/lang/
            Path langDir = outputRoot.resolve("assets").resolve(ExampleMod.MODID).resolve("lang");
            Files.createDirectories(langDir);
            Files.writeString(langDir.resolve("en_us.json"), GSON.toJson(enAffixLang), StandardCharsets.UTF_8);
            Files.writeString(langDir.resolve("zh_cn.json"), GSON.toJson(zhAffixLang), StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 完全对齐你贴出的 Apotheosis 8.x JSON 格式
     */
    private static JsonObject buildSpellAffixJson(ResourceLocation spellId) {
        JsonObject root = new JsonObject();
        root.addProperty("type", ExampleMod.MODID + ":spell_level");
        root.addProperty("spell", spellId.toString());

        JsonArray categories = new JsonArray();
        categories.add("apotheosis:helmet");
        categories.add("apotheosis:chestplate");
        categories.add("apotheosis:leggings");
        categories.add("apotheosis:boots");
        categories.add("apotheosis:melee_weapon");
        categories.add("apotheosis:bow");
        categories.add("apotheosis:trident");
        categories.add("irons_apothic:staff");
        categories.add("irons_apothic:spellbook");
        root.add("categories", categories);

        JsonObject definition = new JsonObject();
        definition.addProperty("affix_type", "stat");
        definition.add("exclusive_set", new JsonArray());

        JsonObject weights = new JsonObject();
        weights.addProperty("quality", 0.1);
        weights.addProperty("weight", 20);
        definition.add("weights", weights);
        root.add("definition", definition);

        JsonObject values = new JsonObject();
        values.add("apotheosis:uncommon", stepRange(1, 1, 1));
        values.add("apotheosis:rare",     stepRange(1, 2, 1));
        values.add("apotheosis:epic",     stepRange(1, 3, 1));
        values.add("apotheosis:mythic",   stepRange(2, 4, 1));
        root.add("values", values);

        return root;
    }

    private static JsonObject stepRange(int min, int max, int step) {
        JsonObject obj = new JsonObject();
        obj.addProperty("min", min);
        obj.addProperty("max", max);
        obj.addProperty("step", step);
        return obj;
    }

    /**
     * 直接读取所有已加载模组中指定语言文件（例如 "en_us" / "zh_cn"）里的全部翻译键值对
     */
    private static Map<String, String> loadAllLangEntries(String locale) {
        Map<String, String> result = new HashMap<>();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();

        for (String namespace : rm.getNamespaces()) {
            ResourceLocation langLoc = ResourceLocation.fromNamespaceAndPath(namespace, "lang/" + locale + ".json");
            for (Resource res : rm.getResourceStack(langLoc)) {
                try (var reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : obj.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            result.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    /**
     * 便捷默认输出路径：游戏根目录/generated_spell_affixes
     */
    public static Path defaultOutputDir() {
        return FMLPaths.GAMEDIR.get().resolve("generated_spell_affixes");
    }
}