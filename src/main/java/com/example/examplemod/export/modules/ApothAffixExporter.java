package com.example.examplemod.export.modules;

import com.example.examplemod.export.core.ExportIO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.nio.file.Path;
import java.util.*;

public final class ApothAffixExporter {

    private static final List<String> RARITY_ORDER = List.of(
            "apotheosis:common",
            "apotheosis:uncommon",
            "apotheosis:rare",
            "apotheosis:epic",
            "apotheosis:mythic",
            "apotheosis:ancient"
    );

    private ApothAffixExporter() {
    }

    public static Path export() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            throw new IllegalStateException("需要在进入世界后执行客户端导出（需要 Level 与注册表上下文）");
        }

        Item.TooltipContext itemCtx = Item.TooltipContext.of(mc.level);
        TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        AttributeTooltipContext tooltipCtx = AttributeTooltipContext.of(mc.player, itemCtx, flag);
        DynamicOps<JsonElement> ops = mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);

        List<LootCategory> categories = collectCategories();
        List<RarityEntry> rarities = collectSortedRarities();
        List<ItemStack> probeStacks = buildProbeStacks();

        List<Affix> allAffixes = new ArrayList<>(AffixRegistry.INSTANCE.getValues());
        allAffixes.sort(Comparator.comparing(a -> a.id().toString()));

        List<AffixRecord> records = new ArrayList<>(allAffixes.size());
        for (Affix affix : allAffixes) {
            records.add(analyzeAffix(affix, categories, rarities, probeStacks, tooltipCtx, ops));
        }

        try {
            Path outDir = ExportIO.resolve("apoth");
            ExportIO.writeText(outDir.resolve("01_神化词缀总表.md"),
                    renderAffixMasterMarkdown(mc, records, rarities));
            ExportIO.writeText(outDir.resolve("02_神化全槽位对照表.md"),
                    renderSlotOverviewMarkdown(categories, rarities, allAffixes, probeStacks, tooltipCtx));
            ExportIO.writeJson(outDir.resolve("apoth_affixes.json"),
                    renderAffixJson(mc, records));
            return outDir;
        } catch (Exception e) {
            throw new RuntimeException("导出神化词缀失败: " + e.getMessage(), e);
        }
    }

    private static List<LootCategory> collectCategories() {
        List<LootCategory> list = new ArrayList<>();
        for (LootCategory cat : Apoth.BuiltInRegs.LOOT_CATEGORY) {
            if (!cat.isNone()) {
                list.add(cat);
            }
        }
        list.sort(Comparator.comparing(c -> c.getKey().toString()));
        return list;
    }

    private static List<RarityEntry> collectSortedRarities() {
        List<RarityEntry> list = new ArrayList<>();
        for (LootRarity rarity : RarityRegistry.INSTANCE.getValues()) {
            DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(rarity);
            ResourceLocation id = RarityRegistry.INSTANCE.getKey(rarity);
            if (id != null && holder.isBound()) {
                list.add(new RarityEntry(id, rarity, holder));
            }
        }
        list.sort(Comparator.comparingInt((RarityEntry r) -> {
            int idx = RARITY_ORDER.indexOf(r.id().toString());
            return idx >= 0 ? idx : Integer.MAX_VALUE;
        }).thenComparing(r -> r.id().toString()));
        return list;
    }

    /**
     * 提供常见代表性装备，避免部分词缀在 canApplyTo(ItemStack.EMPTY, ...) 时误判为 false。
     */
    private static List<ItemStack> buildProbeStacks() {
        return List.of(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.DIAMOND_AXE),
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.DIAMOND_SHOVEL),
                new ItemStack(Items.BOW),
                new ItemStack(Items.CROSSBOW),
                new ItemStack(Items.TRIDENT),
                new ItemStack(Items.MACE),
                new ItemStack(Items.SHIELD),
                new ItemStack(Items.DIAMOND_HELMET),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS),
                new ItemStack(Items.DIAMOND_BOOTS)
        );
    }

    private static ItemStack findFirstMatchingStack(
            Affix affix,
            LootCategory cat,
            LootRarity rarity,
            List<ItemStack> probeStacks
    ) {
        for (ItemStack stack : probeStacks) {
            try {
                if (affix.canApplyTo(stack, cat, rarity)) {
                    return stack;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static AffixRecord analyzeAffix(
            Affix affix,
            List<LootCategory> categories,
            List<RarityEntry> rarities,
            List<ItemStack> probeStacks,
            AttributeTooltipContext tooltipCtx,
            DynamicOps<JsonElement> ops
    ) {
        DynamicHolder<Affix> affixHolder = AffixRegistry.INSTANCE.holder(affix);
        String id = affix.id().toString();
        String type = String.valueOf(affix.definition().type());

        Set<String> matchedCategories = new LinkedHashSet<>();
        Map<String, RarityDesc> rarityDescriptions = new LinkedHashMap<>();

        for (RarityEntry rarity : rarities) {
            ItemStack sampleStackForRarity = null;
            for (LootCategory cat : categories) {
                ItemStack matched = findFirstMatchingStack(affix, cat, rarity.rarity(), probeStacks);
                if (matched != null) {
                    matchedCategories.add(cat.getKey().toString());
                    if (sampleStackForRarity == null) {
                        sampleStackForRarity = matched;
                    }
                }
            }

            if (sampleStackForRarity != null) {
                rarityDescriptions.put(
                        rarity.id().toString(),
                        renderDescriptions(affix, affixHolder, rarity.holder(), sampleStackForRarity, tooltipCtx)
                );
            }
        }

        // 若该词缀有特殊条件导致所有探测均未匹配，仍强行用最高可用稀有度抓一次描述，保证不丢失词缀
        if (rarityDescriptions.isEmpty() && !rarities.isEmpty()) {
            RarityEntry fallback = rarities.get(rarities.size() - 1);
            rarityDescriptions.put(
                    fallback.id().toString() + " (未匹配槽位探测)",
                    renderDescriptions(affix, affixHolder, fallback.holder(), ItemStack.EMPTY, tooltipCtx)
            );
        }

        JsonElement rawCodecJson = null;
        try {
            @SuppressWarnings("unchecked")
            Codec<Affix> codec = (Codec<Affix>) affix.getCodec();
            rawCodecJson = codec.encodeStart(ops, affix).result().orElse(null);
        } catch (Throwable ignored) {
        }

        return new AffixRecord(id, type, new ArrayList<>(matchedCategories), rarityDescriptions, rawCodecJson);
    }

    private static RarityDesc renderDescriptions(
            Affix affix,
            DynamicHolder<Affix> affixHolder,
            DynamicHolder<LootRarity> rarityHolder,
            ItemStack sampleStack,
            AttributeTooltipContext tooltipCtx
    ) {
        AffixInstance minInst = new AffixInstance(affixHolder, 0.0F, rarityHolder, sampleStack);
        AffixInstance maxInst = new AffixInstance(affixHolder, 1.0F, rarityHolder, sampleStack);

        String augmenting = safeText(() -> affix.getAugmentingText(maxInst, tooltipCtx));
        String minDesc = safeText(() -> affix.getDescription(minInst, tooltipCtx));
        String maxDesc = safeText(() -> affix.getDescription(maxInst, tooltipCtx));

        return new RarityDesc(augmenting, minDesc, maxDesc);
    }

    private static String safeText(ComponentSupplier supplier) {
        try {
            Component comp = supplier.get();
            return comp != null ? ExportIO.stripColor(comp.getString()).trim() : "";
        } catch (Throwable t) {
            return "[渲染异常: " + t.getClass().getSimpleName() + "]";
        }
    }

    private static String renderAffixMasterMarkdown(
            Minecraft mc,
            List<AffixRecord> records,
            List<RarityEntry> rarities
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 神化已注册词缀总表\n\n");
        sb.append("- **当前语言**: `").append(mc.getLanguageManager().getSelected()).append("`\n");
        sb.append("- **已注册词缀总数**: ").append(records.size()).append("\n");
        sb.append("- **已注册稀有度**: ");
        for (int i = 0; i < rarities.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append("`").append(rarities.get(i).id()).append("`");
        }
        sb.append("\n\n");
        sb.append("> **说明**：`重铸台描述` 来自 `getAugmentingText`；`面板描述(最低~最高)` 来自 `getDescription(0.0F / 1.0F)`。\n\n");

        sb.append("| 词缀 ID | 类型 | 适用部位 | 适用稀有度与描述 |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");

        for (AffixRecord rec : records) {
            String cats = rec.categories().isEmpty()
                    ? "*(特殊条件/未匹配)*"
                    : String.join(", ", rec.categories().stream().map(c -> "`" + c.replace("apotheosis:", "") + "`").toList());

            StringBuilder descCol = new StringBuilder();
            for (Map.Entry<String, RarityDesc> e : rec.rarityDescriptions().entrySet()) {
                if (!descCol.isEmpty()) descCol.append("<br>");
                String rarityShort = e.getKey().replace("apotheosis:", "");
                RarityDesc d = e.getValue();

                String line;
                if (!d.augmenting().isEmpty()) {
                    line = d.augmenting();
                    if (!d.minDesc().equals(d.maxDesc()) && !d.minDesc().isEmpty()) {
                        line += " *(面板: " + d.minDesc() + " ~ " + d.maxDesc() + ")*";
                    }
                } else if (d.minDesc().equals(d.maxDesc())) {
                    line = d.maxDesc();
                } else {
                    line = d.minDesc() + " ~ " + d.maxDesc();
                }

                descCol.append("**[").append(rarityShort).append("]** ")
                        .append(ExportIO.stripColorAndCleanCell(line));
            }

            sb.append("| `").append(rec.id()).append("` | ")
                    .append(ExportIO.stripColorAndCleanCell(rec.type())).append(" | ")
                    .append(ExportIO.stripColorAndCleanCell(cats)).append(" | ")
                    .append(descCol).append(" |\n");
        }
        return sb.toString();
    }

    private static String renderSlotOverviewMarkdown(
            List<LootCategory> categories,
            List<RarityEntry> rarities,
            List<Affix> allAffixes,
            List<ItemStack> probeStacks,
            AttributeTooltipContext tooltipCtx
    ) {
        RarityEntry targetRarity = rarities.stream()
                .filter(r -> r.id().toString().equals("apotheosis:mythic"))
                .findFirst()
                .orElse(rarities.isEmpty() ? null : rarities.get(rarities.size() - 1));

        Collection<Gem> allGems = GemRegistry.INSTANCE.getValues().stream()
                .sorted(Comparator.comparing(g -> g.getId().toString()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("# 神化全槽位词缀与宝石对照表\n\n");
        if (targetRarity != null) {
            sb.append("> 词缀示例按稀有度 `").append(targetRarity.id()).append("` 满 Roll 展示；若某词缀不支持该稀有度，则取其最高可用稀有度。\n\n");
        }

        for (LootCategory cat : categories) {
            sb.append("## 部位: `").append(cat.getKey()).append("`\n\n");

            sb.append("### 可洗出词缀\n\n");
            sb.append("| 词缀 ID | 类型 | 示例稀有度 | 描述 |\n");
            sb.append("| :--- | :--- | :--- | :--- |\n");

            int affixCount = 0;
            for (Affix affix : allAffixes) {
                RarityEntry matchedRarity = null;
                ItemStack matchedStack = null;

                // 优先用 targetRarity，不匹配则向下找该词缀在该部位支持的最高稀有度
                if (targetRarity != null) {
                    matchedStack = findFirstMatchingStack(affix, cat, targetRarity.rarity(), probeStacks);
                    if (matchedStack != null) matchedRarity = targetRarity;
                }
                if (matchedRarity == null) {
                    for (int i = rarities.size() - 1; i >= 0; i--) {
                        RarityEntry r = rarities.get(i);
                        ItemStack s = findFirstMatchingStack(affix, cat, r.rarity(), probeStacks);
                        if (s != null) {
                            matchedRarity = r;
                            matchedStack = s;
                            break;
                        }
                    }
                }

                if (matchedRarity != null) {
                    affixCount++;
                    RarityDesc desc = renderDescriptions(
                            affix, AffixRegistry.INSTANCE.holder(affix),
                            matchedRarity.holder(), matchedStack, tooltipCtx);
                    String text = !desc.augmenting().isEmpty() ? desc.augmenting() : desc.maxDesc();
                    sb.append("| `").append(affix.id()).append("` | ")
                            .append(affix.definition().type()).append(" | `")
                            .append(matchedRarity.id()).append("` | ")
                            .append(ExportIO.stripColorAndCleanCell(text)).append(" |\n");
                }
            }
            if (affixCount == 0) {
                sb.append("| *(无)* | - | - | - |\n");
            }
            sb.append("\n");

            sb.append("### 可镶嵌宝石\n\n");
            sb.append("| 宝石 ID | 最低纯度效果 | 完美纯度效果 |\n");
            sb.append("| :--- | :--- | :--- |\n");

            int gemCount = 0;
            for (Gem gem : allGems) {
                DynamicHolder<Gem> gemHolder = GemRegistry.INSTANCE.holder(gem);
                GemInstance minInst = new GemInstance(gemHolder, cat, gem.getMinPurity(), ItemStack.EMPTY, 0);
                GemInstance perfectInst = new GemInstance(gemHolder, cat, Purity.PERFECT, ItemStack.EMPTY, 0);

                if (perfectInst.getBonus().isPresent()) {
                    gemCount++;
                    String minText = safeText(() -> minInst.getSocketBonusTooltip(tooltipCtx));
                    String perfText = safeText(() -> perfectInst.getSocketBonusTooltip(tooltipCtx));
                    String tag = gem.isUnique() ? " `[唯一]`" : "";
                    sb.append("| `").append(gem.getId()).append("`").append(tag)
                            .append(" | ").append(ExportIO.stripColorAndCleanCell(minText))
                            .append(" | ").append(ExportIO.stripColorAndCleanCell(perfText))
                            .append(" |\n");
                }
            }
            if (gemCount == 0) {
                sb.append("| *(无)* | - | - |\n");
            }
            sb.append("\n---\n\n");
        }
        return sb.toString();
    }

    private static JsonObject renderAffixJson(Minecraft mc, List<AffixRecord> records) {
        JsonObject root = new JsonObject();
        root.addProperty("language", mc.getLanguageManager().getSelected());
        root.addProperty("count", records.size());

        JsonArray arr = new JsonArray();
        for (AffixRecord rec : records) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", rec.id());
            obj.addProperty("type", rec.type());

            JsonArray cats = new JsonArray();
            rec.categories().forEach(cats::add);
            obj.add("categories", cats);

            JsonObject descByRarity = new JsonObject();
            for (Map.Entry<String, RarityDesc> e : rec.rarityDescriptions().entrySet()) {
                JsonObject rObj = new JsonObject();
                rObj.addProperty("augmenting_text", e.getValue().augmenting());
                rObj.addProperty("min_description", e.getValue().minDesc());
                rObj.addProperty("max_description", e.getValue().maxDesc());
                descByRarity.add(e.getKey(), rObj);
            }
            obj.add("descriptions", descByRarity);

            if (rec.rawConfig() != null) {
                obj.add("raw_config", rec.rawConfig());
            }
            arr.add(obj);
        }
        root.add("affixes", arr);
        return root;
    }

    @FunctionalInterface
    private interface ComponentSupplier {
        Component get();
    }

    private record RarityEntry(ResourceLocation id, LootRarity rarity, DynamicHolder<LootRarity> holder) {
    }

    private record RarityDesc(String augmenting, String minDesc, String maxDesc) {
    }

    private record AffixRecord(
            String id,
            String type,
            List<String> categories,
            Map<String, RarityDesc> rarityDescriptions,
            JsonElement rawConfig
    ) {
    }
}