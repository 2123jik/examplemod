package com.example.examplemod.tool;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = "examplemod")
public class SpellDataExporter {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("spell_dump")
                        .requires(source -> source.hasPermission(0))
                        .executes(ctx -> {
                            try {
                                exportSpellList();
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "§a[铁魔法法术导出成功！]§f 文件：.minecraft/spell_dump/00_铁魔法法术大表.md"
                                ), false);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("§c导出失败: " + e.getMessage()));
                                e.printStackTrace();
                            }
                            return 1;
                        })
        );
    }

    public static void exportSpellList() throws Exception {
        Registry<AbstractSpell> registry = SpellRegistry.REGISTRY;
        if (registry == null) throw new IllegalStateException("SpellRegistry.REGISTRY 为 null");

        File dir = FMLPaths.GAMEDIR.get().resolve("spell_dump").toFile();
        if (!dir.exists()) dir.mkdirs();
        File outputFile = new File(dir, "00_铁魔法法术大表.md");

        // 按学派分组
        Map<String, List<AbstractSpell>> bySchool = new TreeMap<>();
        for (AbstractSpell spell : registry) {
            if (spell == null) continue;
            try { if (!spell.isEnabled()) continue; } catch (Throwable t) { continue; }
            String school;
            try {
                school = spell.getSchoolType().getDisplayName().getString();
                if (school.startsWith("school.")) school = "(未本地化) " + school;
            } catch (Throwable t) {
                school = "Unknown";
            }
            bySchool.computeIfAbsent(school, k -> new ArrayList<>()).add(spell);
        }

        try (BufferedWriter w = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            w.write("# 铁魔法法术大表\n\n");
            w.write("> 描述来源：`spell.<ns>.<name>.guide` 键，从 en_us / zh_cn 任一 lang 读取。\n");
            w.write("> 若某法术无 `.guide` 键，描述显示为 `-`。\n");
            w.write("> **白名单用 ID 列**（反引号内即 ID）。\n\n");
            w.write("> 总法术数：" + registry.size() + "\n\n");

            // 学派索引
            w.write("## 📖 学派索引\n\n");
            for (String school : bySchool.keySet()) {
                w.write("- **" + school + "** （" + bySchool.get(school).size() + " 个）\n");
            }
            w.write("\n---\n\n");

            // 明细表
            for (Map.Entry<String, List<AbstractSpell>> entry : bySchool.entrySet()) {
                String school = entry.getKey();
                List<AbstractSpell> spells = entry.getValue();
                spells.sort(Comparator.comparing(AbstractSpell::getSpellId));

                w.write("## 🔮 " + school + "（" + spells.size() + " 个）\n\n");
                w.write("| 法术名 | ID | 施法类型 | 最高等级 | 基础蓝耗 | 描述 |\n");
                w.write("| :--- | :--- | :--- | :--- | :--- | :--- |\n");

                for (AbstractSpell spell : spells) {
                    String id;
                    try { id = spell.getSpellId(); } catch (Throwable t) { id = "?"; }

                    String nameKey = "";
                    try { nameKey = spell.getComponentId(); } catch (Throwable ignored) {}

                    // ⭐ 名称：从 lang 读（en_us 或 zh_cn）
                    String name = nameKey.isEmpty() ? "?" : Component.translatable(nameKey).getString();
                    if (name.equals(nameKey)) name = "⚠️ " + nameKey;  // 没翻译

                    // ⭐ 描述：nameKey + ".guide"
                    String desc = "-";
                    if (!nameKey.isEmpty()) {
                        String guideKey = nameKey + ".guide";
                        String guide = Component.translatable(guideKey).getString();
                        if (!guide.equals(guideKey)) {
                            desc = clean(guide);
                        }
                    }

                    String castType = "?";
                    try {
                        CastType ct = spell.getCastType();
                        if (ct != null) castType = ct.name();
                    } catch (Throwable ignored) {}

                    int maxLevel = 1;
                    try { maxLevel = spell.getMaxLevel(); } catch (Throwable ignored) {}

                    int manaCost = 0;
                    try { manaCost = spell.getManaCost(1); } catch (Throwable ignored) {}

                    w.write("| " + clean(name) + " | `" + id + "` | " + castType + " | "
                            + maxLevel + " | " + manaCost + " | " + desc + " |\n");
                }
                w.write("\n---\n\n");
            }

            // 纯 ID 块
            w.write("## 📋 全部法术 ID（每行一个，直接复制）\n\n```\n");
            List<String> ids = registry.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> { try { return s.isEnabled(); } catch (Throwable t) { return false; } })
                    .map(AbstractSpell::getSpellId)
                    .sorted().collect(Collectors.toList());
            for (String id : ids) { w.write(id); w.write("\n"); }
            w.write("```\n\n");

            // .guide 键块
            w.write("## 🌐 全部 .guide 描述键\n\n```\n");
            List<String> guides = registry.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> { try { return s.isEnabled(); } catch (Throwable t) { return false; } })
                    .sorted(Comparator.comparing(AbstractSpell::getSpellId))
                    .map(s -> {
                        try { return s.getComponentId() + ".guide"; }
                        catch (Throwable t) { return s.getSpellId() + " -> (失败)"; }
                    }).collect(Collectors.toList());
            for (String g : guides) { w.write(g); w.write("\n"); }
            w.write("```\n");
        }
    }

    private static String clean(String input) {
        if (input == null) return "";
        return input.replace("\n", " ").replace("\r", " ").replace("|", "\\|");
    }
}