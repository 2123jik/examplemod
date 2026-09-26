package com.example.examplemod.tooltip;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class TooltipContributors {
    private static final CopyOnWriteArrayList<
            Function<TooltipContext, Collection<TooltipContribution>>
            > CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private TooltipContributors() {}

    @FunctionalInterface
    public interface Provider {
        void appendContributions(
                TooltipContext context,
                List<TooltipContribution> sink
        );
    }

    public static void register(
            Function<TooltipContext, Collection<TooltipContribution>> contributor
    ) {
        CONTRIBUTORS.addIfAbsent(Objects.requireNonNull(contributor, "contributor"));
    }

    public static boolean unregister(
            Function<TooltipContext, Collection<TooltipContribution>> contributor
    ) {
        return CONTRIBUTORS.remove(contributor);
    }

    static void contribute(TooltipContext context, List<TooltipContribution> sink) {
        for (var contributor : CONTRIBUTORS) {
            Collection<TooltipContribution> additions = contributor.apply(context);
            if (additions != null) sink.addAll(additions);
        }
    }
}
