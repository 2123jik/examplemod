package com.example.examplemod.client.tooltip;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

// 1. Tooltip 优先级分类
public enum TooltipCategory {
    IDENTITY(0), DURABILITY(10), ATTRIBUTES(20), TOOL(25), ENCHANTMENTS(30),
    EFFECTS(40), FOOD(50), FUEL(60), COMPOST(65), COOLDOWN(70), TAGS(80),
    COMPONENTS(90), CONTAINER_PREVIEW(100), CUSTOM(110), DEBUG(120);

    private final int order;
    TooltipCategory(int order) { this.order = order; }
    public int order() { return this.order; }
}

// 2. 上下文上下文信息封装
record TooltipContext(
        ItemStack stack,
        Player player,
        Item.TooltipContext vanillaContext,
        TooltipFlag flags,
        boolean detailMode,
        Screen screen
) {
    public static TooltipContext from(ItemTooltipEvent event) {
        return new TooltipContext(
                event.getItemStack(), event.getEntity(), event.getContext(),
                event.getFlags(), Screen.hasShiftDown(), Minecraft.getInstance().screen
        );
    }

    public static TooltipContext from(RenderTooltipEvent.GatherComponents event) {
        Minecraft mc = Minecraft.getInstance();
        var vanillaCtx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        var flags = mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        return new TooltipContext(event.getItemStack(), mc.player, vanillaCtx, flags, Screen.hasShiftDown(), mc.screen);
    }
}

// 3. Tooltip 元素条目
record TooltipContribution(
        TooltipCategory category,
        int priority,
        String key,
        Either<Component, TooltipComponent> content,
        BooleanSupplier visible
) {
    public TooltipContribution {
        Objects.requireNonNull(category); Objects.requireNonNull(key);
        Objects.requireNonNull(content); Objects.requireNonNull(visible);
    }

    public static TooltipContribution text(TooltipCategory cat, int prio, String key, Component text) {
        return new TooltipContribution(cat, prio, key, Either.left(Objects.requireNonNull(text)), () -> true);
    }

    public static TooltipContribution visual(TooltipCategory cat, int prio, String key, TooltipComponent visual) {
        return new TooltipContribution(cat, prio, key, Either.right(Objects.requireNonNull(visual)), () -> true);
    }

    public boolean isVisible() { return this.visible.getAsBoolean(); }
}

// 4. 扩展贡献者管理器
final class TooltipContributors {
    private static final CopyOnWriteArrayList<Function<TooltipContext, Collection<TooltipContribution>>> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private TooltipContributors() {}

    @FunctionalInterface
    public interface Provider {
        void appendContributions(TooltipContext context, List<TooltipContribution> sink);
    }

    public static void register(Function<TooltipContext, Collection<TooltipContribution>> contributor) {
        CONTRIBUTORS.addIfAbsent(Objects.requireNonNull(contributor));
    }

    static void contribute(TooltipContext context, List<TooltipContribution> sink) {
        for (var contributor : CONTRIBUTORS) {
            Collection<TooltipContribution> additions = contributor.apply(context);
            if (additions != null) sink.addAll(additions);
        }
    }
}