package com.example.examplemod.tool;

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

    /**
     * data_maps 目录规则（NeoForge 1.21）：
     *   data/<datapack_namespace>/data_maps/<registry_namespace>/<registry_path>/<任意名>.json
     *
     * LHTraits.DATA 挂的 registry = l2hostility:trait
     * → data/examplemod/data_maps/l2hostility/trait/<name>.json
     */
    private static final Path OUTPUT_DIR =
            Paths.get("src", "main", "resources", "data", "examplemod",
                    "data_maps", "l2hostility", "trait").toAbsolutePath();

    private static final String SUFFIX = "_spelltrait";

    // ============ 每个 trait 的默认值，自己调 ============
    private static final int COST      = 30;   // 选择这个 trait 消耗的等级
    private static final int WEIGHT    = 100;  // 随机权重，越大越常见
    private static final int MAX_RANK  = 5;    // 最大等级（traitLevel 上限）
    private static final int MIN_LEVEL = 20;   // 怪物最低等级才能出这个 trait
    // ====================================================

    public static void main(String[] args) throws IOException {

        if (!Files.exists(DEBUG_FILE)) {
            System.err.println("[Gen] 找不到 " + DEBUG_FILE);
            return;
        }
        Files.createDirectories(OUTPUT_DIR);

        // 1) 从 debug 文件读 item id（每个 id 同时就是 trait 的 id）
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

        // 2) 拼一个包含全部条目的 datapack JSON
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