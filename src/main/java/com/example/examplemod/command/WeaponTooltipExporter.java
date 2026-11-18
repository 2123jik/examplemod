package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.mojang.brigadier.Command;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class WeaponTooltipExporter {

    // 兼容常见模组的 c: 通用武器标签
    private static final TagKey<Item> C_MELEE_WEAPON =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/melee_weapon"));
    private static final TagKey<Item> C_RANGED_WEAPON =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/ranged_weapon"));
    private static final TagKey<Item> C_WEAPONS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "weapons"));

    private static int delayTicks = -1;
    private static boolean pendingExportAll = false;

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("exportweapons")
                        // /exportweapons -> 立即导出所有武器
                        .executes(ctx -> {
                            export(false);
                            return Command.SINGLE_SUCCESS;
                        })
                        // /exportweapons delay -> 3秒后导出（期间可以一直按住 Shift/Ctrl，把折叠描述一起导出）
                        .then(Commands.literal("delay")
                                .executes(ctx -> {
                                    delayTicks = 60;
                                    pendingExportAll = false;
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("将在 3 秒后导出武器描述（现在可按住 Shift / Ctrl 展开折叠信息）..."), false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        // /exportweapons all -> 导出全部已注册物品（防止某些法杖/枪械没被识别为武器）
                        .then(Commands.literal("all")
                                .executes(ctx -> {
                                    export(true);
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (delayTicks > 0) {
            delayTicks--;
        } else if (delayTicks == 0) {
            delayTicks = -1;
            export(pendingExportAll);
        }
    }

    private static void export(boolean exportAllItems) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // 与游戏内实际渲染逻辑保持一致（支持原版、模组 appendHoverText、ItemTooltipEvent 以及 F3+H 高级提示）
        Item.TooltipContext tooltipContext = Item.TooltipContext.of(mc.level);
        TooltipFlag tooltipFlag = mc.options.advancedItemTooltips
                ? TooltipFlag.Default.ADVANCED
                : TooltipFlag.Default.NORMAL;

        List<Map.Entry<ResourceKey<Item>, Item>> entries = BuiltInRegistries.ITEM.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .toList();

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (Map.Entry<ResourceKey<Item>, Item> entry : entries) {
            ResourceLocation id = entry.getKey().location();
            Item item = entry.getValue();
            ItemStack stack = new ItemStack(item);

            if (stack.isEmpty()) {
                continue;
            }
            if (!exportAllItems && !isWeapon(stack)) {
                continue;
            }

            count++;
            String displayName = stripFormatting(stack.getHoverName().getString());

            sb.append("==================================================\n");
            sb.append("[").append(count).append("] ").append(displayName)
                    .append(" (").append(id).append(")\n");
            sb.append("==================================================\n");

            try {
                List<String> lines = extractRenderedTooltipLines(mc, stack, tooltipContext, tooltipFlag);
                for (String line : lines) {
                    sb.append(line).append("\n");
                }
            } catch (Throwable t) {
                sb.append("[读取 Tooltip 时出错: ").append(t.getClass().getSimpleName())
                        .append(" - ").append(t.getMessage()).append("]\n");
            }
            sb.append("\n");
        }

        String header = (exportAllItems ? "全部物品" : "武器") + " Tooltip 导出完成\n"
                + "当前语言: " + mc.getLanguageManager().getSelected() + "\n"
                + "共导出数量: " + count + "\n\n";

        String fileName = exportAllItems ? "all_item_tooltips.txt" : "weapon_tooltips.txt";
        Path outputPath = mc.gameDirectory.toPath().resolve(fileName);

        try {
            Files.writeString(outputPath, header + sb, StandardCharsets.UTF_8);
            int finalCount = count;
            mc.player.displayClientMessage(
                    Component.literal("已导出 " + finalCount + " 个物品的 Tooltip 到: " + outputPath.toAbsolutePath()),
                    false
            );
        } catch (IOException e) {
            mc.player.displayClientMessage(
                    Component.literal("导出失败: " + e.getMessage()),
                    false
            );
        }
    }

    /**
     * 最大程度还原游戏内鼠标悬停看到的文本行：
     * 1. 调用 ItemStack#getTooltipLines（包含名称、属性、附魔、模组描述、ItemTooltipEvent）
     * 2. 触发 RenderTooltipEvent.GatherComponents（捕获渲染阶段动态插入文本的模组）
     */
    private static List<String> extractRenderedTooltipLines(
            Minecraft mc,
            ItemStack stack,
            Item.TooltipContext context,
            TooltipFlag flag
    ) {
        List<Component> rawComponents = stack.getTooltipLines(context, mc.player, flag);

        List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>();
        for (Component line : rawComponents) {
            elements.add(Either.left(line));
        }
        stack.getTooltipImage().ifPresent(img -> {
            if (!elements.isEmpty()) {
                elements.add(1, Either.right(img));
            } else {
                elements.add(Either.right(img));
            }
        });

        try {
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            RenderTooltipEvent.GatherComponents gatherEvent = new RenderTooltipEvent.GatherComponents(
                    stack, screenWidth, screenHeight, elements, -1
            );
            NeoForge.EVENT_BUS.post(gatherEvent);

            if (!gatherEvent.isCanceled()) {
                List<String> result = new ArrayList<>();
                for (Either<FormattedText, TooltipComponent> element : elements) {
                    element.left().ifPresent(ft -> result.add(stripFormatting(ft.getString())));
                }
                return result;
            }
        } catch (Throwable ignored) {
            // 如果个别模组在无 Screen 环境下处理 GatherComponents 报错，回退到基础文本列表
        }

        List<String> fallback = new ArrayList<>(rawComponents.size());
        for (Component line : rawComponents) {
            fallback.add(stripFormatting(line.getString()));
        }
        return fallback;
    }

    private static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();

        // 1. 常见武器基类
        if (item instanceof SwordItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof MaceItem
                || item instanceof ProjectileWeaponItem
                || item instanceof AxeItem) {
            return true;
        }

        // 2. 原版与 NeoForge 武器标签
        if (stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.WEAPON_ENCHANTABLE)
                || stack.is(ItemTags.SHARP_WEAPON_ENCHANTABLE)
                || stack.is(ItemTags.SWORD_ENCHANTABLE)
                || stack.is(ItemTags.BOW_ENCHANTABLE)
                || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)
                || stack.is(ItemTags.TRIDENT_ENCHANTABLE)
                || stack.is(ItemTags.MACE_ENCHANTABLE)
                || stack.is(Tags.Items.TOOLS_BOW)
                || stack.is(Tags.Items.TOOLS_CROSSBOW)
                || stack.is(Tags.Items.TOOLS_SPEAR)
                || stack.is(C_MELEE_WEAPON)
                || stack.is(C_RANGED_WEAPON)
                || stack.is(C_WEAPONS)) {
            return true;
        }

        // 3. 过滤常规挖掘工具（镐/铲/锄），保留有正攻击伤害加成的模组自定义武器（太刀、战锤、长矛、镰刀等）
        boolean isDiggingTool = item instanceof PickaxeItem
                || item instanceof ShovelItem
                || item instanceof HoeItem
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES);

        if (!isDiggingTool) {
            ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
            for (ItemAttributeModifiers.Entry modEntry : modifiers.modifiers()) {
                if (modEntry.attribute().is(Attributes.ATTACK_DAMAGE) && modEntry.modifier().amount() > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String stripFormatting(String text) {
        String stripped = ChatFormatting.stripFormatting(text);
        return stripped != null ? stripped : "";
    }
}