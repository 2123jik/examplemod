package com.example.examplemod;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED;

    public static final ModConfigSpec.BooleanValue SHOW_DURABILITY;
    public static final ModConfigSpec.BooleanValue SHOW_TOOL_SPEED;
    public static final ModConfigSpec.BooleanValue SHOW_FOOD;
    public static final ModConfigSpec.BooleanValue SHOW_EFFECTS;
    public static final ModConfigSpec.BooleanValue SHOW_FUEL;
    public static final ModConfigSpec.BooleanValue SHOW_COMPOST;
    public static final ModConfigSpec.BooleanValue SHOW_COOLDOWN;
    public static final ModConfigSpec.BooleanValue SHOW_CONTAINER_PREVIEW;
    public static final ModConfigSpec.BooleanValue SHOW_MODEL_STAGE;

    public static final ModConfigSpec.BooleanValue SHOW_CUSTOM_RULES;

    public static final ModConfigSpec.BooleanValue SHOW_HELD_HUD;
    public static final ModConfigSpec.BooleanValue SHOW_SLOT_DEBUG;

    public static final ModConfigSpec.BooleanValue RARITY_THEME;
    public static final ModConfigSpec.IntValue PINNED_PAGE_SIZE;
    public static final ModConfigSpec.IntValue MAX_LINES;
    public static final ModConfigSpec.IntValue MAX_TOOLTIP_WIDTH;
    public static final ModConfigSpec.LongValue BACKGROUND_COLOR;
    public static final ModConfigSpec.LongValue BORDER_COLOR;
    // ==== L2Hostility 联动 ====
    public static final ModConfigSpec.DoubleValue L2H_TRAIT_CHANCE_SOFT_CAP;
    public static final ModConfigSpec.DoubleValue L2H_TRAIT_CHANCE_HALF_LEVEL;
    public static final ModConfigSpec.DoubleValue L2H_TRAIT_CHANCE_FLOOR;
    public static final ModConfigSpec.BooleanValue L2H_ENABLE_SOFT_SATURATION;
    static {
        BUILDER.push("general");
        ENABLED = BUILDER.comment("启用 Tooltip 增强层").define("enabled", true);
        BUILDER.pop();

        BUILDER.push("features");
        SHOW_DURABILITY = BUILDER.comment("显示耐久度平滑色彩条").define("showDurability", true);
        SHOW_TOOL_SPEED = BUILDER.comment("显示工具采掘倍率").define("showToolSpeed", true);
        SHOW_FOOD = BUILDER.comment("显示饱食度与饱和度静态图标").define("showFood", true);
        SHOW_EFFECTS = BUILDER.comment("显示药水/迷之炖菜效果").define("showEffects", true);
        SHOW_FUEL = BUILDER.comment("显示熔炉燃料燃烧耗时与份数").define("showFuel", true);
        SHOW_COMPOST = BUILDER.comment("显示堆肥概率百分比").define("showCompost", true);
        SHOW_COOLDOWN = BUILDER.comment("显示物品冷却指示条").define("showCooldown", true);
        SHOW_CONTAINER_PREVIEW = BUILDER.comment("显示容器与收纳袋网格预览").define("showContainerPreview", true);
        SHOW_MODEL_STAGE = BUILDER.comment("在 Tooltip 侧边显示 3D 模型舞台 (可随时关闭)").define("showModelStage", true);
        BUILDER.pop();

        BUILDER.push("rules");
        SHOW_CUSTOM_RULES = BUILDER.comment("加载 JSON 自定义规则文本").define("showCustomRules", true);
        BUILDER.pop();

        BUILDER.push("hud");
        SHOW_HELD_HUD = BUILDER.comment("在快捷栏上方显示主手物品概览").define("showHeldHud", true);
        SHOW_SLOT_DEBUG = BUILDER.comment("显示槽位索引调试信息").define("showSlotDebug", false);
        BUILDER.pop();

        BUILDER.push("appearance");
        BUILDER.push("l2hostility");
        L2H_TRAIT_CHANCE_SOFT_CAP = BUILDER
                .comment("L2Hostility 词条概率的渐近上限 (软饱和)，必须落在 (0,1) 开区间")
                .comment("原版 Math.min(1, lv*slope) 是硬截断，lv>=100 恒为 1.0")
                .comment("设为 0.85 表示即使 Lv.10000 的怪也最多 85% 概率带词条")
                .defineInRange("traitChanceSoftCap", 0.85, 0.01, 0.999);

        L2H_TRAIT_CHANCE_HALF_LEVEL = BUILDER
                .comment("达到 softCap 一半时所对应的怪物等级")
                .comment("与原版 initialTraitChanceSlope 解耦，方便单独调曲线陡峭程度")
                .comment("默认 200：Lv.200 时约为 42.5%")
                .defineInRange("traitChanceHalfLevel", 200.0, 1.0, 100000.0);

        L2H_TRAIT_CHANCE_FLOOR = BUILDER
                .comment("词条概率的最小保底值，防止低等级怪永远不带词条")
                .comment("设为 0 则完全按曲线走")
                .defineInRange("traitChanceFloor", 0.0, 0.0, 0.9);

        L2H_ENABLE_SOFT_SATURATION = BUILDER
                .comment("总开关：关闭后恢复原版 Math.min(1, lv*slope) 硬截断行为")
                .define("enableSoftSaturation", true);
        BUILDER.pop();
        RARITY_THEME = BUILDER.comment("边框颜色跟随物品稀有度颜色").define("rarityTheme", true);
        PINNED_PAGE_SIZE = BUILDER.comment("固定 Tooltip 每页行数").defineInRange("pinnedPageSize", 12, 4, 64);
        MAX_LINES = BUILDER.comment("最大附加行数").defineInRange("maxLines", 32, 0, 128);
        MAX_TOOLTIP_WIDTH = BUILDER.comment("Tooltip 最大像素宽度").defineInRange("maxTooltipWidth", 320, 120, 640);
        BACKGROUND_COLOR = BUILDER.comment("背景 ARGB").defineInRange("backgroundColor", 0xF0100010L, 0L, 0xFFFFFFFFL);
        BORDER_COLOR = BUILDER.comment("边框 ARGB").defineInRange("borderColor", 0xFF5050A0L, 0L, 0xFFFFFFFFL);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int getBackgroundColor() {
        return (int) BACKGROUND_COLOR.get().longValue();
    }

    public static int getBorderColor() {
        return (int) BORDER_COLOR.get().longValue();
    }

    private Config() {}
}