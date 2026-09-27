package com.example.examplemod.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;

public class ModpackDataDumper {

    // 你的 PrismLauncher mods 路径
    private static final String MODS_PATH = "C:/Users/lyj/AppData/Roaming/PrismLauncher/instances/1.21.1/minecraft/mods";
    private static final File OUT_DIR = new File("loot_foundation");

    public static void main(String[] args) {
        File modsDir = new File(MODS_PATH);
        if (!modsDir.exists()) {
            System.err.println("错误：找不到 mods 目录，请检查路径: " + MODS_PATH);
            return;
        }

        OUT_DIR.mkdirs();
        System.out.println(">>> 开始全量抽取 220+ 模组的战利品与装备地基数据...");

        // 1. 战利品表收集
        TreeSet<String> chestTables = new TreeSet<>();
        TreeSet<String> entityTables = new TreeSet<>();
        TreeSet<String> otherLootTables = new TreeSet<>();

        // 2. Curios 槽位映射：slotName -> Set<itemId>
        Map<String, TreeSet<String>> curiosSlots = new TreeMap<>();

        // 3. 核心 RPG 道具按类收集
        TreeSet<String> weapons = new TreeSet<>();
        TreeSet<String> magicScrolls = new TreeSet<>();
        TreeSet<String> gemsAndMats = new TreeSet<>();
        TreeSet<String> artifacts = new TreeSet<>();

        // 4. 全量物品名册：modId -> Set<itemId>
        Map<String, TreeSet<String>> allModItems = new TreeMap<>();

        File[] jars = modsDir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jars == null) return;

        for (File jar : jars) {
            try (JarFile jarFile = new JarFile(jar)) {
                jarFile.stream().forEach(entry -> {
                    String name = entry.getName();

                    // ==========================================
                    // A. 抓取所有战利品表 (Loot Tables)
                    // ==========================================
                    if (name.startsWith("data/") && (name.contains("/loot_table/") || name.contains("/loot_tables/")) && name.endsWith(".json")) {
                        String id = parseResourceLocation(name, "loot_tables?");
                        if (id != null) {
                            if (id.contains("chests/")) {
                                chestTables.add(id);
                            } else if (id.contains("entities/")) {
                                entityTables.add(id);
                            } else {
                                otherLootTables.add(id);
                            }
                        }
                    }

                    // ==========================================
                    // B. 抓取 Curios 槽位与饰品映射 (Curios Tags)
                    // ==========================================
                    if (name.startsWith("data/") && name.matches("^data/[^/]+/tags/items?/curios/.*\\.json$")) {
                        String slotName = name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
                        try (var reader = new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                            JsonElement json = JsonParser.parseReader(reader);
                            if (json.isJsonObject() && json.getAsJsonObject().has("values")) {
                                JsonArray vals = json.getAsJsonObject().getAsJsonArray("values");
                                for (JsonElement v : vals) {
                                    String item = v.getAsString();
                                    if (!item.startsWith("#")) { // 过滤嵌套tag
                                        curiosSlots.computeIfAbsent(slotName, k -> new TreeSet<>()).add(item);
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // ==========================================
                    // C. 抓取全量物品名册与 RPG 核心道具 (Item Models)
                    // ==========================================
                    if (name.startsWith("assets/") && name.contains("/models/item/") && name.endsWith(".json")) {
                        String[] parts = name.split("/");
                        if (parts.length >= 5) {
                            String namespace = parts[1];
                            String itemPath = parts[4].substring(0, parts[4].length() - 5);
                            String itemId = namespace + ":" + itemPath;

                            allModItems.computeIfAbsent(namespace, k -> new TreeSet<>()).add(itemId);

                            // 武器类识别 (Simply Swords, Cataclysm, Miracle, Silent Gear)
                            if (namespace.equals("simplyswords") || namespace.equals("weapons_of_miracle") 
                                    || (namespace.equals("cataclysm") && itemId.matches(".*(sword|blade|axe|spear|hammer|bow|shield|claymore).*"))) {
                                weapons.add(itemId);
                            }

                            // 魔法、卷轴与法典 (Iron's Spells, Apprentice's Codex)
                            if ((namespace.equals("irons_spellbooks") || namespace.equals("apprentices_codex")) 
                                    && itemId.matches(".*(scroll|spell_book|ink|tome|focus|staff).*")) {
                                magicScrolls.add(itemId);
                            }

                            // 宝石、词缀材料与强化石 (Apotheosis, Celestial Forge, Iron's Jewelry)
                            if ((namespace.equals("apotheosis") && itemId.matches(".*(gem|scrap|dust|tome).*"))
                                    || (namespace.equals("celestial_forge") && itemId.matches(".*(reinforce|stone|material).*"))
                                    || namespace.equals("irons_jewelry")) {
                                gemsAndMats.add(itemId);
                            }

                            // 背包道具、圣遗物与随身工具 (Reliquary, Waystones, Artifacts)
                            if (namespace.equals("reliquary") || namespace.equals("waystones") || namespace.equals("artifacts")) {
                                artifacts.add(itemId);
                            }
                        }
                    }
                });
            } catch (Exception ignored) {}
        }

        // ==========================================
        // D. 统一写入文件
        // ==========================================
        writeToFile("01_chests_loot_tables.txt", chestTables);
        writeToFile("02_entity_boss_loot_tables.txt", entityTables);
        writeToFile("03_other_loot_tables.txt", otherLootTables);

        // Curios 分槽位输出
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(OUT_DIR, "04_curios_by_slot.txt")))) {
            curiosSlots.forEach((slot, items) -> {
                pw.println("========== 槽位: [" + slot + "] (" + items.size() + " 件) ==========");
                items.forEach(pw::println);
                pw.println();
            });
        } catch (Exception e) { e.printStackTrace(); }

        // RPG 核心支柱输出
        writeToFile("05_weapons.txt", weapons);
        writeToFile("06_magic_scrolls.txt", magicScrolls);
        writeToFile("07_gems_and_reinforce_mats.txt", gemsAndMats);
        writeToFile("08_inventory_artifacts.txt", artifacts);

        // 全量名册输出（按 Mod 分组）
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(OUT_DIR, "09_all_items_catalog.txt")))) {
            allModItems.forEach((mod, items) -> {
                pw.println("========== MOD: " + mod + " (" + items.size() + " 个物品) ==========");
                items.forEach(pw::println);
                pw.println();
            });
        } catch (Exception e) { e.printStackTrace(); }

        System.out.println(">>> 导出大功告成！文件均已保存至: " + OUT_DIR.getAbsolutePath());
    }

    private static String parseResourceLocation(String fullPath, String regex) {
        String[] parts = fullPath.split("/");
        if (parts.length >= 4) {
            String namespace = parts[1];
            String sub = fullPath.replaceFirst("^data/[^/]+/" + regex + "/", "");
            return namespace + ":" + sub.substring(0, sub.length() - 5);
        }
        return null;
    }

    private static void writeToFile(String filename, Collection<String> lines) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(OUT_DIR, filename)))) {
            lines.forEach(pw::println);
        } catch (Exception e) { e.printStackTrace(); }
    }
}