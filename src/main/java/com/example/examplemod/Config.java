package com.example.examplemod;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 核心开关
    public static final ModConfigSpec.BooleanValue ENABLED;

    // 提示框内容模块开关 (已彻底剔除战斗看板与比对)
    public static final ModConfigSpec.BooleanValue SHOW_DURABILITY;
    public static final ModConfigSpec.BooleanValue SHOW_TOOL_SPEED;
    public static final ModConfigSpec.BooleanValue SHOW_FOOD;
    public static final ModConfigSpec.BooleanValue SHOW_EFFECTS;
    public static final ModConfigSpec.BooleanValue SHOW_FUEL;
    public static final ModConfigSpec.BooleanValue SHOW_COMPOST;
    public static final ModConfigSpec.BooleanValue SHOW_COOLDOWN;
    public static final ModConfigSpec.BooleanValue SHOW_CONTAINER_PREVIEW;
    public static final ModConfigSpec.BooleanValue SHOW_MODEL_STAGE;

    // 数据驱动规则
    public static final ModConfigSpec.BooleanValue SHOW_CUSTOM_RULES;

    // HUD 与功能
    public static final ModConfigSpec.BooleanValue SHOW_HELD_HUD;
    public static final ModConfigSpec.BooleanValue SHOW_SLOT_DEBUG;

    // 排版与样式
    public static final ModConfigSpec.BooleanValue RARITY_THEME;
    public static final ModConfigSpec.IntValue PINNED_PAGE_SIZE;
    public static final ModConfigSpec.IntValue MAX_LINES;
    public static final ModConfigSpec.IntValue MAX_TOOLTIP_WIDTH;
    public static final ModConfigSpec.LongValue BACKGROUND_COLOR;
    public static final ModConfigSpec.LongValue BORDER_COLOR;

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