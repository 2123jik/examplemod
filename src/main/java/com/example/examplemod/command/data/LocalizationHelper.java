package com.example.examplemod.command.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class LocalizationHelper {
    private static Map<String, String> ZH_CN_CACHE = null;
    private static Map<String, String> EN_US_CACHE = null;

    private LocalizationHelper() {
    }

    public static synchronized void ensureLoaded() {
        if (ZH_CN_CACHE != null && EN_US_CACHE != null) {
            return;
        }

        Map<String, String> zhMap = new HashMap<>();
        Map<String, String> enMap = new HashMap<>();

        // 1. 扫描所有已加载模组的 Jar 包
        if (ModList.get() != null) {
            for (IModFileInfo modFile : ModList.get().getModFiles()) {
                Path root = modFile.getFile().getSecureJar().getRootPath();
                scanLangFiles(root, zhMap, enMap);
            }
        }

        // 2. 扫描环境 Classpath
        String classPath = System.getProperty("java.class.path", "");
        for (String entry : classPath.split(File.pathSeparator)) {
            if (entry.endsWith(".jar") || new File(entry).isDirectory()) {
                try {
                    Path p = Path.of(entry);
                    if (Files.exists(p)) {
                        scanLangFiles(p, zhMap, enMap);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        ZH_CN_CACHE = zhMap;
        EN_US_CACHE = enMap;
    }

    public static TranslationResult resolve(Object obj) {
        ensureLoaded();
        String key = getTranslationKey(obj);
        if (key == null || key.isBlank()) {
            return TranslationResult.EMPTY;
        }

        // 1. 查中文
        String zh = ZH_CN_CACHE.get(key);

        // 2. 查英文（优先模组 en_us，兜底原版系统 Language 实例）
        String en = EN_US_CACHE.get(key);
        if (en == null) {
            Language lang = Language.getInstance();
            if (lang.has(key)) {
                en = lang.getOrDefault(key);
            }
        }

        return new TranslationResult(key, zh, en);
    }

    private static void scanLangFiles(Path path, Map<String, String> zhMap, Map<String, String> enMap) {
        try {
            if (Files.isDirectory(path)) {
                Path assets = path.resolve("assets");
                if (Files.exists(assets)) {
                    try (Stream<Path> stream = Files.walk(assets)) {
                        stream.forEach(p -> parseIfLangFile(p, zhMap, enMap));
                    }
                }
            } else if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
                URI uri = URI.create("jar:" + path.toUri());
                try (FileSystem fs = getFileSystem(uri)) {
                    for (Path root : fs.getRootDirectories()) {
                        Path assets = root.resolve("assets");
                        if (Files.exists(assets)) {
                            try (Stream<Path> stream = Files.walk(assets)) {
                                stream.forEach(p -> parseIfLangFile(p, zhMap, enMap));
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void parseIfLangFile(Path p, Map<String, String> zhMap, Map<String, String> enMap) {
        String normalized = p.toString().replace('\\', '/');
        if (normalized.endsWith("/lang/zh_cn.json")) {
            parseJson(p, zhMap);
        } else if (normalized.endsWith("/lang/en_us.json")) {
            parseJson(p, enMap);
        }
    }

    private static void parseJson(Path jsonPath, Map<String, String> output) {
        try (InputStream in = Files.newInputStream(jsonPath);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    output.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static FileSystem getFileSystem(URI uri) {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (Exception e) {
            try {
                return FileSystems.newFileSystem(uri, Collections.emptyMap());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static String getTranslationKey(Object value) {
        if (value == null) return null;

        if (value instanceof Holder<?> holder) {
            value = holder.value();
        }

        if (value instanceof Item item) return item.getDescriptionId();
        if (value instanceof Block block) return block.getDescriptionId();
        if (value instanceof EntityType<?> entity) return entity.getDescriptionId();
        if (value instanceof MobEffect effect) return effect.getDescriptionId();
        if (value instanceof Attribute attr) return attr.getDescriptionId();
        if (value instanceof Enchantment enchantment) {
            Component desc = enchantment.description();
            if (desc.getContents() instanceof TranslatableContents tc) {
                return tc.getKey();
            }
        }

        try {
            Method m = value.getClass().getMethod("getDescriptionId");
            return (String) m.invoke(value);
        } catch (Exception ignored) {
        }

        return null;
    }

    public record TranslationResult(String key, String zh, String en) {
        public static final TranslationResult EMPTY = new TranslationResult(null, null, null);

        public boolean hasAny() {
            return key != null || zh != null || en != null;
        }
    }
}