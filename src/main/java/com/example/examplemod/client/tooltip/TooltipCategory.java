package com.example.examplemod.client.tooltip;

public enum TooltipCategory {
    IDENTITY(0),
    DURABILITY(10),
    ATTRIBUTES(20),
    TOOL(25),
    ENCHANTMENTS(30),
    EFFECTS(40),
    FOOD(50),
    FUEL(60),
    COMPOST(65),
    COOLDOWN(70),
    TAGS(80),
    COMPONENTS(90),
    CONTAINER_PREVIEW(100),
    CUSTOM(110),
    DEBUG(120);

    private final int order;

    TooltipCategory(int order) {
        this.order = order;
    }

    public int order() {
        return this.order;
    }
}