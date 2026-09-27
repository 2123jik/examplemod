package com.example.examplemod.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public record TooltipContext(
        ItemStack stack,
        Player player,
        Item.TooltipContext vanillaContext,
        TooltipFlag flags,
        boolean detailMode,
        Screen screen
) {
    public static TooltipContext from(ItemTooltipEvent event) {
        return new TooltipContext(
                event.getItemStack(),
                event.getEntity(),
                event.getContext(),
                event.getFlags(),
                Screen.hasShiftDown(),
                Minecraft.getInstance().screen
        );
    }

    public static TooltipContext from(RenderTooltipEvent.GatherComponents event) {
        Minecraft mc = Minecraft.getInstance();
        Item.TooltipContext vanillaCtx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        TooltipFlag flags = mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;

        return new TooltipContext(
                event.getItemStack(),
                mc.player,
                vanillaCtx,
                flags,
                Screen.hasShiftDown(),
                mc.screen
        );
    }
}