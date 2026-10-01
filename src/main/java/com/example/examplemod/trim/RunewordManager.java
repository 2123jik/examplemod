package com.example.examplemod.trim;


import com.example.examplemod.ExampleMod;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public class RunewordManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final Map<String, RunewordDefinition> RUNEWORDS = new HashMap<>();

    public RunewordManager() {
        super(GSON, "words"); // 读取 data/<namespace>/words/ 下的所有 json
    }

    @Override
    protected void apply(Map<ResourceLocation, com.google.gson.JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        RUNEWORDS.clear();
        map.forEach((loc, element) -> {
            try {
                JsonObject obj = element.getAsJsonObject();
                RunewordDefinition def = RunewordDefinition.fromJson(obj);
                RUNEWORDS.put(def.id(), def);
            } catch (Exception e) {
                ExampleMod.LOGGER.error("Failed to parse runeword JSON: " + loc, e);
            }
        });
        ExampleMod.LOGGER.info("Loaded {} runewords successfully.", RUNEWORDS.size());
    }
}