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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class DataExporter {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private DataExporter() {
    }

    public static ExportResult export(
        MinecraftServer server,
        Path target
    ) throws IOException {
        Map<String, Set<String>> registries = new LinkedHashMap<>();

        // 导出静态注册表，例如 item、block、entity_type、attribute 等。
        collectBuiltInRegistries(registries);

        // 导出服务器当前 RegistryAccess 中可见的注册表。
        // 这里可以包含数据包或服务器运行环境提供的动态注册表。
        collectServerRegistries(
            server.registryAccess(),
            registries
        );

        JsonObject root = new JsonObject();
        root.addProperty("format_version", 1);
        root.addProperty("minecraft_version", "1.21.1");
        root.add("registries", writeRegistries(registries));

        // 当前版本先保留 enums 字段，后面再接入枚举扫描。
        root.add("enums", new JsonArray());

        Path absolutePath = target
            .toAbsolutePath()
            .normalize();

        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(
            absolutePath,
            StandardCharsets.UTF_8
        )) {
            GSON.toJson(root, writer);
        }

        int valueCount = registries.values()
            .stream()
            .mapToInt(Set::size)
            .sum();

        return new ExportResult(
            absolutePath,
            registries.size(),
            valueCount,
            0,
            0
        );
    }

    private static void collectBuiltInRegistries(
        Map<String, Set<String>> output
    ) {
        BuiltInRegistries.REGISTRY.entrySet()
            .stream()
            .sorted(Comparator.comparing(
                entry -> entry.getKey().location().toString()
            ))
            .forEach(entry -> {
                ResourceLocation registryId =
                    entry.getKey().location();

                Registry<?> registry = entry.getValue();

                addRegistry(
                    output,
                    registryId,
                    registry
                );
            });
    }

    private static void collectServerRegistries(
        RegistryAccess registryAccess,
        Map<String, Set<String>> output
    ) {
        registryAccess.registries()
            .sorted(Comparator.comparing(
                entry -> entry.key().location().toString()
            ))
            .forEach(entry -> {
                ResourceLocation registryId =
                    entry.key().location();

                Registry<?> registry = entry.value();

                addRegistry(
                    output,
                    registryId,
                    registry
                );
            });
    }

    private static void addRegistry(
        Map<String, Set<String>> output,
        ResourceLocation registryId,
        Registry<?> registry
    ) {
        Set<String> values = output.computeIfAbsent(
            registryId.toString(),
            ignored -> new TreeSet<>()
        );

        registry.keySet()
            .stream()
            .map(ResourceLocation::toString)
            .forEach(values::add);
    }

    private static JsonArray writeRegistries(
        Map<String, Set<String>> registries
    ) {
        JsonArray result = new JsonArray();

        registries.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String registryId = entry.getKey();

                JsonObject source = new JsonObject();
                source.addProperty("id", registryId);

                JsonArray aliases = new JsonArray();

                for (String alias : registryAliases(registryId)) {
                    aliases.add(alias);
                }

                source.add("aliases", aliases);

                JsonArray values = new JsonArray();

                for (String value : entry.getValue()) {
                    values.add(value);
                }

                source.add("values", values);
                result.add(source);
            });

        return result;
    }

    private static Set<String> registryAliases(
        String registryId
    ) {
        Set<String> aliases = new TreeSet<>();

        String path = registryId.substring(
            registryId.indexOf(':') + 1
        );

        aliases.add(path);

        int slashIndex = path.lastIndexOf('/');
        if (slashIndex >= 0) {
            aliases.add(path.substring(slashIndex + 1));
        }

        return aliases;
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
