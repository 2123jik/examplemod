package com.example.examplemod.export;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.export.core.ModGuard;
import com.example.examplemod.export.modules.MonsterDumpExporter;
import com.example.examplemod.export.modules.PackIndexExporter;
import com.example.examplemod.export.modules.TrimDataExporter;
import com.example.examplemod.export.modules.WeaponTooltipExporter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;

public final class ExportCommands {

    private ExportCommands() {
    }

    // ==================== 服务端指令: /devtools ====================
    @EventBusSubscriber(modid = ExampleMod.MODID)
    public static class ServerSide {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(
                    Commands.literal("devtools")
                            .requires(source -> source.hasPermission(2))
                            // /devtools index [path]
                            .then(Commands.literal("index")
                                    .executes(ctx -> {
                                        var res = PackIndexExporter.run(ctx.getSource().getServer(), null);
                                        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                                                "§a[PackDev] 导出成功！\n§7路径: §f%s\n§7注册表: §e%d§7 (%d 值) | 枚举: §e%d§7 (%d 值)",
                                                res.path(), res.registryCount(), res.registryValueCount(),
                                                res.enumCount(), res.enumValueCount()
                                        )), false);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("path", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                Path custom = Path.of(StringArgumentType.getString(ctx, "path"));
                                                var res = PackIndexExporter.run(ctx.getSource().getServer(), custom);
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "§a[PackDev] 已导出到: §f" + res.path()), false);
                                                return Command.SINGLE_SUCCESS;
                                            })))
                            // /devtools monsters
                            .then(Commands.literal("monsters")
                                    .executes(ctx -> {
                                        Path out = MonsterDumpExporter.export(ctx.getSource().getLevel());
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§a[Monsters] 怪物类层级已导出至: §f" + out), false);
                                        return Command.SINGLE_SUCCESS;
                                    }))
                            // /devtools trims
                            .then(Commands.literal("trims")
                                    .executes(ctx -> {
                                        Path out = TrimDataExporter.export(ctx.getSource().getServer());
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§a[Trims] 盔甲纹饰已导出至: §f" + out), false);
                                        return Command.SINGLE_SUCCESS;
                                    }))
                            // /devtools apoth_studio
                            .then(Commands.literal("apoth_studio")
                                    .executes(ctx -> {
                                        if (!ModGuard.isLoaded("apotheosis")) {
                                            ctx.getSource().sendFailure(Component.literal("§c未安装 Apotheosis 模组"));
                                            return 0;
                                        }
                                        Path out = com.example.examplemod.export.modules.ApothStudioExporter
                                                .export(ctx.getSource().registryAccess());
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§a[ApothStudio] 数据底模已打包至: §f" + out), false);
                                        return Command.SINGLE_SUCCESS;
                                    }))
            );
        }
    }

    // ==================== 客户端指令: /devtools_c ====================
    @EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
    public static class ClientSide {

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(
                    Commands.literal("devtools_c")
                            // /devtools_c apoth
                            .then(Commands.literal("apoth")
                                    .executes(ctx -> {
                                        if (!ModGuard.isLoaded("apotheosis")) {
                                            ctx.getSource().sendFailure(Component.literal("§c未安装 Apotheosis 模组"));
                                            return 0;
                                        }
                                        Path dir = com.example.examplemod.export.modules.ApothAffixExporter.export();
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§a[Apoth] 词缀总表、描述与槽位对照已导出至: §f" + dir), false);
                                        return Command.SINGLE_SUCCESS;
                                    }))
                            // /devtools_c spells
                            .then(Commands.literal("spells")
                                    .executes(ctx -> {
                                        if (!ModGuard.isLoaded("irons_spellbooks")) {
                                            ctx.getSource().sendFailure(Component.literal("§c未安装 Iron's Spellbooks 模组"));
                                            return 0;
                                        }
                                        Path out = com.example.examplemod.export.modules.SpellDataExporter.export();
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§a[Spells] 铁魔法法术表已导出至: §f" + out), false);
                                        return Command.SINGLE_SUCCESS;
                                    }))
                            // /devtools_c weapons [all | delay [all]]
                            .then(Commands.literal("weapons")
                                    .executes(ctx -> {
                                        WeaponTooltipExporter.exportNow(false);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.literal("all")
                                            .executes(ctx -> {
                                                WeaponTooltipExporter.exportNow(true);
                                                return Command.SINGLE_SUCCESS;
                                            }))
                                    .then(Commands.literal("delay")
                                            .executes(ctx -> {
                                                WeaponTooltipExporter.scheduleDelay(false, 60);
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "§e将在 3 秒后导出武器描述（现在可按住 Shift/Ctrl 展开折叠提示）..."), false);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                            .then(Commands.literal("all")
                                                    .executes(ctx -> {
                                                        WeaponTooltipExporter.scheduleDelay(true, 60);
                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                "§e将在 3 秒后导出全部物品描述..."), false);
                                                        return Command.SINGLE_SUCCESS;
                                                    }))))
            );
        }
    }
}