package com.example.examplemod.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * 从 mod jar 中提取纹理，复制到本模组的资源目录。
 *
 * 运行方式（JDK 11+，单文件直接运行）：
 *     java TextureCopier.java
 * 或先编译再运行：
 *     javac TextureCopier.java && java TextureCopier
 *
 * 请在项目根目录（有 src/ 那一层）运行，否则 OUTPUT_DIR 会落到别处。
 */
public class TextureCopier {

    // ==================== 可配置区 ====================

    /** 你自己的模组 ID */
    private static final String TARGET_MODID = "examplemod";

    /** 输出目录：<项目根>/src/main/resources/assets/<modid>/textures/item */
    private static final Path OUTPUT_DIR = Paths.get(
            "src", "main", "resources", "assets",
            TARGET_MODID, "textures", "item"
    );

    /** 搜索 mod jar 的目录（全部会扫描，去重后使用） */
    private static final List<Path> MODS_SEARCH_PATHS = List.of(
            Paths.get("C:", "Users", "lyj", "AppData", "Roaming",
                    "PrismLauncher", "instances", "hostility", "minecraft", "mods"),
            Paths.get("run", "mods"),
            Paths.get("mods")
    );

    /**
     * 提取规则：
     *   key   = mod jar 文件名里包含的关键词（小写匹配）
     *   value = jar 内部路径匹配规则列表：
     *           - 以 "/" 结尾   => 前缀匹配（该目录下所有 png）
     *           - 以 "~" 开头   => 路径片段匹配（jar 内部路径包含该片段）
     *           - 其他          => 精确匹配单个文件
     *
     *   注意：多个模组命中的文件会被复制到同一个 textures/item 目录，
     *        若有同名文件会互相覆盖。可在 copyEntry 里按模组名建子目录。
     */
    private static final Map<String, List<String>> EXTRACT_RULES = new LinkedHashMap<>();

    static {
        // L2 Hostility 的背景图
        EXTRACT_RULES.put("l2hostility",
                List.of("assets/l2hostility/textures/item/bg.png"));

        // Iron's Spellbooks：所有 spell_icons 下的 png（无论在哪一层）
        EXTRACT_RULES.put("irons_spellbooks",
                List.of("~spell_icons/"));

        // ISS: Magic From The East：同上
        EXTRACT_RULES.put("iss_magicfromtheeast",
                List.of("~spell_icons/"));
    }

    // ==================== 主逻辑 ====================

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        System.out.println("输出目录: " + OUTPUT_DIR.toAbsolutePath());

        List<Path> jars = findModJars();
        if (jars.isEmpty()) {
            System.err.println("未找到任何 mod jar，请检查 MODS_SEARCH_PATHS。");
            return;
        }

        System.out.println("找到 " + jars.size() + " 个 jar：");
        for (Path p : jars) System.out.println("   " + p);

        int total = 0;
        for (Path jarPath : jars) {
            String lower = jarPath.getFileName().toString().toLowerCase(Locale.ROOT);
            for (var rule : EXTRACT_RULES.entrySet()) {
                if (lower.contains(rule.getKey())) {
                    System.out.println("\n>> 处理 " + jarPath.getFileName());
                    total += extractFromJar(jarPath, rule.getKey(), rule.getValue());
                }
            }
        }

        System.out.println("\n全部完成，共复制 " + total + " 个文件到 " + OUTPUT_DIR);
    }

    private static List<Path> findModJars() throws IOException {
        List<Path> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Path dir : MODS_SEARCH_PATHS) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.list(dir)) {
                for (Path p : s.toList()) {
                    if (!p.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) continue;
                    String key = p.toAbsolutePath().normalize().toString();
                    if (seen.add(key)) out.add(p);
                }
            }
        }
        return out;
    }

    private static int extractFromJar(Path jarPath, String modKeyword, List<String> rules) throws IOException {
        int count = 0;
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String entryName = e.getName();
                if (e.isDirectory() || !entryName.toLowerCase(Locale.ROOT).endsWith(".png")) continue;

                for (String rule : rules) {
                    if (matches(entryName, rule)) {
                        copyEntry(jar, e, entryName, modKeyword);
                        count++;
                        break;
                    }
                }
            }
        }
        if (count == 0) System.out.println("   （未找到匹配文件）");
        return count;
    }

    private static boolean matches(String entryName, String rule) {
        if (rule.startsWith("~")) {
            return entryName.contains(rule.substring(1));
        }
        if (rule.endsWith("/")) {
            return entryName.startsWith(rule);
        }
        return entryName.equals(rule);
    }

    private static void copyEntry(JarFile jar, JarEntry entry, String entryName, String modKeyword) throws IOException {
        String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);

        // 若两个模组有同名图标，按模组名建子目录避免覆盖。
        // 想去掉子目录，改成：
        //     Path target = OUTPUT_DIR.resolve(fileName);
        Path target = OUTPUT_DIR.resolve(modKeyword).resolve(fileName);
        Files.createDirectories(target.getParent());

        try (InputStream in = jar.getInputStream(entry)) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("   复制: " + entryName + "  ->  " + target.getFileName());
    }
}