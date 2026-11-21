package com.example.examplemod.export.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.BufferedWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class EquipmentExportCommand {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static int pendingShiftTicks = -1;

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("export_gear")
                // 立即导出普通 Tooltip
                .executes(ctx -> runExport())
                // /export_gear shift -> 3秒倒计时，按住 Shift 抓取展开后的完整模组说明
                .then(Commands.literal("shift").executes(ctx -> {
                    pendingShiftTicks = 60; // 3秒 (20 tick/s)
                    ctx.getSource().sendSuccess(
                        () -> Component.literal("§e请在 3 秒内按住 [Shift] 键不要松开，倒计时结束后自动导出完整 Tooltip..."),
                        false
                    );
                    return 1;
                }))
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (pendingShiftTicks < 0) return;

        if (pendingShiftTicks == 0) {
            pendingShiftTicks = -1;
            runExport();
        } else {
            pendingShiftTicks--;
        }
    }

    private static int runExport() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return 0;

        try {
            Path dumpDir = FMLPaths.GAMEDIR.get().resolve("dumps");
            Files.createDirectories(dumpDir);

            List<GearEntry> entries = collectAllGear(mc);

            // 1. 导出 JSON（结构化数据）
            Path jsonFile = dumpDir.resolve("equipment_export.json");
            JsonArray jsonArray = new JsonArray();
            for (GearEntry e : entries) {
                jsonArray.add(e.toJson());
            }
            try (Writer writer = Files.newBufferedWriter(jsonFile, StandardCharsets.UTF_8)) {
                GSON.toJson(jsonArray, writer);
            }

            // 2. 导出 Markdown（给人直接阅读）
            Path mdFile = dumpDir.resolve("equipment_export.md");
            writeMarkdown(mdFile, entries);

            mc.player.sendSystemMessage(Component.literal(
                "§a成功导出 " + entries.size() + " 件武器/装备！\n" +
                "§7阅读版: §f" + mdFile.toAbsolutePath() + "\n" +
                "§7数据版: §f" + jsonFile.toAbsolutePath()
            ));
            return entries.size();
        } catch (Exception e) {
            mc.player.sendSystemMessage(Component.literal("§c导出失败: " + e.getMessage()));
            return 0;
        }
    }

    private static List<GearEntry> collectAllGear(Minecraft mc) {
        List<GearEntry> result = new ArrayList<>();
        Item.TooltipContext tooltipCtx = Item.TooltipContext.of(mc.level);

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;

            try {
                ItemStack stack = new ItemStack(item);
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                ItemAttributeModifiers modifiers = stack.getAttributeModifiers();

                Set<String> categories = detectCategories(item, stack, modifiers);
                if (categories.isEmpty()) continue;

                String name = strip(stack.getHoverName().getString());

                // 抓取完整 Tooltip（含 Item#appendHoverText 与 NeoForge ItemTooltipEvent 注入的词条）
                List<String> tooltipLines = new ArrayList<>();
                try {
                    List<Component> rawLines = stack.getTooltipLines(tooltipCtx, mc.player, TooltipFlag.Default.NORMAL);
                    for (Component line : rawLines) {
                        String text = strip(line.getString());
                        if (!text.isBlank()) {
                            tooltipLines.add(text);
                        }
                    }
                } catch (Throwable t) {
                    tooltipLines.add("[Tooltip 读取异常: " + t.getClass().getSimpleName() + "]");
                }

                result.add(new GearEntry(
                    id.toString(),
                    id.getNamespace(),
                    name,
                    stack.getRarity().name().toLowerCase(Locale.ROOT),
                    stack.getMaxDamage(),
                    new ArrayList<>(categories),
                    modifiers,
                    tooltipLines
                ));
            } catch (Throwable ignored) {
                // 跳过极少数构造默认 ItemStack 异常的特殊物品，避免中断整体导出
            }
        }

        // 按 modid -> id 排序，方便浏览
        result.sort(Comparator.comparing((GearEntry e) -> e.modid).thenComparing(e -> e.id));
        return result;
    }

    private static Set<String> detectCategories(Item item, ItemStack stack, ItemAttributeModifiers modifiers) {
        Set<String> cats = new LinkedHashSet<>();

        boolean isCommonMiningTool = (item instanceof PickaxeItem || item instanceof ShovelItem || item instanceof HoeItem)
            && !stack.is(ItemTags.WEAPON_ENCHANTABLE);

        // 1. 武器类（剑、弓、弩、三叉戟、重锤、武器标签）
        if (item instanceof SwordItem
            || item instanceof ProjectileWeaponItem
            || item instanceof TridentItem
            || item instanceof MaceItem
            || stack.is(ItemTags.SWORDS)
            || stack.is(ItemTags.WEAPON_ENCHANTABLE)
            || stack.is(ItemTags.BOW_ENCHANTABLE)
            || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)
            || stack.is(ItemTags.TRIDENT_ENCHANTABLE)
            || stack.is(ItemTags.MACE_ENCHANTABLE)) {
            cats.add("weapon");
        }

        // 2. 斧类（很多模组战斧/大斧继承 AxeItem）
        if (item instanceof AxeItem || stack.is(ItemTags.AXES)) {
            cats.add("axe");
        }

        // 3. 防具 / 盾牌 / 鞘翅 / 可穿戴装备
        if (item instanceof ArmorItem
            || item instanceof ShieldItem
            || item instanceof ElytraItem
            || item instanceof Equipable
            || stack.is(ItemTags.ARMOR_ENCHANTABLE)
            || stack.is(ItemTags.EQUIPPABLE_ENCHANTABLE)) {
            cats.add("armor");
        }

        // 4. 其它自定义武器/法杖兜底（继承 TieredItem 但不是普通镐铲锄）
        if (item instanceof TieredItem && !isCommonMiningTool && cats.isEmpty()) {
            cats.add("weapon_or_tool");
        }

        // 5. 属性兜底（捕获 Iron's Spells 法杖、Mod 自定义武器/护甲等）
        if (!isCommonMiningTool && !modifiers.modifiers().isEmpty()) {
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.modifier().amount() > 0) {
                    cats.add("weapon");
                } else if (entry.attribute().is(Attributes.ARMOR) || entry.attribute().is(Attributes.ARMOR_TOUGHNESS)) {
                    cats.add("armor");
                } else {
                    cats.add("attributed_gear");
                }
            }
        }

        // 6. Curios 饰品 & NeoForge c:* 通用武器防具标签
        stack.getTags().forEach(tag -> {
            String ns = tag.location().getNamespace();
            String path = tag.location().getPath();
            if ("curios".equals(ns)) {
                cats.add("curio:" + path);
            } else if ("c".equals(ns) && (path.startsWith("armors") || path.contains("weapon") || path.equals("tools/shield"))) {
                cats.add("tag:" + path);
            }
        });

        return cats;
    }

    private static void writeMarkdown(Path mdFile, List<GearEntry> entries) throws Exception {
        // 按 modid 分组
        Map<String, List<GearEntry>> byMod = new LinkedHashMap<>();
        for (GearEntry e : entries) {
            byMod.computeIfAbsent(e.modid, k -> new ArrayList<>()).add(e);
        }

        try (BufferedWriter w = Files.newBufferedWriter(mdFile, StandardCharsets.UTF_8)) {
            w.write("# 武器与装备导出清单（共 " + entries.size() + " 件）\n\n");

            for (Map.Entry<String, List<GearEntry>> group : byMod.entrySet()) {
                w.write("## " + group.getKey() + " (" + group.getValue().size() + " 件)\n\n");
                for (GearEntry e : group.getValue()) {
                    w.write("### " + e.name + " (`" + e.id + "`)\n");
                    w.write("- **分类**: `" + String.join(", ", e.categories) + "`"
                        + " | **稀有度**: `" + e.rarity + "`"
                        + " | **耐久**: `" + (e.durability > 0 ? e.durability : "无限/无") + "`\n");

                    if (!e.tooltip.isEmpty()) {
                        w.write("- **Tooltip**:\n");
                        for (int i = 0; i < e.tooltip.size(); i++) {
                            String line = e.tooltip.get(i);
                            // 第一行通常就是物品名本身，与标题重复时可跳过
                            if (i == 0 && line.equals(e.name)) continue;
                            w.write("  > " + line + "\n");
                        }
                    }
                    w.write("\n");
                }
            }
        }
    }

    private static String strip(String s) {
        if (s == null) return "";
        String cleaned = ChatFormatting.stripFormatting(s);
        return cleaned != null ? cleaned.trim() : "";
    }

    private record GearEntry(
        String id,
        String modid,
        String name,
        String rarity,
        int durability,
        List<String> categories,
        ItemAttributeModifiers modifiers,
        List<String> tooltip
    ) {
        JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", id);
            obj.addProperty("modid", modid);
            obj.addProperty("name", name);
            obj.addProperty("rarity", rarity);
            obj.addProperty("durability", durability);

            JsonArray catArr = new JsonArray();
            categories.forEach(catArr::add);
            obj.add("categories", catArr);

            JsonArray attrArr = new JsonArray();
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                JsonObject a = new JsonObject();
                a.addProperty("attribute", entry.attribute().unwrapKey().map(k -> k.location().toString()).orElse("unknown"));
                a.addProperty("amount", entry.modifier().amount());
                a.addProperty("operation", entry.modifier().operation().name().toLowerCase(Locale.ROOT));
                a.addProperty("slot", entry.slot().name().toLowerCase(Locale.ROOT));
                attrArr.add(a);
            }
            if (!attrArr.isEmpty()) {
                obj.add("attributes", attrArr);
            }

            JsonArray tipArr = new JsonArray();
            tooltip.forEach(tipArr::add);
            obj.add("tooltip", tipArr);
            return obj;
        }
    }
}