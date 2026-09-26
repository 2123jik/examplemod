package com.example.examplemod.tooltip;

import com.example.examplemod.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

public final class TooltipInteraction {
    private static ItemStack currentStack = ItemStack.EMPTY;
    private static List<Component> currentLines = List.of();

    private static ItemStack pinnedStack = ItemStack.EMPTY;
    private static List<Component> pinnedLines = List.of();
    private static int pageOffset;
    private static boolean renderingPinned;

    private TooltipInteraction() {}

    static boolean isRenderingPinned() {
        return renderingPinned;
    }

    public static void capture(ItemStack stack, List<Component> lines) {
        if (renderingPinned) return;

        if (stack.isEmpty() || lines.isEmpty()) {
            clearCurrent();
            return;
        }

        currentStack = stack.copy();
        currentLines = copyLines(lines);
    }

    public static void reset() {
        clearCurrent();
        clearPinned();
        renderingPinned = false;
    }

    public static void togglePinned() {
        if (!Config.ENABLED.get()) return;

        if (currentStack.isEmpty()) {
            clearPinned();
            return;
        }

        if (ItemStack.matches(currentStack, pinnedStack)) {
            clearPinned();
            return;
        }

        pinnedStack = currentStack.copy();
        pinnedLines = copyLines(currentLines);
        pageOffset = 0;
    }

    @SubscribeEvent
    public static void onFrameStart(RenderGuiEvent.Pre event) {
        clearCurrent();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!canRenderPinned() || event.getScrollDeltaY() == 0.0) return;

        int maximum = maxPageOffset();
        if (maximum == 0) return;

        int direction = event.getScrollDeltaY() < 0.0 ? 1 : -1;
        pageOffset = Math.clamp(pageOffset, 0, maximum);
        pageOffset = Math.clamp(pageOffset + direction, 0, maximum);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderPinned(RenderGuiEvent.Post event) {
        if (!canRenderPinned()) return;

        int maximum = maxPageOffset();
        pageOffset = Math.clamp(pageOffset, 0, maximum);

        int pageSize = pageSize();
        int from = pageOffset * pageSize;
        int to = from + Math.min(pageSize, pinnedLines.size() - from);

        List<Component> page = new ArrayList<>(to - from + 1);
        page.add(Component.translatable(
                "examplemod.tooltip.pinned", pageOffset + 1, maximum + 1
        ));
        page.addAll(pinnedLines.subList(from, to));

        Minecraft mc = Minecraft.getInstance();
        renderingPinned = true;
        try {
            event.getGuiGraphics().renderTooltip(
                    mc.font,
                    page,
                    pinnedStack.getTooltipImage(),
                    mc.getWindow().getGuiScaledWidth() - 8,
                    8
            );
        } finally {
            renderingPinned = false;
        }
    }

    private static boolean canRenderPinned() {
        Minecraft mc = Minecraft.getInstance();
        return Config.ENABLED.get()
                && mc.level != null
                && mc.screen == null
                && !pinnedStack.isEmpty()
                && !pinnedLines.isEmpty();
    }

    private static int pageSize() {
        return Math.max(1, Config.PINNED_PAGE_SIZE.get());
    }

    private static int maxPageOffset() {
        return Math.max(0, (pinnedLines.size() - 1) / pageSize());
    }

    private static List<Component> copyLines(List<Component> lines) {
        return lines.stream().<Component>map(Component::copy).toList();
    }

    private static void clearCurrent() {
        currentStack = ItemStack.EMPTY;
        currentLines = List.of();
    }

    private static void clearPinned() {
        pinnedStack = ItemStack.EMPTY;
        pinnedLines = List.of();
        pageOffset = 0;
    }
}
