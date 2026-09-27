package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ChestScreenOverlay {

    private static final int OVERLAY_Z = 300;

    @SubscribeEvent
    public static void onRenderGuiForeground(ContainerScreenEvent.Render.Foreground event) {
        var guiGraphics = event.getGuiGraphics();
        var screen = event.getContainerScreen();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || !slot.hasItem()) continue;

            ItemStack stack = slot.getItem();
            int x = slot.x;
            int y = slot.y;

            var rarityHolder = stack.get(Apoth.Components.RARITY);
            if (rarityHolder != null && rarityHolder.isBound()) {
                renderPip(guiGraphics, x + 1, y + 1,
                        rarityHolder.get().color().getValue() | 0xFF000000);
            }

            if (stack.getItem() instanceof Scroll) {
                Integer scrollColor = getScrollRarityColor(stack);
                if (scrollColor != null) renderPip(guiGraphics, x + 1, y + 1, scrollColor);
            }

            Purity purity = stack.get(Apoth.Components.PURITY);
            if (purity != null && purity.getColor() != null) {
                int size = 3;
                renderPip(guiGraphics, slot.x + 16 - size - 1, y + 1,
                        purity.getColor().getValue() | 0xFF000000);
            }
        }
    }

    private static Integer getScrollRarityColor(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.getUnsafe();
            if (tag.getBoolean("is_unidentified") && tag.contains("spell_rarity")) {
                try {
                    SpellRarity rarity = SpellRarity.valueOf(
                            tag.getString("spell_rarity").toUpperCase(Locale.ROOT));
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