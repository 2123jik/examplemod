package com.example.examplemod.command.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class DataExporter {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private DataExporter() {
    }

    public static ExportResult export(MinecraftServer server, Path target) throws IOException {
        LocalizationHelper.ensureLoaded();

        Map<String, Map<String, LocalizationHelper.TranslationResult>> registries = new LinkedHashMap<>();
        collectBuiltInRegistries(registries);
        collectServerRegistries(server.registryAccess(), registries);

        Map<Class<?>, Set<String>> enums = EnumScanner.scanEnums();

        JsonObject root = new JsonObject();
        root.addProperty("format_version", 1);
        root.addProperty("minecraft_version", "1.21.1");
        root.add("registries", writeRegistries(registries));
        root.add("enums", writeEnums(enums));

        Path absolutePath = target.toAbsolutePath().normalize();
        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(absolutePath, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }

        int registryValueCount = registries.values().stream().mapToInt(Map::size).sum();
        int enumValueCount = enums.values().stream().mapToInt(Set::size).sum();

        return new ExportResult(
            absolutePath,
            registries.size(),
            registryValueCount,
            enums.size(),
            enumValueCount
        );
    }

    private static void collectBuiltInRegistries(Map<String, Map<String, LocalizationHelper.TranslationResult>> output) {
        BuiltInRegistries.REGISTRY.entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().location().toString()))
            .forEach(entry -> addRegistry(output, entry.getKey().location(), entry.getValue()));
    }

    private static void collectServerRegistries(RegistryAccess access, Map<String, Map<String, LocalizationHelper.TranslationResult>> output) {
        access.registries()
            .sorted(Comparator.comparing(entry -> entry.key().location().toString()))
            .forEach(entry -> addRegistry(output, entry.key().location(), entry.value()));
    }

    private static void addRegistry(Map<String, Map<String, LocalizationHelper.TranslationResult>> output, ResourceLocation registryId, Registry<?> registry) {
        Map<String, LocalizationHelper.TranslationResult> itemMap = output.computeIfAbsent(registryId.toString(), ignored -> new TreeMap<>());

        for (var entry : registry.entrySet()) {
            String id = entry.getKey().location().toString();
            Object obj = entry.getValue();
            LocalizationHelper.TranslationResult trans = LocalizationHelper.resolve(obj);
            itemMap.put(id, trans);
        }
    }

    private static JsonArray writeRegistries(Map<String, Map<String, LocalizationHelper.TranslationResult>> registries) {
        JsonArray result = new JsonArray();
        registries.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String registryId = entry.getKey();
                Map<String, LocalizationHelper.TranslationResult> valMap = entry.getValue();

                JsonObject source = new JsonObject();
                source.addProperty("id", registryId);

                JsonArray aliases = new JsonArray();
                for (String alias : NameUtil.registryAliases(registryId)) {
                    aliases.add(alias);
                }
                source.add("aliases", aliases);

                JsonArray values = new JsonArray();
                JsonObject translations = new JsonObject();

                for (Map.Entry<String, LocalizationHelper.TranslationResult> valEntry : valMap.entrySet()) {
                    String id = valEntry.getKey();
                    values.add(id);

                    LocalizationHelper.TranslationResult tr = valEntry.getValue();
                    if (tr.hasAny()) {
                        JsonObject trObj = new JsonObject();
                        if (tr.key() != null) trObj.addProperty("key", tr.key());
                        if (tr.zh() != null) trObj.addProperty("zh", tr.zh());
                        if (tr.en() != null) trObj.addProperty("en", tr.en());
                        translations.add(id, trObj);
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
                Class<?> clazz = entry.getKey();
                JsonObject source = new JsonObject();
                source.addProperty("id", clazz.getName());

                JsonArray aliases = new JsonArray();
                for (String alias : NameUtil.enumAliases(clazz)) {
                    aliases.add(alias);
                }
                source.add("aliases", aliases);

                JsonArray values = new JsonArray();
                for (String value : entry.getValue()) {
                    values.add(value);
                }
                source.add("values", values);
                source.add("translations", new JsonObject());

                result.add(source);
            });
        return result;
    }

    public record ExportResult(
        Path path,
        int registryCount,
        int registryValueCount,
        int enumCount,
        int enumValueCount
    ) {
    }
}