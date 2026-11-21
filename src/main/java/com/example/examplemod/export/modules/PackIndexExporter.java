package com.example.examplemod.export.modules;

import com.example.examplemod.export.core.ExportIO;
import com.example.examplemod.export.core.JarResourceHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class PackIndexExporter {

    private PackIndexExporter() {
    }

    public static ExportResult run(MinecraftServer server, Path customTarget) {
        LangCache langCache = LangCache.load();

        Map<String, Map<String, TranslationResult>> registries = new LinkedHashMap<>();
        BuiltInRegistries.REGISTRY.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .forEach(e -> collectRegistry(registries, e.getKey().location(), e.getValue(), langCache));

        RegistryAccess access = server.registryAccess();
        access.registries()
                .sorted(Comparator.comparing(e -> e.key().location().toString()))
                .forEach(e -> collectRegistry(registries, e.key().location(), e.value(), langCache));

        Map<Class<?>, Set<String>> enums = scanEnums();

        JsonObject root = new JsonObject();
        root.addProperty("format_version", 1);
        root.addProperty("minecraft_version", SharedConstants.getCurrentVersion().getName());
        root.add("registries", writeRegistries(registries));
        root.add("enums", writeEnums(enums));

        Path target = customTarget != null ? customTarget : ExportIO.resolve("packdev_index.json");
        try {
            Path written = ExportIO.writeJson(target, root);
            int regValCount = registries.values().stream().mapToInt(Map::size).sum();
            int enumValCount = enums.values().stream().mapToInt(Set::size).sum();
            return new ExportResult(written, registries.size(), regValCount, enums.size(), enumValCount);
        } catch (Exception e) {
            throw new RuntimeException("PackDev 导出失败: " + e.getMessage(), e);
        }
    }

    private static void collectRegistry(
            Map<String, Map<String, TranslationResult>> output,
            ResourceLocation registryId,
            Registry<?> registry,
            LangCache langCache
    ) {
        Map<String, TranslationResult> itemMap = output.computeIfAbsent(registryId.toString(), k -> new TreeMap<>());
        for (var entry : registry.entrySet()) {
            itemMap.put(entry.getKey().location().toString(), langCache.resolve(entry.getValue()));
        }
    }

    private static JsonArray writeRegistries(Map<String, Map<String, TranslationResult>> registries) {
        JsonArray result = new JsonArray();
        registries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String registryId = entry.getKey();
                    JsonObject source = new JsonObject();
                    source.addProperty("id", registryId);

                    JsonArray aliases = new JsonArray();
                    registryAliases(registryId).forEach(aliases::add);
                    source.add("aliases", aliases);

                    JsonArray values = new JsonArray();
                    JsonObject translations = new JsonObject();
                    for (var valEntry : entry.getValue().entrySet()) {
                        values.add(valEntry.getKey());
                        TranslationResult tr = valEntry.getValue();
                        if (tr.hasAny()) {
                            JsonObject trObj = new JsonObject();
                            if (tr.key() != null) trObj.addProperty("key", tr.key());
                            if (tr.zh() != null) trObj.addProperty("zh", tr.zh());
                            if (tr.en() != null) trObj.addProperty("en", tr.en());
                            translations.add(valEntry.getKey(), trObj);
                        }
                    }
                    source.add("values", values);
                    source.add("translations", translations);
                    result.add(source);
                });
        return result;
    }

    private static JsonArray writeEnums(Map<Class<?>, Set<String>> enums) {
        JsonArray result = new JsonArray();
        enums.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getName()))
                .forEach(entry -> {
                    JsonObject source = new JsonObject();
                    source.addProperty("id", entry.getKey().getName());

                    JsonArray aliases = new JsonArray();
                    enumAliases(entry.getKey()).forEach(aliases::add);
                    source.add("aliases", aliases);

                    JsonArray values = new JsonArray();
                    entry.getValue().forEach(values::add);
                    source.add("values", values);
                    source.add("translations", new JsonObject());
                    result.add(source);
                });
        return result;
    }

    // ==================== 枚举扫描 ====================
    private static Map<Class<?>, Set<String>> scanEnums() {
        Set<String> classNames = new HashSet<>();
        JarResourceHelper.visitAllModAndClasspathRoots(root -> {
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".class"))
                        .forEach(p -> {
                            String rel = root.relativize(p).toString();
                            classNames.add(rel.replace('/', '.').replace('\\', '.').replaceAll("\\.class$", ""));
                        });
            } catch (Exception ignored) {
            }
        });

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Map<Class<?>, Set<String>> result = new LinkedHashMap<>();
        for (String name : classNames) {
            if (isExcludedClass(name)) continue;
            try {
                Class<?> clazz = Class.forName(name, false, cl);
                if (clazz.isEnum() && !clazz.isAnonymousClass() && Modifier.isPublic(clazz.getModifiers())) {
                    Set<String> values = extractEnumValues(clazz);
                    if (!values.isEmpty()) {
                        result.put(clazz, values);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return result;
    }

    private static boolean isExcludedClass(String name) {
        return name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("jdk.") || name.startsWith("sun.")
                || name.startsWith("org.objectweb.asm.") || name.startsWith("com.google.gson.")
                || name.startsWith("org.apache.") || name.startsWith("io.netty.")
                || name.startsWith("net.minecraft.client.");
    }

    private static Set<String> extractEnumValues(Class<?> clazz) {
        Set<String> values = new LinkedHashSet<>();
        Object[] constants = clazz.getEnumConstants();
        if (constants == null) return values;

        Method serializedMethod = null;
        try {
            serializedMethod = clazz.getMethod("getSerializedName");
        } catch (NoSuchMethodException ignored) {
        }

        for (Object constant : constants) {
            String val = null;
            if (constant instanceof StringRepresentable sr) {
                val = sr.getSerializedName();
            }
            if (val == null && serializedMethod != null) {
                try {
                    Object res = serializedMethod.invoke(constant);
                    if (res != null) val = res.toString();
                } catch (Exception ignored) {
                }
            }
            if (val == null && constant instanceof Enum<?> e) {
                val = e.name().toLowerCase(Locale.ROOT);
            }
            if (val != null && !val.isBlank()) {
                values.add(val);
            }
        }
        return values;
    }

    // ==================== 命名别名 ====================
    private static String snakeCase(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('$', '_').replace('-', '_').replace('.', '_')
                .toLowerCase(Locale.ROOT);
    }

    private static Set<String> registryAliases(String registryId) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        String path = registryId.substring(registryId.indexOf(':') + 1);
        aliases.add(snakeCase(path));
        int slash = path.lastIndexOf('/');
        if (slash >= 0) aliases.add(snakeCase(path.substring(slash + 1)));
        return aliases;
    }

    private static Set<String> enumAliases(Class<?> type) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(snakeCase(type.getSimpleName()));
        Class<?> cur = type;
        StringBuilder composite = new StringBuilder();
        while (cur != null && cur != Object.class) {
            if (!composite.isEmpty()) composite.insert(0, "_");
            composite.insert(0, cur.getSimpleName());
            aliases.add(snakeCase(cur.getSimpleName()));
            cur = cur.getEnclosingClass();
        }
        aliases.add(snakeCase(composite.toString()));
        return aliases;
    }

    // ==================== 双语缓存 ====================
    private static final class LangCache {
        private final Map<String, String> zhMap;
        private final Map<String, String> enMap;

        private LangCache(Map<String, String> zhMap, Map<String, String> enMap) {
            this.zhMap = zhMap;
            this.enMap = enMap;
        }

        static LangCache load() {
            Map<String, String> zh = new HashMap<>();
            Map<String, String> en = new HashMap<>();
            JarResourceHelper.visitAllModAndClasspathRoots(root -> {
                Path assets = root.resolve("assets");
                if (!Files.exists(assets)) return;
                try (Stream<Path> stream = Files.walk(assets)) {
                    stream.forEach(p -> {
                        String norm = p.toString().replace('\\', '/');
                        if (norm.endsWith("/lang/zh_cn.json")) {
                            parseJson(p, zh);
                        } else if (norm.endsWith("/lang/en_us.json")) {
                            parseJson(p, en);
                        }
                    });
                } catch (Exception ignored) {
                }
            });
            return new LangCache(zh, en);
        }

        private static void parseJson(Path jsonPath, Map<String, String> output) {
            try (InputStream in = Files.newInputStream(jsonPath);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        output.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        TranslationResult resolve(Object obj) {
            String key = getTranslationKey(obj);
            if (key == null || key.isBlank()) return TranslationResult.EMPTY;
            String zh = zhMap.get(key);
            String en = enMap.get(key);
            if (en == null) {
                Language lang = Language.getInstance();
                if (lang.has(key)) en = lang.getOrDefault(key);
            }
            return new TranslationResult(key, zh, en);
        }

        private static String getTranslationKey(Object value) {
            if (value == null) return null;
            if (value instanceof Holder<?> holder) value = holder.value();
            if (value instanceof Item item) return item.getDescriptionId();
            if (value instanceof Block block) return block.getDescriptionId();
            if (value instanceof EntityType<?> entity) return entity.getDescriptionId();
            if (value instanceof MobEffect effect) return effect.getDescriptionId();
            if (value instanceof Attribute attr) return attr.getDescriptionId();
            if (value instanceof Enchantment ench
                    && ench.description().getContents() instanceof TranslatableContents tc) {
                return tc.getKey();
            }
            try {
                Method m = value.getClass().getMethod("getDescriptionId");
                return (String) m.invoke(value);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public record TranslationResult(String key, String zh, String en) {
        static final TranslationResult EMPTY = new TranslationResult(null, null, null);
        boolean hasAny() { return key != null || zh != null || en != null; }
    }

    public record ExportResult(Path path, int registryCount, int registryValueCount, int enumCount, int enumValueCount) {
    }
}