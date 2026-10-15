package com.nf121.examplemod.trim;

import com.nf121.examplemod.ExampleMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public class RunewordManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunewordManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // 监听器单例
    public static final RunewordManager INSTANCE = new RunewordManager();

    // 内存数据缓存（原子替换引用，保证并发安全）
    private static volatile Map<String, RunewordDefinition> RUNEWORDS = Map.of();

    private RunewordManager() {
        super(GSON, "words"); // 读取 data/<namespace>/words/*.json
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        Map<String, RunewordDefinition> tempMap = new HashMap<>();

        map.forEach((loc, element) -> {
            try {
                JsonObject obj = element.getAsJsonObject();
                RunewordDefinition def = RunewordDefinition.fromJson(obj);
                tempMap.put(def.id(), def);
            } catch (Exception e) {
                // 打印出具体是哪个数据包的哪个 JSON 挂了，排查 bug 一秒定位
                LOGGER.error("无法解析符文之语配置文件: {}", loc, e);
            }
        });

        // 解析完成后一次性原子切换，防止 /reload 期间并发读到空数据
        RUNEWORDS = Collections.unmodifiableMap(tempMap);
        LOGGER.info("成功加载 {} 个符文之语配置！", RUNEWORDS.size());
    }

    // ==================== 查询对外 API ====================

    public static Optional<RunewordDefinition> get(String id) {
        return Optional.ofNullable(RUNEWORDS.get(id));
    }

    public static Map<String, RunewordDefinition> getAll() {
        return RUNEWORDS;
    }

    // ==================== 自动总线注册 ====================

    @SubscribeEvent
    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        // 复用单例注册
        event.addListener(INSTANCE);
    }
}