package com.example.examplemod.export.modules;

import com.example.examplemod.export.core.ExportIO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;

import java.nio.file.Path;

public final class TrimDataExporter {

    private TrimDataExporter() {
    }

    public static Path export(MinecraftServer server) {
        RegistryAccess registryAccess = server.registryAccess();
        Registry<TrimPattern> patternRegistry = registryAccess.registryOrThrow(Registries.TRIM_PATTERN);
        Registry<TrimMaterial> materialRegistry = registryAccess.registryOrThrow(Registries.TRIM_MATERIAL);

        JsonObject rootJson = new JsonObject();
        JsonArray patternsArray = new JsonArray();
        JsonArray materialsArray = new JsonArray();

        patternRegistry.holders().forEach(holder -> {
            TrimPattern pattern = holder.value();
            JsonObject obj = new JsonObject();
            obj.addProperty("id", holder.key().location().toString());
            obj.addProperty("asset_id", pattern.assetId().toString());
            obj.addProperty("template_item", BuiltInRegistries.ITEM.getKey(pattern.templateItem().value()).toString());
            obj.addProperty("description_key", extractKey(pattern.description()));
            obj.addProperty("description", pattern.description().getString());
            patternsArray.add(obj);
        });

        materialRegistry.holders().forEach(holder -> {
            TrimMaterial material = holder.value();
            JsonObject obj = new JsonObject();
            obj.addProperty("id", holder.key().location().toString());
            obj.addProperty("asset_name", material.assetName());
            obj.addProperty("ingredient_item", BuiltInRegistries.ITEM.getKey(material.ingredient().value()).toString());
            obj.addProperty("description_key", extractKey(material.description()));
            obj.addProperty("description", material.description().getString());
            obj.addProperty("color", material.description().getStyle().getColor() != null
                    ? material.description().getStyle().getColor().serialize()
                    : "unknown");
            materialsArray.add(obj);
        });

        rootJson.add("patterns", patternsArray);
        rootJson.add("materials", materialsArray);

        try {
            return ExportIO.writeJson(ExportIO.resolve("trim_dump.json"), rootJson);
        } catch (Exception e) {
            throw new RuntimeException("导出盔甲纹饰失败: " + e.getMessage(), e);
        }
    }

    private static String extractKey(Component component) {
        if (component.getContents() instanceof TranslatableContents tc) {
            return tc.getKey();
        }
        return "";
    }
}