package com.example.examplemod.export.modules;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.export.core.ExportIO;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class WeaponTooltipExporter {

    private static final TagKey<Item> C_MELEE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/melee_weapon"));
    private static final TagKey<Item> C_RANGED = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/ranged_weapon"));
    private static final TagKey<Item> C_WEAPONS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "weapons"));

    private static int delayTicks = -1;
    private static boolean pendingAll = false;

    private WeaponTooltipExporter() {
    }

    public static void scheduleDelay(boolean exportAll, int ticks) {
        pendingAll = exportAll;
        delayTicks = ticks;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (delayTicks > 0) {
            delayTicks--;
        } else if (delayTicks == 0) {
            delayTicks = -1;
            exportNow(pendingAll);
        }
    }

    public static void exportNow(boolean exportAllItems) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Item.TooltipContext ctx = Item.TooltipContext.of(mc.level);
        TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;

        List<Map.Entry<ResourceKey<Item>, Item>> entries = BuiltInRegistries.ITEM.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .toList();

        StringBuilder body = new StringBuilder();
        int count = 0;

        for (var entry : entries) {
            ItemStack stack = new ItemStack(entry.getValue());
            if (stack.isEmpty() || (!exportAllItems && !isWeapon(stack))) continue;

            count++;
            String name = ExportIO.stripColor(stack.getHoverName().getString());
            body.append("==================================================\n")
                    .append("[").append(count).append("] ").append(name)
                    .append(" (").append(entry.getKey().location()).append(")\n")
                    .append("==================================================\n");

            try {
                for (String line : extractLines(mc, stack, ctx, flag)) {
                    body.append(line).append('\n');
                }
            } catch (Throwable t) {
                body.append("[读取 Tooltip 出错: ").append(t.getClass().getSimpleName())
                        .append(" - ").append(t.getMessage()).append("]\n");
            }
            body.append('\n');
        }

        String header = (exportAllItems ? "全部物品" : "武器") + " Tooltip 导出\n"
                + "当前语言: " + mc.getLanguageManager().getSelected() + "\n"
                + "导出数量: " + count + "\n\n";

        Path out = ExportIO.resolve(exportAllItems ? "all_item_tooltips.txt" : "weapon_tooltips.txt");
        try {
            ExportIO.writeText(out, header + body);
            mc.player.displayClientMessage(Component.literal("§a已导出 " + count + " 个物品到: §f" + out), false);
        } catch (Exception e) {
            mc.player.displayClientMessage(Component.literal("§c导出失败: " + e.getMessage()), false);
        }
    }

    private static List<String> extractLines(Minecraft mc, ItemStack stack, Item.TooltipContext ctx, TooltipFlag flag) {
        List<Component> raw = stack.getTooltipLines(ctx, mc.player, flag);
        List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>();
        for (Component c : raw) elements.add(Either.left(c));
        stack.getTooltipImage().ifPresent(img -> elements.add(elements.isEmpty() ? 0 : 1, Either.right(img)));

        try {
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            RenderTooltipEvent.GatherComponents event = new RenderTooltipEvent.GatherComponents(stack, w, h, elements, -1);
            NeoForge.EVENT_BUS.post(event);
            if (!event.isCanceled()) {
                List<String> lines = new ArrayList<>();
                for (var el : elements) {
                    el.left().ifPresent(ft -> lines.add(ExportIO.stripColor(ft.getString())));
                }
                return lines;
            }
        } catch (Throwable ignored) {
        }

        return raw.stream().map(c -> ExportIO.stripColor(c.getString())).toList();
    }

    private static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem
                || item instanceof TridentItem || item instanceof MaceItem
                || item instanceof ProjectileWeaponItem || item instanceof AxeItem) {
            return true;
        }
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.WEAPON_ENCHANTABLE)
                || stack.is(ItemTags.SHARP_WEAPON_ENCHANTABLE) || stack.is(ItemTags.SWORD_ENCHANTABLE)
                || stack.is(ItemTags.BOW_ENCHANTABLE) || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)
                || stack.is(ItemTags.TRIDENT_ENCHANTABLE) || stack.is(ItemTags.MACE_ENCHANTABLE)
                || stack.is(Tags.Items.TOOLS_BOW) || stack.is(Tags.Items.TOOLS_CROSSBOW)
                || stack.is(Tags.Items.TOOLS_SPEAR)
                || stack.is(C_MELEE) || stack.is(C_RANGED) || stack.is(C_WEAPONS)) {
            return true;
        }
        boolean isDigging = item instanceof PickaxeItem || item instanceof ShovelItem || item instanceof HoeItem
                || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
        if (!isDigging) {
            ItemAttributeModifiers mods = stack.getAttributeModifiers();
            for (var entry : mods.modifiers()) {
                if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.modifier().amount() > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}