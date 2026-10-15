package com.nf121.examplemod.client;

import com.nf121.examplemod.ExampleMod;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class ContainerScreenOverlay {

    private static final int OVERLAY_Z = 300;

    @SubscribeEvent
    public static void onRenderGuiForeground(ContainerScreenEvent.Render.Foreground event) {
        var guiGraphics = event.getGuiGraphics();
        var screen = event.getContainerScreen();
        Font font = Minecraft.getInstance().font;

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || !slot.hasItem()) continue;
            // 调用抽离的统一渲染方法
            renderItemOverlay(guiGraphics, font, slot.getItem(), slot.x, slot.y);
        }
    }
    /**
     * 公共渲染方法：在任意给定的 16x16 槽位坐标 (x, y) 处绘制角标
     * 可直接被 EquipmentItemIcon 调用
     */
    public static void renderItemOverlay(GuiGraphics guiGraphics, Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;

        // --- 1. 食物属性渲染 (左上角: 饥饿值 / 右上角: 饱和度) ---
        FoodProperties foodProps = stack.get(DataComponents.FOOD);
        if (foodProps != null) {
            float scale = 0.5f;

            int nutrition = foodProps.nutrition();
            String hungerText = String.valueOf(nutrition);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + 1, y + 1, OVERLAY_Z);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, hungerText, 0, 0, 0xFFEAA154, true);
            guiGraphics.pose().popPose();

            int saturation = (int) foodProps.saturation();
            String satText = String.valueOf(saturation);

            float satWidth = font.width(satText) * scale;
            float satX = x + 16 - satWidth - 1;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(satX, y + 1, OVERLAY_Z);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, satText, 0, 0, 0xFFE3BB22, true);
            guiGraphics.pose().popPose();
            return;
        }

        // --- 2. 附魔书缩写流式折行 ---
        ItemEnchantments storedEnchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchants != null && !storedEnchants.isEmpty()) {
            float enchantScale = 0.5f;
            int maxFontWidth = (int) (16.0f / enchantScale);

            StringBuilder sb = new StringBuilder();
            for (var entry : storedEnchants.entrySet()) {
                String name = entry.getKey().value().description().getString().replace(" ", "");
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(name);
            }

            var lines = font.split(Component.literal(sb.toString()), maxFontWidth);
            float localLineStep = 7.0f;
            float maxLocalY = 16.0f / enchantScale;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, OVERLAY_Z);
            guiGraphics.pose().scale(enchantScale, enchantScale, 1.0f);

            float localY = 0.0f;
            for (var line : lines) {
                if (localY + 7.0f > maxLocalY) break;
                guiGraphics.drawString(font, line, 0, (int) localY, 0xFF55FFFF, false);
                localY += localLineStep;
            }

            guiGraphics.pose().popPose();
            return;
        }

        // --- 3. 神化稀有度品质角标 ---
        var rarityHolder = stack.get(Apoth.Components.RARITY);
        if (rarityHolder != null && rarityHolder.isBound()) {
            renderPip(guiGraphics, x + 1, y + 1, rarityHolder.get().color().getValue() | 0xFF000000);
        }

        // --- 4. 铁魔法法术卷轴品质 ---
        if (stack.getItem() instanceof Scroll) {
            Integer scrollColor = getScrollRarityColor(stack);
            if (scrollColor != null) renderPip(guiGraphics, x + 1, y + 1, scrollColor);
        }

        // --- 5. 宝石纯度角标 ---
        Purity purity = stack.get(Apoth.Components.PURITY);
        if (purity != null && purity.getColor() != null) {
            int size = 3;
            renderPip(guiGraphics, x + 16 - size - 1, y + 1, purity.getColor().getValue() | 0xFF000000);
        }
    }
    private static Integer getScrollRarityColor(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.getUnsafe();
            if (tag.getBoolean("is_unidentified") && tag.contains("spell_rarity")) {
                try {
                    SpellRarity rarity = SpellRarity.valueOf(tag.getString("spell_rarity").toUpperCase(Locale.ROOT));
                    Integer color = rarity.getChatFormatting().getColor();
                    return color != null ? (color | 0xFF000000) : null;
                } catch (Exception ignored) {}
            }
        }

        if (ISpellContainer.isSpellContainer(stack)) {
            ISpellContainer container = ISpellContainer.get(stack);
            if (container != null && !container.isEmpty()) {
                SpellData spellData = container.getSpellAtIndex(0);
                if (spellData != null && spellData != SpellData.EMPTY) {
                    Integer color = spellData.getRarity().getChatFormatting().getColor();
                    return color != null ? (color | 0xFF000000) : null;
                }
            }
        }
        return null;
    }

    private static void renderPip(GuiGraphics guiGraphics, int x, int y, int argbColor) {
        int size = 3;
        guiGraphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, OVERLAY_Z - 1, 0x60000000);
        guiGraphics.fill(x, y, x + size, y + size, OVERLAY_Z, argbColor);
    }
}
