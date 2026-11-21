package com.example.examplemod.export.modules;

import com.example.examplemod.export.core.ExportIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;

import java.nio.file.Path;
import java.util.*;

public final class MonsterDumpExporter {

    private MonsterDumpExporter() {
    }

    public static Path export(ServerLevel level) {
        Set<Class<?>> classes = new HashSet<>();
        Map<Class<?>, List<ResourceLocation>> classToIds = new HashMap<>();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Entity instance = null;
            try {
                instance = type.create(level);
                if (instance instanceof Monster) {
                    Class<?> clazz = instance.getClass();
                    classes.add(clazz);
                    classToIds.computeIfAbsent(clazz, k -> new ArrayList<>())
                            .add(BuiltInRegistries.ENTITY_TYPE.getKey(type));
                }
            } catch (Throwable ignored) {
            } finally {
                if (instance != null) {
                    instance.discard();
                }
            }
        }

        Map<Class<?>, TreeSet<String>> byParent = new TreeMap<>(Comparator.comparing(Class::getName));
        for (Class<?> clazz : classes) {
            boolean hasChild = classes.stream().anyMatch(other -> other != clazz && clazz.isAssignableFrom(other));
            if (hasChild) continue;

            Class<?> parent = clazz.getSuperclass();
            for (ResourceLocation id : classToIds.getOrDefault(clazz, List.of())) {
                byParent.computeIfAbsent(parent, k -> new TreeSet<>())
                        .add(clazz.getName() + "    " + id);
            }
        }

        StringBuilder sb = new StringBuilder();
        byParent.forEach((parent, leaves) -> {
            sb.append("=== ").append(parent.getName()).append(" ===\n");
            for (String leaf : leaves) {
                sb.append("  ").append(leaf).append('\n');
            }
        });

        try {
            return ExportIO.writeText(ExportIO.resolve("monsters.txt"), sb.toString());
        } catch (Exception e) {
            throw new RuntimeException("导出怪物列表失败: " + e.getMessage(), e);
        }
    }
}