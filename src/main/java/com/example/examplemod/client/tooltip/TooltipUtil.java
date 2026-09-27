package com.example.examplemod.client.tooltip;

import com.example.examplemod.Config;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class TooltipUtil {
    private static final Comparator<TooltipContribution> ORDER = Comparator
            .comparingInt((TooltipContribution c) -> c.category().order())
            .thenComparingInt(TooltipContribution::priority)
            .thenComparing(TooltipContribution::key);

    private TooltipUtil() {}

    public static List<Either<FormattedText, TooltipComponent>> buildElements(
            List<TooltipContribution> contributions
    ) {
        int limit = Math.max(0, Config.MAX_LINES.get());
        if (limit == 0 || contributions.isEmpty()) return List.of();

        List<TooltipContribution> visible = new ArrayList<>(contributions.size());
        for (TooltipContribution contribution : contributions) {
            if (contribution.isVisible()) visible.add(contribution);
        }
        visible.sort(ORDER);

        List<Either<FormattedText, TooltipComponent>> result =
                new ArrayList<>(Math.min(limit, visible.size()));
        Set<String> seen = new HashSet<>();

        for (TooltipContribution contribution : visible) {
            if (!seen.add(contribution.key())) continue;

            result.add(contribution.content().mapBoth(
                    text -> (FormattedText) text,
                    visual -> visual
            ));
            if (result.size() == limit) break;
        }
        return result;
    }

    public static float tickRate(TooltipContext context) {
        float rate = context.vanillaContext() == null
                ? 20.0F : context.vanillaContext().tickRate();
        return Float.isFinite(rate) && rate > 0.0F ? rate : 20.0F;
    }

    public static float unit(float value) {
        return Float.isFinite(value) ? Mth.clamp(value, 0.0F, 1.0F) : 0.0F;
    }

    public static float ratio(int value, int maximum) {
        return maximum > 0 ? unit((float) value / maximum) : 0.0F;
    }

    public static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        Objects.requireNonNull(stacks, "stacks");
        return stacks.stream().map(ItemStack::copy).toList();
    }

    public static int lerpColor(int from, int to, float factor) {
        float t = unit(factor);
        int r = Math.round(Mth.lerp(t, from >> 16 & 255, to >> 16 & 255));
        int g = Math.round(Mth.lerp(t, from >> 8 & 255, to >> 8 & 255));
        int b = Math.round(Mth.lerp(t, from & 255, to & 255));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
