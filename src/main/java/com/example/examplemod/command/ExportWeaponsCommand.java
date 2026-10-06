package com.example.examplemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

@EventBusSubscriber
public class ExportWeaponsCommand {

    // 监听指令注册事件
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("export_weapons")
                // 权限级别 2 (创造模式/管理员)，如果单机测试想随意使用可改为 permission >= 0
                .requires(source -> source.hasPermission(2))
                .executes(ExportWeaponsCommand::exportWeaponList)
        );
    }

    private static int exportWeaponList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // 收集所有可附魔/武器相关的物品集合
        Set<Item> collectedItems = new LinkedHashSet<>();

        // 1. 核心标签：1.21+ 官方武器附魔标签 (minecraft:enchantable/weapon)
        collectItemsFromTag(ItemTags.WEAPON_ENCHANTABLE, collectedItems);

        // 2. 补充标签（确保部分只打了刀剑/锐器附魔标签的模组武器也被囊括）
        collectItemsFromTag(ItemTags.SWORD_ENCHANTABLE, collectedItems);
        collectItemsFromTag(ItemTags.SHARP_WEAPON_ENCHANTABLE, collectedItems);

        if (collectedItems.isEmpty()) {
            source.sendFailure(Component.literal("未检索到任何符合可附魔标签的物品！"));
            return 0;
        }

        // 转换为排序后的 ResourceLocation 字符串列表
        List<String> outputLines = collectedItems.stream()
                .filter(item -> item != Items.AIR)
                .map(BuiltInRegistries.ITEM::getKey)
                .filter(Objects::nonNull)
                .map(ResourceLocation::toString)
                .sorted()
                .toList();

        // 导出文件目标路径：当前游戏目录/exported_enchantable_weapons.txt
        Path exportPath = Path.of("exported_enchantable_weapons.txt");

        try {
            // 写入文件（覆盖现有文件）
            Files.write(exportPath, outputLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 发送成功消息
            source.sendSuccess(() -> Component.literal(
                String.format("§a成功导出 §e%d §a个可附魔武器物品！\n§7文件保存在: §f%s",
                    outputLines.size(),
                    exportPath.toAbsolutePath()
                )
            ), true);

            return outputLines.size();
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c写入文件失败: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static void collectItemsFromTag(TagKey<Item> tagKey, Set<Item> targetSet) {
        BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(tag -> {
            for (Holder<Item> holder : tag) {
                targetSet.add(holder.value());
            }
        });
    }
}