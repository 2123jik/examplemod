package com.example.examplemod.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.TreeSet;
import java.util.jar.JarFile;

public class OfflineLootTableDumper {
    public static void main(String[] args) {
        // 你的 mods 路径
        File modsDir = new File("C:/Users/lyj/AppData/Roaming/PrismLauncher/instances/1.21.1/minecraft/mods");
        File outFile = new File("all_chests.txt");

        TreeSet<String> chestTables = new TreeSet<>();
        File[] jars = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));

        if (jars != null) {
            for (File jar : jars) {
                try (JarFile jarFile = new JarFile(jar)) {
                    jarFile.stream().forEach(entry -> {
                        String name = entry.getName();
                        // 匹配 data/<namespace>/loot_table(s)/.../chests/...json
                        if (name.startsWith("data/") && (name.contains("/loot_table/") || name.contains("/loot_tables/"))
                                && name.contains("/chests/") && name.endsWith(".json")) {

                            String[] parts = name.split("/");
                            if (parts.length >= 4) {
                                String namespace = parts[1];
                                // 去掉前缀和 .json 后缀，还原成标准 ResourceLocation
                                String sub = name.replaceFirst("^data/[^/]+/loot_tables?/", "");
                                sub = sub.substring(0, sub.length() - 5);

                                chestTables.add(namespace + ":" + sub);
                            }
                        }
                    });
                } catch (Exception ignored) {
                    // 自动跳过无效或加壳文件
                }
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outFile))) {
            for (String table : chestTables) {
                writer.println(table);
            }
            System.out.println("提取完成！共找到 " + chestTables.size() + " 个箱子战利品表，结果已保存至: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}