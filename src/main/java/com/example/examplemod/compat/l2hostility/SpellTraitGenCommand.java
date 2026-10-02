package com.example.examplemod.compat.l2hostility;

import com.example.examplemod.util.IStructureScanner;
import com.example.examplemod.util.TrimDataExporter;
import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@EventBusSubscriber(modid = "examplemod")
public class SpellTraitGenCommand {

    
    private static final String DEFAULT_OUT =
            "../src/main/resources/data/l2hostility/data_maps/l2hostility/trait";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(
                Commands.literal("scan_structures")
                        .requires(source -> source.hasPermission(2)) // 需要 OP 权限
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            StructureTemplateManager manager = source.getServer().getStructureManager();

                            source.sendSuccess(() -> Component.literal("§e[结构检测] 正在进行全量 NBT 解压与文件校验，请稍候..."), true);

                            // 安全造型调用接口
                            if (manager instanceof IStructureScanner scanner) {
                                int corruptFound = scanner.scanAndCacheCorruptStructures();
                                source.sendSuccess(() -> Component.literal(
                                        String.format("§a[结构检测] 扫描完成！成功排查并拉黑了 §c%d §a个损坏结构！", corruptFound)
                                ), true);
                            } else {
                                source.sendFailure(Component.literal("§c[错误] IStructureScanner Mixin 未能成功注入到 StructureTemplateManager！"));
                            }

                            return 1;
                        })
        );
        d.register(Commands.literal("spelltrait")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("gen")
                        .executes(ctx -> gen(ctx.getSource(), DEFAULT_OUT))
                        .then(Commands.argument("outDir", StringArgumentType.greedyString())
                                .executes(ctx -> gen(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "outDir"))))));
        d.register(Commands.literal("export_attributes")
                .requires(source -> source.hasPermission(2)) // 需要 2 级管理员/作弊权限
                .executes(SpellTraitGenCommand::exportAttributes));

        d.register(Commands.literal("exporttrims")
                .requires(source -> source.hasPermission(2)) // 仅管理员/OP可用
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    // 异步或同步执行导出
                    TrimDataExporter.exportAllTrims(source.getServer());
                    source.sendSuccess(() -> Component.literal("§a[整合包] 所有的 Trim 模板与材质已导出至根目录 trim_dump.json！"), true);
                    return 1;
                }));

        d.register(
                Commands.literal("classify_tess")
                        .requires(source -> source.hasPermission(2)) // 需要管理员权限
                        .executes(SpellTraitGenCommand::execute)
        );

    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        Path configDir = FMLPaths.CONFIGDIR.get();
        Path tessFile = configDir.resolve("collapsiblegroups/groups/tess.json");

        if (!Files.exists(tessFile)) {
            source.sendFailure(Component.literal("未找到文件: " + tessFile.toAbsolutePath()));
            return 0;
        }

        try {
            // 1. 读取并提取所有物品 ID
            String content = Files.readString(tessFile, StandardCharsets.UTF_8);
            JsonObject rootJson = JsonParser.parseString(content).getAsJsonObject();
            JsonArray anyArray = rootJson.getAsJsonObject("filter").getAsJsonArray("any");

            Set<String> itemIds = new LinkedHashSet<>();
            for (JsonElement el : anyArray) {
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("id")) {
                        itemIds.add(obj.get("id").getAsString());
                    } else if (obj.has("stack") && obj.getAsJsonObject("stack").has("data")) {
                        JsonObject data = obj.getAsJsonObject("stack").getAsJsonObject("data");
                        if (data.has("id")) {
                            itemIds.add(data.get("id").getAsString());
                        }
                    }
                }
            }

            // 2. 统计【物品 -> Tags】和【Tag -> 物品集合】
            Map<String, Set<TagKey<Item>>> itemToTags = new HashMap<>();
            Map<TagKey<Item>, Set<String>> tagMembers = new HashMap<>();

            for (String rawId : itemIds) {
                ResourceLocation rl = ResourceLocation.tryParse(rawId);
                if (rl == null) continue;

                Optional<Holder.Reference<Item>> holderOpt = BuiltInRegistries.ITEM.getHolder(rl);
                if (holderOpt.isEmpty()) continue;

                List<TagKey<Item>> tags = holderOpt.get().tags().toList();
                itemToTags.put(rawId, new HashSet<>(tags));

                for (TagKey<Item> tag : tags) {
                    tagMembers.computeIfAbsent(tag, k -> new HashSet<>()).add(rawId);
                }
            }

            // 3. 核心算法：偏序筛选，每个物品寻找最具体的叶子 Tag
            // 最终分组：Tag标识 -> 该分组内的物品集合
            Map<String, Set<String>> finalGroups = new TreeMap<>();
            Set<String> untaggedOrUnique = new LinkedHashSet<>();

            for (String itemId : itemIds) {
                Set<TagKey<Item>> candidates = itemToTags.get(itemId);
                if (candidates == null || candidates.isEmpty()) {
                    untaggedOrUnique.add(itemId);
                    continue;
                }

                // 筛选出极小元（叶子节点）：如果 Tag A 的成员范围严格大于且包含了 Tag B，剔除 Tag A
                List<TagKey<Item>> minimalTags = new ArrayList<>();
                for (TagKey<Item> t1 : candidates) {
                    Set<String> m1 = tagMembers.get(t1);
                    boolean isSuperset = false;

                    for (TagKey<Item> t2 : candidates) {
                        if (t1.equals(t2)) continue;
                        Set<String> m2 = tagMembers.get(t2);

                        // m2 是 m1 的真子集（m1 范围更大，属于泛化大类），t1 淘汰
                        if (m1.containsAll(m2) && m1.size() > m2.size()) {
                            isSuperset = true;
                            break;
                        }
                    }

                    if (!isSuperset) {
                        minimalTags.add(t1);
                    }
                }

                if (minimalTags.isEmpty()) {
                    untaggedOrUnique.add(itemId);
                    continue;
                }

                // 如果剩下等价的叶子 Tag（如 c:swords 与 minecraft:swords 范围完全一致），挑选最规范的一个
                TagKey<Item> bestTag = chooseCanonicalTag(minimalTags, tagMembers);

                // 将该物品归入选出的具体小类
                String groupName = bestTag.location().toString();
                finalGroups.computeIfAbsent(groupName, k -> new LinkedHashSet<>()).add(itemId);
            }

            // 4. 将分类结果写入 JSON
            Path outDir = configDir.resolve("collapsiblegroups/groups/classified");
            Files.createDirectories(outDir);

            int groupCount = 0;
            JsonObject summaryJson = new JsonObject();

            for (Map.Entry<String, Set<String>> entry : finalGroups.entrySet()) {
                String tagString = entry.getKey();
                Set<String> items = entry.getValue();

                // 记录到总览
                JsonArray arr = new JsonArray();
                items.forEach(arr::add);
                summaryJson.add("#" + tagString, arr);

                // 生成单独的可折叠分组 JSON
                ResourceLocation tagLoc = ResourceLocation.parse(tagString);
                String safeName = "tess_" + tagLoc.getNamespace() + "_" + tagLoc.getPath().replace("/", "_");

                JsonObject groupJson = new JsonObject();
                groupJson.addProperty("schema_version", 1);
                groupJson.addProperty("id", safeName);

                JsonObject nameObj = new JsonObject();
                nameObj.addProperty("fallback", "#" + tagString);
                groupJson.add("name", nameObj);
                groupJson.addProperty("enabled", true);

                JsonObject filterObj = new JsonObject();
                JsonArray itemsArray = new JsonArray();
                for (String it : items) {
                    JsonObject node = new JsonObject();
                    node.addProperty("type", "item");
                    node.addProperty("id", it);
                    itemsArray.add(node);
                }
                filterObj.add("any", itemsArray);
                groupJson.add("filter", filterObj);

                Files.writeString(outDir.resolve(safeName + ".json"), GSON.toJson(groupJson), StandardCharsets.UTF_8);
                groupCount++;
            }

            // 未成功分类的零散/无Tag物品写入一个单独的兜底组
            if (!untaggedOrUnique.isEmpty()) {
                JsonArray arr = new JsonArray();
                untaggedOrUnique.forEach(arr::add);
                summaryJson.add("other", arr);
            }

            Files.writeString(outDir.resolve("_summary_tags.json"), GSON.toJson(summaryJson), StandardCharsets.UTF_8);

            source.sendSuccess(() -> Component.literal("§a分类完成！算法已自动过滤大类，生成了 " +  " 个具体的独立分类，存入: config/collapsiblegroups/groups/classified/"), false);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("分类出错: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 当几个叶子 Tag 涵盖物品完全相同时（如 c:swords 和 minecraft:swords）：
     * 1. 优先选择社区通用标签命名空间 (c: 或 forge:)
     * 2. 其次选择字符更短更直观的命名
     */
    private static TagKey<Item> chooseCanonicalTag(List<TagKey<Item>> tags, Map<TagKey<Item>, Set<String>> tagMembers) {
        // 先按群体规模排序，优先取数量最少（定义最严）的
        tags.sort(Comparator.comparingInt(t -> tagMembers.get(t).size()));

        int minSize = tagMembers.get(tags.get(0)).size();
        List<TagKey<Item>> smallestTags = tags.stream()
                .filter(t -> tagMembers.get(t).size() == minSize)
                .toList();

        for (TagKey<Item> t : smallestTags) {
            if ("c".equals(t.location().getNamespace()) || "forge".equals(t.location().getNamespace())) {
                return t;
            }
        }
        for (TagKey<Item> t : smallestTags) {
            if ("minecraft".equals(t.location().getNamespace())) {
                return t;
            }
        }
        return smallestTags.get(0);
    }
    private static int exportAttributes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // 导出文件保存到运行目录下的 dumps/ 文件夹
        File dumpDir = new File("dumps");
        if (!dumpDir.exists()) {
            dumpDir.mkdirs();
        }

        File txtFile = new File(dumpDir, "attributes.txt");
        File jsonFile = new File(dumpDir, "attributes.json");

        JsonArray jsonArray = new JsonArray();

        try (FileWriter txtWriter = new FileWriter(txtFile, StandardCharsets.UTF_8);
             FileWriter jsonWriter = new FileWriter(jsonFile, StandardCharsets.UTF_8)) {

            txtWriter.write(String.format("%-45s | %-12s | %-12s | %-12s | %s\n",
                    "Attribute ID", "Default Val", "Min Val", "Max Val", "Syncable"));
            txtWriter.write("-".repeat(110) + "\n");

            int count = 0;

            // 遍历所有已注册的 Attribute
            for (Holder.Reference<Attribute> holder : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
                ResourceLocation id = holder.key().location();
                Attribute attribute = holder.value();

                double defaultValue = attribute.getDefaultValue();
                boolean syncable = attribute.isClientSyncable();

                // 绝大多数生物属性都是 RangedAttribute（带有最大值、最小值限制）
                double min = Double.NaN;
                double max = Double.NaN;
                if (attribute instanceof RangedAttribute ranged) {
                    min = ranged.getMinValue();
                    max = ranged.getMaxValue();
                }

                // 1. 写入 TXT 文本表格
                txtWriter.write(String.format("%-45s | %-12.2f | %-12s | %-12s | %b\n",
                        id.toString(),
                        defaultValue,
                        Double.isNaN(min) ? "N/A" : String.format("%.2f", min),
                        Double.isNaN(max) ? "N/A" : String.format("%.2f", max),
                        syncable
                ));

                // 2. 写入 JSON 结构
                JsonObject obj = new JsonObject();
                obj.addProperty("id", id.toString());
                obj.addProperty("translation_key", attribute.getDescriptionId());
                obj.addProperty("default_value", defaultValue);
                if (!Double.isNaN(min)) obj.addProperty("min_value", min);
                if (!Double.isNaN(max)) obj.addProperty("max_value", max);
                obj.addProperty("syncable", syncable);
                jsonArray.add(obj);

                count++;
            }

            // 输出 JSON 到文件
            GSON.toJson(jsonArray, jsonWriter);

            source.sendSuccess(() -> Component.literal("§a成功导出 "
                    + "§eTXT 文件保存在: §f" + txtFile.getAbsolutePath() + "\n"
                    + "§eJSON 文件保存在: §f" + jsonFile.getAbsolutePath()), false);
            return count;

        } catch (IOException e) {
            source.sendFailure(Component.literal("§c导出失败，发生 IO 错误: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    private static int gen(CommandSourceStack src, String outDirStr) {
        Path outDir = Paths.get(outDirStr).toAbsolutePath();
        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            src.sendFailure(Component.literal("创建目录失败: " + e));
            return 0;
        }

        JsonObject values = new JsonObject();
        int ok = 0, fail = 0;

        for (String idStr : SpellTraitRegistrar.WHITELIST) {
            ResourceLocation spellId = ResourceLocation.tryParse(idStr);
            if (spellId == null) { fail++; continue; }

            AbstractSpell spell = SpellRegistry.REGISTRY.get(spellId);
            if (spell == null || !spell.isEnabled()) { fail++; continue; }

            ResourceLocation traitId = SpellTraitRegistrar.traitIdOf(idStr);
            if (traitId == null) { fail++; continue; }

            try {
                values.add(traitId.toString(), buildConfig(spell));
                ok++;
            } catch (Throwable t) {
                fail++;
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty("replace", false);
        root.add("values", values);

        Path file = outDir.resolve("trait_data.json");
        try {
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            src.sendFailure(Component.literal("写文件失败: " + e));
            return 0;
        }

        int fOk = ok, fFail = fail;
        src.sendSuccess(() -> Component.literal(
                "生成完成: 成功 " + fOk + "，失败 " + fFail + "\n文件: " + file), true);
        return ok;
    }

    private static JsonObject buildConfig(AbstractSpell spell) {
        int rarity = spell.getRarity(1).getValue();


        int minLevel = switch (rarity) {
            case 0 -> 20;
            case 1 -> 50;
            case 2 -> 100;
            case 3 -> 200;
            default -> 400;
        };
        int weight  = Math.max(10, 100 - rarity * 15);
        int maxRank = Math.max(1, spell.getMaxLevel());

        int rawMana = spell.getManaCost(1);
        boolean continuous = spell.getCastType() == CastType.CONTINUOUS;
        double effectiveMana = continuous ? rawMana * 20.0 : Math.max(10, rawMana);
        int cost = (int) Math.round(
                Math.sqrt(effectiveMana)
                        * (1.0 + rarity * 0.4)
                        * (1.0 + Math.sqrt(maxRank) * 0.4));
        if (cost < 1) cost = 1;

        JsonObject o = new JsonObject();
        o.addProperty("cost", cost);
        o.addProperty("weight", weight);
        o.addProperty("max_rank", maxRank);
        o.addProperty("min_level", minLevel);
        return o;
    }
}