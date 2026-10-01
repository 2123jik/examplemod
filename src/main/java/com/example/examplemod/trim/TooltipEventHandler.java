package com.example.examplemod.trim;

import com.example.examplemod.ExampleMod;

import com.example.examplemod.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

import static com.example.examplemod.ExampleMod.MODID;

@EventBusSubscriber(modid = MODID)
public class TooltipEventHandler {

    // 使用 LOWEST 优先级，确保神化（Apotheosis）完成前后缀拼装后我们再接管第 0 行
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) return;
        String wordId = stack.get(ModDataComponents.ACTIVE_RUNEWORD.get());
        if (wordId != null && RunewordManager.RUNEWORDS.containsKey(wordId)) {
            RunewordDefinition def = RunewordManager.RUNEWORDS.get(wordId);

            Component currentFullName = tooltip.get(0);

            // 【恢复国际化翻译键】: runeword.<modid>.<id>
            MutableComponent runewordTitle = Component.translatable("runeword." + ExampleMod.MODID + "." + def.id())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            MutableComponent formattedFullName = Component.translatable(
                    "runeword." + ExampleMod.MODID + ".title_format",
                    runewordTitle,
                    currentFullName
            );

            tooltip.set(0, formattedFullName);

            StringBuilder seqBuilder = new StringBuilder();
            for (int i = 0; i < def.sequence().size(); i++) {
                String pattern = def.sequence().get(i).pattern();
                String shortPattern = pattern.contains(":") ? pattern.substring(pattern.indexOf(':') + 1) : pattern;
                seqBuilder.append("§e[").append(shortPattern).append("]");
                if (i < def.sequence().size() - 1) {
                    seqBuilder.append(" §7➔ ");
                }
            }
            return;
        }

        // 2. 如果未激活，展示刻印历史
        List<ModDataComponents.TrimStep> history = stack.get(ModDataComponents.TRIM_HISTORY.get());
        if (history != null && !history.isEmpty()) {
            tooltip.add(Component.empty());

            for (int i = 0; i < history.size(); i++) {
                ModDataComponents.TrimStep step = history.get(i);
                String patternName = simplifyId(step.pattern());
                String matName = simplifyId(step.material());
                tooltip.add(Component.translatable("tooltip." + MODID + ".history_step", (i + 1), patternName, matName)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static String simplifyId(String rawId) {
        if (rawId == null) return "unknown";
        int colonIdx = rawId.indexOf(':');
        return colonIdx >= 0 ? rawId.substring(colonIdx + 1) : rawId;
    }
}