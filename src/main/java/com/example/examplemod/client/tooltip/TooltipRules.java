package com.example.examplemod.client.tooltip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class TooltipRules implements PreparableReloadListener {
    public static final TooltipRules INSTANCE = new TooltipRules();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "tooltips/";
    private static final String EXTENSION = ".json";

    private volatile Map<ResourceLocation, List<String>> rules = Map.of();

    private TooltipRules() {}

    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier,
            ResourceManager manager,
            ProfilerFiller prepProfiler,
            ProfilerFiller reloadProfiler,
            Executor bgExecutor,
            Executor gameExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> load(manager), bgExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(result -> rules = result, gameExecutor);
    }

    private static Map<ResourceLocation, List<String>> load(ResourceManager manager) {
        Map<ResourceLocation, List<String>> result = new HashMap<>();
        var resources = manager.listResources(
                "tooltips", id -> id.getPath().endsWith(EXTENSION)
        );

        var entries = new ArrayList<>(resources.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<ResourceLocation, Resource> entry : entries) {
            ResourceLocation source = entry.getKey();

            try (var reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8
            )) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new IllegalArgumentException("规则根节点必须是对象");
                }

                JsonObject object = root.getAsJsonObject();
                ResourceLocation item = itemId(source, object);
                JsonElement lines = object.get("lines");

                if (lines == null) continue;
                if (!lines.isJsonArray()) {
                    throw new IllegalArgumentException("lines 必须是数组");
                }

                List<String> parsed = new ArrayList<>();
                for (JsonElement line : lines.getAsJsonArray()) {
                    if (!line.isJsonPrimitive()) {
                        throw new IllegalArgumentException("lines 元素必须是基本值");
                    }
                    parsed.add(line.getAsString());
                }

                if (result.put(item, List.copyOf(parsed)) != null) {
                    LOGGER.warn("重复的 tooltip 规则，{} 覆盖了物品 {} 的已有规则",
                            source, item);
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("无法加载 tooltip 规则 {}", source, exception);
            }
        }
        return Map.copyOf(result);
    }

    private static ResourceLocation itemId(ResourceLocation source, JsonObject object) {
        if (object.has("item")) {
            JsonElement value = object.get("item");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("item 必须是资源 ID 字符串");
            }

            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id == null) {
                throw new IllegalArgumentException("无效的物品 ID: " + value);
            }
            return id;
        }

        String path = source.getPath();
        return ResourceLocation.fromNamespaceAndPath(
                source.getNamespace(),
                path.substring(DIRECTORY.length(), path.length() - EXTENSION.length())
        );
    }

    public static void add(ItemStack stack, List<TooltipContribution> sink) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<String> lines = INSTANCE.rules.get(id);
        if (lines == null) return;

        for (int index = 0; index < lines.size(); index++) {
            sink.add(TooltipContribution.text(
                    TooltipCategory.CUSTOM,
                    index,
                    "rule:" + id + ":" + index,
                    Component.literal(lines.get(index)).withStyle(ChatFormatting.GRAY)
            ));
        }
    }
}
