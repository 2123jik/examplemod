package com.example.examplemod.export.modules;

import com.example.examplemod.export.core.ExportIO;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.*;

public final class SpellDataExporter {

    private SpellDataExporter() {
    }

    public static Path export() {
        Registry<AbstractSpell> registry = SpellRegistry.REGISTRY;
        if (registry == null) {
            throw new IllegalStateException("SpellRegistry.REGISTRY 为 null");
        }

        Map<String, List<AbstractSpell>> bySchool = new TreeMap<>();
        List<AbstractSpell> enabledSpells = new ArrayList<>();

        for (AbstractSpell spell : registry) {
            if (spell == null) continue;
            try {
                if (!spell.isEnabled()) continue;
            } catch (Throwable t) {
                continue;
            }
            enabledSpells.add(spell);
            String school;
            try {
                school = spell.getSchoolType().getDisplayName().getString();
                if (school.startsWith("school.")) school = "(未本地化) " + school;
            } catch (Throwable t) {
                school = "Unknown";
            }
            bySchool.computeIfAbsent(school, k -> new ArrayList<>()).add(spell);
        }

        StringBuilder w = new StringBuilder();
        w.append("# 铁魔法法术大表\n\n")
                .append("> 已启用法术数：").append(enabledSpells.size())
                .append("（注册表总数：").append(registry.size()).append("）\n\n")
                .append("## 学派索引\n\n");

        bySchool.forEach((school, list) ->
                w.append("- **").append(school).append("**（").append(list.size()).append(" 个）\n"));
        w.append("\n---\n\n");

        for (var entry : bySchool.entrySet()) {
            List<AbstractSpell> spells = entry.getValue();
            spells.sort(Comparator.comparing(AbstractSpell::getSpellId));

            w.append("## ").append(entry.getKey()).append("（").append(spells.size()).append(" 个）\n\n")
                    .append("| 法术名 | ID | 施法类型 | 最高等级 | 基础蓝耗 | 描述 |\n")
                    .append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");

            for (AbstractSpell spell : spells) {
                String id = safeCall(spell::getSpellId, "?");
                String nameKey = safeCall(spell::getComponentId, "");
                String name = nameKey.isEmpty() ? "?" : Component.translatable(nameKey).getString();
                if (name.equals(nameKey)) name = "⚠️ " + nameKey;

                String desc = "-";
                if (!nameKey.isEmpty()) {
                    String guideKey = nameKey + ".guide";
                    String guide = Component.translatable(guideKey).getString();
                    if (!guide.equals(guideKey)) {
                        desc = ExportIO.stripColorAndCleanCell(guide);
                    }
                }

                CastType ct = safeCall(spell::getCastType, null);
                String castType = ct != null ? ct.name() : "?";
                int maxLevel = safeCall(spell::getMaxLevel, 1);
                int manaCost = safeCall(() -> spell.getManaCost(1), 0);

                w.append("| ").append(ExportIO.stripColorAndCleanCell(name))
                        .append(" | `").append(id)
                        .append("` | ").append(castType)
                        .append(" | ").append(maxLevel)
                        .append(" | ").append(manaCost)
                        .append(" | ").append(desc).append(" |\n");
            }
            w.append("\n---\n\n");
        }

        w.append("## 全部已启用法术 ID\n\n```\n");
        enabledSpells.stream()
                .map(AbstractSpell::getSpellId)
                .sorted()
                .forEach(id -> w.append(id).append('\n'));
        w.append("```\n");

        try {
            return ExportIO.writeText(ExportIO.resolve("iron_spells.md"), w.toString());
        } catch (Exception e) {
            throw new RuntimeException("导出铁魔法数据失败: " + e.getMessage(), e);
        }
    }

    private static <T> T safeCall(ThrowingSupplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}