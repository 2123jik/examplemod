package com.example.examplemod.client.tooltip;

import com.example.examplemod.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

public final class TooltipHandler {
    private static boolean registered;

    private TooltipHandler() {}

    public static synchronized void register() {
        if (registered) return;
        NeoForge.EVENT_BUS.register(TooltipHandler.class);
        registered = true;
    }

    @SubscribeEvent
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        if (TooltipInteraction.isRenderingPinned()) return;

        ItemStack stack = event.getItemStack();
        if (!Config.ENABLED.get() || stack.isEmpty()
                || stack.has(DataComponents.HIDE_TOOLTIP)) return;

        TooltipContext context = TooltipContext.from(event);
        List<TooltipContribution> contributions = new ArrayList<>(16);

        if (!stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)) {
            for (var provider : BuiltinTooltipProviders.ALL) {
                provider.appendContributions(context, contributions);
            }
        }

        if (Config.SHOW_CUSTOM_RULES.get()) {
            TooltipRules.add(stack, contributions);
        }

        TooltipContributors.contribute(context, contributions);
        event.getTooltipElements().addAll(TooltipUtil.buildElements(contributions));

        List<Component> lines = new ArrayList<>(event.getTooltipElements().size());
        for (var element : event.getTooltipElements()) {
            element.ifLeft(text -> lines.add(
                    text instanceof Component component
                            ? component.copy()
                            : Component.literal(text.getString())
            ));
        }
        TooltipInteraction.capture(stack, lines);
    }
}
