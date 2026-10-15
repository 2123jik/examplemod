package com.nf121.examplemod.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TrimDataExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 导出整合包内所有的 Trim 数据
     * @param server 当前运行的 MinecraftServer 实例
     */
    public static void exportAllTrims(MinecraftServer server) {
        // 获取动态注册表管理器 (TrimPattern 和 TrimMaterial 属于 Dynamic Registry / Data Registry)
        RegistryAccess registryAccess = server.registryAccess();

        Registry<TrimPattern> patternRegistry = registryAccess.registryOrThrow(Registries.TRIM_PATTERN);
        Registry<TrimMaterial> materialRegistry = registryAccess.registryOrThrow(Registries.TRIM_MATERIAL);

        JsonObject rootJson = new JsonObject();
        JsonArray patternsArray = new JsonArray();
        JsonArray materialsArray = new JsonArray();

        // 1. 遍历整合包所有模组的 Trim Patterns (模板)
        patternRegistry.holders().forEach(holder -> {
            ResourceLocation patternKey = holder.key().location();
            TrimPattern pattern = holder.value();

            JsonObject patternObj = new JsonObject();
            patternObj.addProperty("id", patternKey.toString());
            patternObj.addProperty("asset_id", pattern.assetId().toString());
            
            // 获取对应的模板物品注册名
            Item templateItem = pattern.templateItem().value();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(templateItem);
            patternObj.addProperty("template_item", itemId.toString());
            
            // 获取翻译键（用于显示名称）
            patternObj.addProperty("description_key", pattern.description().getString());

            patternsArray.add(patternObj);
        });

        // 2. 遍历整合包所有模组的 Trim Materials (材料)
        materialRegistry.holders().forEach(holder -> {
            ResourceLocation materialKey = holder.key().location();
            TrimMaterial material = holder.value();

            JsonObject materialObj = new JsonObject();
            materialObj.addProperty("id", materialKey.toString());
            materialObj.addProperty("asset_name", material.assetName());

            // 材料对应的矿物物品
            Item ingredientItem = material.ingredient().value();
            ResourceLocation ingredientId = BuiltInRegistries.ITEM.getKey(ingredientItem);
            materialObj.addProperty("ingredient_item", ingredientId.toString());

            // 材质颜色
            materialObj.addProperty("color", material.description().getStyle().getColor() != null ?
                    material.description().getStyle().getColor().serialize() : "unknown");

            materialsArray.add(materialObj);
        });

        rootJson.add("patterns", patternsArray);
        rootJson.add("materials", materialsArray);

        // 3. 将结果输出到游戏根目录下的 trim_dump.json
        File outputFile = new File(server.getServerDirectory().toFile(), "trim_dump.json");
        try (FileWriter writer = new FileWriter(outputFile)) {
            GSON.toJson(rootJson, writer);
            System.out.println("[TrimExporter] 导出成功！文件保存至: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}