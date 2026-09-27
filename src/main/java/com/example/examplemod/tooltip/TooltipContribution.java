package com.example.examplemod.tooltip;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public record TooltipContribution(
        TooltipCategory category,
        int priority,
        String key,
        Either<Component, TooltipComponent> content,
        BooleanSupplier visible
) {
    public TooltipContribution {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(visible, "visible");
    }

    public static TooltipContribution text(
            TooltipCategory category, int priority, String key, Component text
    ) {
        return new TooltipContribution(
                category, priority, key,
                Either.left(Objects.requireNonNull(text, "text")),
                () -> true
        );
    }

    public static TooltipContribution visual(
            TooltipCategory category, int priority, String key, TooltipComponent visual
    ) {
        return new TooltipContribution(
                category, priority, key,
                Either.right(Objects.requireNonNull(visual, "visual")),
                () -> true
        );
    }


    public boolean isVisible() {
        return this.visible.getAsBoolean();
    }
}