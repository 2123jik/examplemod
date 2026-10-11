package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.util.IStructureScanner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(StructureTemplateManager.class)
public abstract class MixinStructureTemplateManager implements IStructureScanner {

    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private Map<ResourceLocation, Optional<StructureTemplate>> structureRepository;

    @Unique
    private static final Set<ResourceLocation> CORRUPT_BLACK_LIST = ConcurrentHashMap.newKeySet();

    @Unique
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Unique
    private static File getConfigFile() {
        // 保存在 config/corrupt_structures_blacklist.json
        return new File("config/corrupt_structures_blacklist.json");
    }

    /**
     * 构造函数或初始化时自动加载磁盘黑名单
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInitLoadDiskCache(CallbackInfo ci) {
        loadBlacklistFromDisk();
    }

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onGetFastPass(ResourceLocation id, CallbackInfoReturnable<Optional<StructureTemplate>> cir) {
        if (CORRUPT_BLACK_LIST.contains(id)) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Override
    public int scanAndCacheCorruptStructures() {
        StructureTemplateManager self = (StructureTemplateManager) (Object) this;
        var allTemplates = self.listTemplates().toList();

        LOGGER.info("[StructureScanner] 开始扫描全部 {} 个结构...", allTemplates.size());
        int corruptCount = 0;

        for (ResourceLocation id : allTemplates) {
            if (CORRUPT_BLACK_LIST.contains(id)) {
                corruptCount++;
                continue;
            }

            try {
                this.structureRepository.remove(id);
                Optional<StructureTemplate> loaded = self.get(id);

                if (loaded.isEmpty()) {
                    CORRUPT_BLACK_LIST.add(id);
                    this.structureRepository.put(id, Optional.empty());
                    corruptCount++;
                }
            } catch (Throwable t) {
                CORRUPT_BLACK_LIST.add(id);
                this.structureRepository.put(id, Optional.empty());
                corruptCount++;
                LOGGER.error("[StructureScanner] 标记坏结构: {}", id, t);
            }
        }

        // 扫描完成后保存至本地 JSON
        saveBlacklistToDisk();

        LOGGER.info("[StructureScanner] 扫描完成！共拦截 {} 个损坏结构并已保存配置。", corruptCount);
        return corruptCount;
    }

    @Unique
    private void saveBlacklistToDisk() {
        try {
            File configFile = getConfigFile();
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                Set<String> idStrings = ConcurrentHashMap.newKeySet();
                CORRUPT_BLACK_LIST.forEach(id -> idStrings.add(id.toString()));
                GSON.toJson(idStrings, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[StructureScanner] 无法保存黑名单配置到磁盘", e);
        }
    }

    @Unique
    private void loadBlacklistFromDisk() {
        File configFile = getConfigFile();
        if (!configFile.exists()) return;

        try (FileReader reader = new FileReader(configFile)) {
            Type setType = new TypeToken<Set<String>>(){}.getType();
            Set<String> idStrings = GSON.fromJson(reader, setType);
            if (idStrings != null) {
                idStrings.forEach(str -> CORRUPT_BLACK_LIST.add(ResourceLocation.parse(str)));
                LOGGER.info("[StructureScanner] 已从本地文件加载 {} 个损坏结构黑名单", CORRUPT_BLACK_LIST.size());
            }
        } catch (Exception e) {
            LOGGER.error("[StructureScanner] 读取磁盘黑名单配置失败", e);
        }
    }
}