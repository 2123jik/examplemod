package com.example.examplemod.tool;

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

    /** 你自己的模组 ID（模型/贴图都放这个命名空间下） */
    private static final String MODID = "examplemod";

    /**
     * 纹理复制脚本 TextureCopier 的输出结构：
     *   assets/examplemod/textures/item/<modkeyword>/xxx.png
     * 这里 modkeyword 就是下面 KNOWN_NAMESPACES 里的小写短名。
     *
     * 所以模型里的引用为：
     *   layer0 = examplemod:item/l2hostility/bg
     *   layer1 = examplemod:item/<ns>/<path>
     *
     * 若你当时把 TextureCopier 的子目录关掉了（平铺），
     * 就把下面的 USE_SUBDIR 改成 false。
     */
    private static final boolean USE_SUBDIR = true;

    /** TextureCopier 复制 bg.png 时用的子目录名 */
    private static final String BG_SUBDIR = "l2hostility";
    /** bg 的原始文件名（不含 .png） */
    private static final String BG_NAME = "bg";

    /**
     * 已知法术 namespace。它们的共同点是含下划线，
     * 所以不能靠"第一个下划线"来拆 ns/path。
     * 以后加新 mod 就在这里追加。
     */
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

        // ============ 1) 从 debug 文件提取 item id ============
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

        // ============ 2) 逐个生成模型 JSON ============
        int ok = 0, fail = 0;

        // layer0：bg
        String bgRef = USE_SUBDIR
                ? MODID + ":item/" + BG_SUBDIR + "/" + BG_NAME
                : MODID + ":item/" + BG_NAME;

        for (String itemId : itemIds) {
            // examplemod:irons_spellbooks_fireball_spelltrait
            // → rest = irons_spellbooks_fireball
            String rest = itemId.substring((MODID + ":").length());
            rest = rest.substring(0, rest.length() - SUFFIX.length());

            // 用已知 namespace 前缀匹配，取最长的那个（避免前缀互相包含的歧义）
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

            // layer1：指向复制到 examplemod 下的贴图
            String layer1 = USE_SUBDIR
                    ? MODID + ":item/" + ns + "/" + path
                    : MODID + ":item/" + path;

            String json = "{\n"
                    + "  \"parent\": \"minecraft:item/generated\",\n"
                    + "  \"textures\": {\n"
                    + "    \"layer0\": \"" + bgRef + "\",\n"
                    + "    \"layer1\": \"" + layer1 + "\"\n"
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