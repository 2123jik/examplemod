package com.example.examplemod.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = "examplemod")
public class ApothStudioExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STUDIO_DIR = FMLPaths.GAMEDIR.get().resolve("apoth_studio");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("apoth_studio")
                        .requires(source -> source.hasPermission(0))
                        .then(Commands.literal("export").executes(ctx -> {
                            try {
                                Files.createDirectories(STUDIO_DIR);
                                // 【核心修复点 1】：传入当前世界命令上下文的注册表提供者 (registryAccess)
                                exportStudioPackage(
                                        ctx.getSource().registryAccess(),
                                        STUDIO_DIR.resolve("apoth_studio_data.json").toFile()
                                );
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[ApothStudio]§f 数据底模已全量打包至: §e.minecraft/apoth_studio/apoth_studio_data.json"), false);
                                ctx.getSource().sendSuccess(() -> Component.literal("§b👉 直接在浏览器打开工作台导入此文件即可！"), false);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("§c导出失败: " + e.getMessage()));
                                e.printStackTrace();
                            }
                            return 1;
                        }))
        );
    }

    private static void exportStudioPackage(HolderLookup.Provider registries, File file) throws Exception {
        JsonObject studioRoot = new JsonObject();

        // 【核心修复点 2】：用当前游戏注册表包装 JsonOps，使其具备方块、实体、附魔等全部注册表访问权！
        DynamicOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

        // 1. 导出全部合法分类
        JsonArray categoriesArr = new JsonArray();
        for (LootCategory cat : Apoth.BuiltInRegs.LOOT_CATEGORY) {
            if (!cat.isNone()) {
                JsonObject catObj = new JsonObject();
                catObj.addProperty("id", cat.getKey().toString());
                catObj.addProperty("desc", cat.getDescId());
                categoriesArr.add(catObj);
            }
        }
        studioRoot.add("categories", categoriesArr);

        // 2. 导出所有词缀（带有注册表环境的序列化 + 容错机制）
        JsonObject affixesObj = new JsonObject();
        int affixSuccess = 0;
        for (Affix affix : AffixRegistry.INSTANCE.getValues()) {
            try {
                @SuppressWarnings("unchecked")
                Codec<Affix> codec = (Codec<Affix>) affix.getCodec();
                DataResult<JsonElement> result = codec.encodeStart(ops, affix);

                result.ifSuccess(elem -> affixesObj.add(affix.id().toString(), elem))
                        .ifError(err -> System.err.println("[ApothStudio] 词缀序列化警告 (" + affix.id() + "): " + err.message()));

                if (result.isSuccess()) affixSuccess++;
            } catch (Exception e) {
                System.err.println("[ApothStudio] 跳过无法序列化的词缀: " + affix.id());
            }
        }
        studioRoot.add("affixes", affixesObj);

        // 3. 导出所有宝石（带有注册表环境的序列化 + 容错机制）
        JsonObject gemsObj = new JsonObject();
        int gemSuccess = 0;
        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            try {
                DataResult<JsonElement> result = Gem.CODEC.encodeStart(ops, gem);

                result.ifSuccess(elem -> gemsObj.add(gem.getId().toString(), elem))
                        .ifError(err -> System.err.println("[ApothStudio] 宝石序列化警告 (" + gem.getId() + "): " + err.message()));

                if (result.isSuccess()) gemSuccess++;
            } catch (Exception e) {
                System.err.println("[ApothStudio] 跳过无法序列化的宝石: " + gem.getId());
            }
        }
        studioRoot.add("gems", gemsObj);

        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(studioRoot, writer);
        }

        System.out.println(String.format("[ApothStudio] 成功导出: %d 个词缀, %d 颗宝石", affixSuccess, gemSuccess));
    }
}