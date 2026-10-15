package com.nf121.examplemod.command;

import com.nf121.examplemod.command.data.DataExporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;
import java.util.*;

@EventBusSubscriber
public class ExportWeaponsCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("packdev_export")
                .requires(source -> source.hasPermission(2))
                .executes(context -> execute(
                    context.getSource(),
                    "../.packdev/index.json"
                ))
                .then(
                    Commands.argument("path", StringArgumentType.greedyString())
                        .executes(context -> execute(
                            context.getSource(),
                            StringArgumentType.getString(context, "path")
                        ))
                )
        );
        dispatcher.register(
                Commands.literal("dumpmonster")
                        .executes(ctx -> {
                            var level = ctx.getSource().getLevel();
                            Set<Class<?>> classes = new HashSet<>();
                            Map<Class<?>, EntityType<?>> classToType = new HashMap<>();

                            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                                Entity instance = type.create(level);
                                if (instance == null) continue;
                                Class<?> clazz = instance.getClass();
                                instance.discard();
                                if (!(instance instanceof Monster)) continue;
                                classes.add(clazz);
                                classToType.put(clazz, type);
                            }

                            Map<Class<?>, TreeSet<String>> byParent = new TreeMap<>(
                                    Comparator.comparing(Class::getName));

                            for (Class<?> clazz : classes) {
                                boolean hasChild = false;
                                for (Class<?> other : classes) {
                                    if (other != clazz && clazz.isAssignableFrom(other)) {
                                        hasChild = true;
                                        break;
                                    }
                                }
                                if (hasChild) continue;

                                Class<?> parent = clazz.getSuperclass();
                                String entry = clazz.getName() + "    "
                                        + BuiltInRegistries.ENTITY_TYPE.getKey(classToType.get(clazz));
                                byParent.computeIfAbsent(parent, k -> new TreeSet<>()).add(entry);
                            }

                            StringBuilder sb = new StringBuilder();
                            byParent.forEach((parent, leaves) -> {
                                sb.append("=== ").append(parent.getName()).append(" ===\n");
                                for (String leaf : leaves) {
                                    sb.append("  ").append(leaf).append('\n');
                                }
                            });
                            System.out.println(sb);
                            ctx.getSource().sendSuccess(() -> Component.literal("已打印"), false);
                            return 1;
                        })
        );
    }

    private static int execute(CommandSourceStack source, String pathText) {
        try {
            Path path = Path.of(pathText);
            DataExporter.ExportResult result = DataExporter.export(source.getServer(), path);

            source.sendSuccess(
                () -> Component.literal(String.format(
                    "§a[PackDev] 数据导出成功！\n§7路径: §f%s\n§7注册表: §e%d§7 (共 §e%d§7 个值)\n§7枚举类: §e%d§7 (共 §e%d§7 个值)",
                    result.path(),
                    result.registryCount(),
                    result.registryValueCount(),
                    result.enumCount(),
                    result.enumValueCount()
                )),
                false
            );

            return result.registryValueCount() + result.enumValueCount();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("PackDev export failed: " + exception.getMessage()));
            exception.printStackTrace();
            return 0;
        }
    }
}