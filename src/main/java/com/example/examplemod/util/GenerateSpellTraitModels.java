package com.example.examplemod.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GenerateSpellTraitModels {

    private static final Path DEBUG_FILE =
            Paths.get("run", "spelltrait_debug.txt").toAbsolutePath();

    private static final Path OUTPUT_DIR =
            Paths.get("src", "main", "resources", "assets", "examplemod", "models", "item")
                    .toAbsolutePath();

    private static final String SUFFIX = "_spelltrait";

    private static final String MODID = "examplemod";

    private static final boolean USE_SUBDIR = true;

    private static final String BG_SUBDIR = "l2hostility";
    private static final String BG_NAME = "bg";

    private static final List<String> KNOWN_NAMESPACES = List.of(
            "irons_spellbooks",
            "iss_magicfromtheeast"
    );

    public static void main(String[] args) throws IOException {

        if (!Files.exists(DEBUG_FILE)) {
            System.err.println("[Gen] 找不到 " + DEBUG_FILE);
            return;
        }

        Files.createDirectories(OUTPUT_DIR);

        List<String> itemIds = new ArrayList<>();
        for (String line : Files.readAllLines(DEBUG_FILE, StandardCharsets.UTF_8)) {
            int i = line.indexOf("[OK-REG] 注册成功: ");
            if (i < 0) continue;
            String id = line.substring(i + "[OK-REG] 注册成功: ".length()).trim();
            if (id.startsWith(MODID + ":") && id.endsWith(SUFFIX)) {
                itemIds.add(id);
            }
        }
        System.out.println("[Gen] 读到 " + itemIds.size() + " 个 item id");

        int ok = 0, fail = 0;

        String bgRef = USE_SUBDIR
                ? MODID + ":item/" + BG_SUBDIR + "/" + BG_NAME
                : MODID + ":item/" + BG_NAME;

        for (String itemId : itemIds) {
            String rest = itemId.substring((MODID + ":").length());
            rest = rest.substring(0, rest.length() - SUFFIX.length());

            String ns = null;
            for (String k : KNOWN_NAMESPACES) {
                if (rest.startsWith(k + "_")) {
                    if (ns == null || k.length() > ns.length()) {
                        ns = k;
                    }
                }
            }
            if (ns == null) {
                System.err.println("[Gen] 未知 namespace: " + itemId);
                fail++;
                continue;
            }
            String path = rest.substring(ns.length() + 1);
            if (path.isEmpty()) {
                System.err.println("[Gen] path 为空: " + itemId);
                fail++;
                continue;
            }

            String itemName = ns + "_" + path + SUFFIX;

            String layer1 = USE_SUBDIR
                    ? MODID + ":item/" + ns + "/" + path
                    : MODID + ":item/" + path;

            String json = "{\n"
                    + "  \"parent\": \"minecraft:item/generated\",\n"
                    + "  \"textures\": {\n"
                    + "    \"layer1\": \"" + bgRef + "\",\n"
                    + "    \"layer0\": \"" + layer1 + "\"\n"
                    + "  },\n"
                    + "  \"render_type\": \"minecraft:translucent\"\n"
                    + "}\n";

            Path out = OUTPUT_DIR.resolve(itemName + ".json");
            Files.writeString(out, json, StandardCharsets.UTF_8);
            ok++;
        }

        System.out.println("[Gen] 完成：成功 " + ok + "，失败 " + fail);
        System.out.println("[Gen] 输出目录: " + OUTPUT_DIR);
        System.out.println("[Gen] layer0 = " + bgRef);
        System.out.println("[Gen] layer1 示例 = " + (USE_SUBDIR
                ? MODID + ":item/<ns>/<path>"
                : MODID + ":item/<path>"));
    }
}