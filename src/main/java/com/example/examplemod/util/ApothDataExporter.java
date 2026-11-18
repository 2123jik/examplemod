package com.example.examplemod.util;

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
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@EventBusSubscriber(modid = "examplemod")
public class ApothDataExporter {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("apoth_dump")
                        .requires(source -> source.hasPermission(0))
                        .executes(ctx -> {
                            try {
                                exportSimulatedTooltips();
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[模拟 Tooltip 导出成功！]§f 文件存放在 .minecraft/apoth_dump/00_全槽位可视化对照大表.md"), false);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("§c导出失败: " + e.getMessage()));
                                e.printStackTrace();
                            }
                            return 1;
                        })
        );
    }

    public static void exportSimulatedTooltips() throws Exception {
        File dir = FMLPaths.GAMEDIR.get().resolve("apoth_dump").toFile();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, "00_全槽位可视化对照大表.md");

        AttributeTooltipContext tooltipCtx = AttributeTooltipContext.of(
                null,
                TooltipContext.EMPTY,
                net.minecraft.world.item.TooltipFlag.NORMAL
        );

        DynamicHolder<LootRarity> mythicHolder = RarityRegistry.INSTANCE.holder(
                ResourceLocation.fromNamespaceAndPath("apotheosis", "mythic")
        );
        LootRarity mythicRarity = mythicHolder.isBound() ? mythicHolder.get() : null;

        Collection<Gem> allGems = GemRegistry.INSTANCE.getValues();
        Collection<Affix> allAffixes = AffixRegistry.INSTANCE.getValues();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            writer.write("# 神化全部位装备 Tooltip 真实渲染大表\n\n");
            writer.write("> 标注带有 `👑[唯一]` 的宝石：在同一件装备上**严禁重复镶嵌**（只能插1颗）；未标注的为普通宝石，可在一个装备的多孔位中**自由堆叠**。\n\n");

            for (LootCategory cat : Apoth.BuiltInRegs.LOOT_CATEGORY) {
                if (cat.isNone()) continue;

                writer.write(String.format("## 📌 部位分类: `%s`\n\n", cat.getKey()));

                writer.write("### ⚔️ 该部位可洗出的词缀 (Affixes):\n\n");
                writer.write("| 词缀 ID | 类型 | 模拟 Tooltip 描述 (神话阶满值) |\n");
                writer.write("| :--- | :--- | :--- |\n");

                int affixCount = 0;
                for (Affix affix : allAffixes) {
                    if (mythicRarity != null && affix.canApplyTo(ItemStack.EMPTY, cat, mythicRarity)) {
                        affixCount++;
                        AffixInstance mockInst = new AffixInstance(
                                AffixRegistry.INSTANCE.holder(affix),
                                1.0F,
                                mythicHolder,
                                ItemStack.EMPTY
                        );

                        String text;
                        try {
                            Component comp = affix.getAugmentingText(mockInst, tooltipCtx);
                            text = comp.getString();
                        } catch (Exception e) {
                            text = affix.getDescription(mockInst, tooltipCtx).getString();
                        }

                        writer.write(String.format("| `%s` | %s | **%s** |\n", affix.id(), affix.definition().type(), cleanString(text)));
                    }
                }
                if (affixCount == 0) {
                    writer.write("| *(无)* | - | 当前部位没有配置任何专属词缀 |\n");
                }
                writer.write("\n");

                writer.write("### 💎 该部位可镶嵌的宝石 (Gems):\n\n");
                writer.write("| 宝石 ID (含唯一性) | 最低纯度效果 | 完美纯度效果 (已受 1/4 Mixin 约束) |\n");
                writer.write("| :--- | :--- | :--- |\n");

                int gemCount = 0;
                for (Gem gem : allGems) {
                    DynamicHolder<Gem> gemHolder = GemRegistry.INSTANCE.holder(gem);

                    GemInstance minInst = new GemInstance(gemHolder, cat, gem.getMinPurity(), ItemStack.EMPTY, 0);
                    GemInstance perfectInst = new GemInstance(gemHolder, cat, Purity.PERFECT, ItemStack.EMPTY, 0);

                    if (perfectInst.getBonus().isPresent()) {
                        gemCount++;
                        String minText = minInst.getSocketBonusTooltip(tooltipCtx).getString();
                        String perfectText = perfectInst.getSocketBonusTooltip(tooltipCtx).getString();

                        String uniqueTag = gem.isUnique() ? " 👑`[唯一]`" : "";
                        String gemIdWithTag = String.format("`%s`%s", gem.getId(), uniqueTag);

                        writer.write(String.format("| %s | %s | **%s** |\n",
                                gemIdWithTag,
                                cleanString(minText),
                                cleanString(perfectText)
                        ));
                    }
                }
                if (gemCount == 0) {
                    writer.write("| *(无)* | - | 当前部位没有可用的宝石 |\n");
                }

                writer.write("\n---\n\n");
            }
        }
    }

    private static String cleanString(String input) {
        if (input == null) return "";
        return input.replace("\n", " ").replace("\r", " ").replace("|", "\\|");
    }
}