package com.example.examplemod.runeword;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.examplemod.ExampleMod.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class TooltipEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) return;

        // 1. 已激活符文之语的展示
        ModDataComponents.ActiveRuneword active = stack.get(ModDataComponents.ACTIVE_RUNEWORD.get());
        if (active != null && RunewordPurityHelper.isRunewordIntact(stack, active)) {
            RunewordDefinition def = RunewordManager.getAll().get(active.id());
            if (def == null) return;

            ChatFormatting purityColor = RunewordPurityHelper.getPurityColor(active.purity());
            Component purityLabel = Component.translatable("purity.apotheosis." + active.purity());
            Component currentFullName = tooltip.get(0);

            MutableComponent runewordTitle = Component.translatable("runeword." + ExampleMod.MODID + "." + def.id())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            tooltip.set(0, Component.translatable(
                    "runeword." + ExampleMod.MODID + ".title_format",
                    runewordTitle,
                    currentFullName
            ));

            double scale = RunewordPurityHelper.getScale(active.purity());
            int percent = (int) Math.round(scale * 100);
            tooltip.add(1, Component.translatable("tooltip." + MODID + ".runeword_tier", purityLabel, percent)
                    .withStyle(purityColor));

            if (def.allSpellLevels() > 0) {
                int scaledLevels = RunewordPurityHelper.scaleSpellLevels(def.allSpellLevels(), active.purity());
                tooltip.add(2, Component.translatable("tooltip." + MODID + ".all_spell_levels", scaledLevels)
                        .withStyle(ChatFormatting.AQUA));
            }
            return;
        }

        // 2. 未激活的 common 底材：PD2 风格提示匹配孔数且同品质的可用配方
        var rarityHolder = AffixHelper.getRarity(stack);
        if (!rarityHolder.isBound() || !rarityHolder.getId().equals(RunewordPurityHelper.COMMON_RARITY)) {
            return;
        }

        int sockets = SocketHelper.getSockets(stack);
        if (sockets <= 0) return;

        List<RunewordDefinition> matches = RunewordManager.getAll().values().stream()
                .filter(rw -> rw.socketsRequired() == sockets && rw.matchesBaseItem(stack))
                .toList();

        if (!matches.isEmpty()) {
            tooltip.add(Component.translatable("tooltip." + MODID + ".available_runewords", sockets)
                    .withStyle(ChatFormatting.YELLOW));

            // 如果已经插了部分宝石，提示必须保持同品质
            Purity insertedPurity = null;
            boolean mixed = false;
            for (GemInstance inst : SocketHelper.getGems(stack).gems()) {
                if (inst.isValid()) {
                    Purity p = GemItem.getPurity(inst.gemStack());
                    if (insertedPurity == null) insertedPurity = p;
                    else if (insertedPurity != p) mixed = true;
                }
            }
            if (mixed) {
                tooltip.add(Component.translatable("tooltip." + MODID + ".mixed_purity_warning")
                        .withStyle(ChatFormatting.RED));
            }

            // 紧凑展示匹配的配方列表（如: 眼光(8,3,7,12), 无限(30,23,30,24)）
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < matches.size(); i++) {
                RunewordDefinition rw = matches.get(i);
                String name = Component.translatable("runeword." + MODID + "." + rw.id()).getString();
                String nums = rw.gemSequence().stream()
                        .map(TooltipEventHandler::extractRuneNumber)
                        .collect(Collectors.joining(","));

                if (i > 0) line.append(", ");
                line.append(name).append("(").append(nums).append(")");

                if ((i + 1) % 3 == 0 || i == matches.size() - 1) {
                    tooltip.add(Component.literal(line.toString()).withStyle(ChatFormatting.GRAY));
                    line.setLength(0);
                }
            }
        }
    }

    private static String extractRuneNumber(ResourceLocation gemId) {
        // 把 "examplemod:runes/rune_30_ber" 提取出 "30"
        String path = gemId.getPath();
        int idx = path.indexOf("rune_");
        if (idx >= 0 && path.length() >= idx + 7) {
            String num = path.substring(idx + 5, idx + 7);
            return num.startsWith("0") ? num.substring(1) : num;
        }
        return path;
    }
}