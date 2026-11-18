package com.example.examplemod.runeword;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class RunewordEffectTooltipHandler {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)");

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (stack.isEmpty() || tooltip.isEmpty()) return;

        String activeRunewordId = null;
        double purityScale = 1.0;
        ModDataComponents.ActiveRuneword active = stack.get(ModDataComponents.ACTIVE_RUNEWORD.get());
        if (active != null && RunewordPurityHelper.isRunewordIntact(stack, active)) {
            activeRunewordId = active.id();
            purityScale = RunewordPurityHelper.getScale(active.purity());
        }

        boolean isGemItem = stack.getItem() instanceof GemItem;
        Map<EffectKind, Double> foundEffects = new LinkedHashMap<>();

        ListIterator<Component> it = tooltip.listIterator();
        while (it.hasNext()) {
            Component line = it.next();
            String text = line.getString();

            // 1. 法力缩放类（保留在 Stat 栏，标出铁魔法实际等效值）
            Component scaledManaLine = tryFormatScaledManaStat(text);
            if (scaledManaLine != null) {
                it.set(scaledManaLine);
                continue;
            }

            // 2. 机制/光环/触发类（本质是 Effect）
            EffectKind kind = matchEffect(text);
            if (kind != null) {
                double val = extractNumber(text);
                if (isGemItem) {
                    it.set(Component.literal(" ").append(kind.format(val, null)));
                } else {
                    foundEffects.merge(kind, val, kind.isBoolean ? Math::max : Double::sum);
                    it.remove();
                }
            }
        }

        if (isGemItem) return;

        List<RunewordSpellProcs.SpellProc> spellProcs = RunewordSpellProcs.getProcs(activeRunewordId);
        if (foundEffects.isEmpty() && spellProcs.isEmpty()) return;

        cleanupOrphanModifierHeaders(tooltip);

        int insertAt = tooltip.size();
        for (int i = 0; i < tooltip.size(); i++) {
            if (isSlotModifierHeader(tooltip.get(i))) {
                insertAt = i;
                break;
            }
        }

        List<Component> effectLines = new ArrayList<>();
        for (Map.Entry<EffectKind, Double> entry : foundEffects.entrySet()) {
            effectLines.add(entry.getKey().format(entry.getValue(), activeRunewordId));
        }

        // 加入铁魔法法术触发词条
        for (RunewordSpellProcs.SpellProc proc : spellProcs) {
            int scaledLv = Math.max(1, (int) Math.round(proc.baseLevel() * purityScale));
            String spellKey = "spell." + proc.spellId().getNamespace() + "." + proc.spellId().getPath();
            effectLines.add(
                    Component.literal("◆ ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(proc.trigger().label + "有 ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(fmt(proc.chancePercent()) + "%").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" 几率施展 ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("等级 " + scaledLv + " ").withStyle(ChatFormatting.AQUA))
                            .append(Component.translatable(spellKey).withStyle(ChatFormatting.LIGHT_PURPLE))
            );
        }

        if (insertAt > 0 && !tooltip.get(insertAt - 1).getString().isBlank()) {
            effectLines.add(0, Component.empty());
        }
        if (insertAt < tooltip.size() && !tooltip.get(insertAt).getString().isBlank()) {
            effectLines.add(Component.empty());
        }

        tooltip.addAll(insertAt, effectLines);
    }

    private static Component tryFormatScaledManaStat(String text) {
        if (text.contains("击杀回复法力")) {
            double raw = extractNumber(text);
            return Component.literal(" +" + fmt(raw * 4.0) + " 击杀回复法力").withStyle(ChatFormatting.BLUE);
        }
        if (text.contains("受损的生命转换成法力")) {
            double raw = extractNumber(text);
            return Component.literal(" " + fmt(raw) + "% 受损的生命转换成法力 ")
                    .withStyle(ChatFormatting.BLUE)
                    .append(Component.literal("(铁魔法等效 " + fmt(raw * 3.5) + "%)").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (text.contains("击中时偷取法力")) {
            double raw = extractNumber(text);
            return Component.literal(" " + fmt(raw) + "% 击中时偷取法力 ")
                    .withStyle(ChatFormatting.BLUE)
                    .append(Component.literal("(铁魔法等效 " + fmt(raw * 4.0) + "%)").withStyle(ChatFormatting.DARK_GRAY));
        }
        return null;
    }

    private static EffectKind matchEffect(String text) {
        for (EffectKind kind : EffectKind.values()) {
            if (text.contains(kind.matchKey)) {
                return kind;
            }
        }
        return null;
    }

    private static double extractNumber(String text) {
        Matcher m = NUMBER_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1.0;
    }

    private static boolean isSlotModifierHeader(Component line) {
        if (line.getContents() instanceof TranslatableContents tc) {
            return tc.getKey().startsWith("item.modifiers.");
        }
        return false;
    }

    private static void cleanupOrphanModifierHeaders(List<Component> tooltip) {
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            if (!isSlotModifierHeader(tooltip.get(i))) continue;

            boolean hasFollowingStat = false;
            if (i + 1 < tooltip.size()) {
                String nextStr = tooltip.get(i + 1).getString();
                if (!nextStr.isBlank() && !isSlotModifierHeader(tooltip.get(i + 1))) {
                    hasFollowingStat = nextStr.startsWith(" ") || nextStr.startsWith("+") || nextStr.startsWith("-");
                }
            }

            if (!hasFollowingStat) {
                tooltip.remove(i);
                if (i - 1 >= 0 && tooltip.get(i - 1).getString().isBlank()) {
                    tooltip.remove(i - 1);
                    i--;
                }
            }
        }
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-4) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private enum EffectKind {
        HOLY_FIRE_AURA("圣火燃烧灵气", true) {
            @Override
            Component format(double val, String rwId) {
                String lv = ("dragon_armor".equals(rwId) || "dragon_shield".equals(rwId) || "hand_of_justice".equals(rwId))
                        ? "等级 12 圣火灵气" : "圣火灵气";
                return bullet("装备时激活 ", ChatFormatting.GRAY)
                        .append(Component.literal(lv).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" (周期性灼烧周围敌人)").withStyle(ChatFormatting.DARK_GRAY));
            }
        },
        HOLY_FREEZE_AURA("神圣冰冻灵气", true) {
            @Override
            Component format(double val, String rwId) {
                String lv = "ice".equals(rwId) ? "等级 18 神圣冰冻灵气"
                        : ("doom".equals(rwId) || "shattered_wall".equals(rwId)) ? "等级 16 神圣冰冻灵气"
                        : "神圣冰冻灵气";
                return bullet("装备时激活 ", ChatFormatting.GRAY)
                        .append(Component.literal(lv).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" (冰冻并减速周围敌人)").withStyle(ChatFormatting.DARK_GRAY));
            }
        },
        HOLY_SHOCK_AURA("神圣冲击灵气", true) {
            @Override
            Component format(double val, String rwId) {
                String lv = ("dream_helm".equals(rwId) || "dream_shield".equals(rwId))
                        ? "等级 14 神圣冲击灵气" : "神圣冲击灵气";
                return bullet("装备时激活 ", ChatFormatting.GRAY)
                        .append(Component.literal(lv).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" (周期性雷击周围敌人)").withStyle(ChatFormatting.DARK_GRAY));
            }
        },
        CANNOT_BE_FROZEN("无法冰冻", true) {
            @Override
            Component format(double val, String rwId) {
                return bullet("无法冰冻", ChatFormatting.AQUA)
                        .append(Component.literal(" (免疫冰冻与缓慢)").withStyle(ChatFormatting.DARK_GRAY));
            }
        },
        PREVENT_HEAL("防止目标自疗", true) {
            @Override
            Component format(double val, String rwId) {
                return bullet("攻击时防止目标生命恢复", ChatFormatting.RED)
                        .append(Component.literal(" (持续 10 秒)").withStyle(ChatFormatting.DARK_GRAY));
            }
        },
        OPEN_WOUNDS("撕开伤口流血", false) {
            @Override
            Component format(double val, String rwId) {
                return bullet("攻击有 ", ChatFormatting.GRAY)
                        .append(Component.literal(fmt(val) + "%").withStyle(ChatFormatting.RED))
                        .append(Component.literal(" 几率撕开敌人伤口").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" (造成流血)").withStyle(ChatFormatting.DARK_GRAY));
            }
        },
        HIT_BLIND("击中致盲", false) {
            @Override
            Component format(double val, String rwId) {
                return bullet("攻击有 ", ChatFormatting.GRAY)
                        .append(Component.literal(fmt(val) + "%").withStyle(ChatFormatting.LIGHT_PURPLE))
                        .append(Component.literal(" 几率致盲目标").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" (降低伤害)").withStyle(ChatFormatting.DARK_GRAY));
            }
        },
        FREEZE_TARGET("冻结目标强度", false) {
            @Override
            Component format(double val, String rwId) {
                return bullet("攻击冻结目标", ChatFormatting.BLUE)
                        .append(Component.literal(" (强度 +" + fmt(val) + ")").withStyle(ChatFormatting.DARK_GRAY));
            }
        };

        final String matchKey;
        final boolean isBoolean;

        EffectKind(String matchKey, boolean isBoolean) {
            this.matchKey = matchKey;
            this.isBoolean = isBoolean;
        }

        abstract Component format(double val, String rwId);

        protected static MutableComponent bullet(String text, ChatFormatting color) {
            return Component.literal("◆ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(text).withStyle(color));
        }
    }
}