package com.example.examplemod.command;

import com.example.examplemod.command.data.DataExporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;

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