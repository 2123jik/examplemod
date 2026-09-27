package com.example.examplemod.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GenerateSpellTraitDataMap {

    private static final Path DEBUG_FILE =
            Paths.get("run", "spelltrait_debug.txt").toAbsolutePath();

    private static final Path OUTPUT_DIR =
            Paths.get("src", "main", "resources", "data", "examplemod",
                    "data_maps", "l2hostility", "trait").toAbsolutePath();

    private static final String SUFFIX = "_spelltrait";

    private static final int COST      = 30;       private static final int WEIGHT    = 100;      private static final int MAX_RANK  = 5;        private static final int MIN_LEVEL = 20;   
    public static void main(String[] args) throws IOException {

        if (!Files.exists(DEBUG_FILE)) {
            System.err.println("[Gen] 找不到 " + DEBUG_FILE);
            return;
        }
        Files.createDirectories(OUTPUT_DIR);

        List<String> traitIds = new ArrayList<>();
        for (String line : Files.readAllLines(DEBUG_FILE, StandardCharsets.UTF_8)) {
            int i = line.indexOf("[OK-REG] 注册成功: ");
            if (i < 0) continue;
            String id = line.substring(i + "[OK-REG] 注册成功: ".length()).trim();
            if (id.startsWith("examplemod:") && id.endsWith(SUFFIX)) {
                traitIds.add(id);
            }
        }
        System.out.println("[Gen] 读到 " + traitIds.size() + " 个 trait id");

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"replace\": false,\n");
        sb.append("  \"values\": {\n");

        for (int i = 0; i < traitIds.size(); i++) {
            sb.append("    \"").append(traitIds.get(i)).append("\": {\n");
            sb.append("      \"cost\": ").append(COST).append(",\n");
            sb.append("      \"weight\": ").append(WEIGHT).append(",\n");
            sb.append("      \"max_rank\": ").append(MAX_RANK).append(",\n");
            sb.append("      \"min_level\": ").append(MIN_LEVEL).append("\n");
            sb.append("    }");
            if (i < traitIds.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  }\n");
        sb.append("}\n");

        Path out = OUTPUT_DIR.resolve("spelltrait.json");
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);

        System.out.println("[Gen] 生成 " + traitIds.size() + " 个 TraitConfig");
        System.out.println("[Gen] 输出: " + out);
    }
}