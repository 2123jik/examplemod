package com.nf121.examplemod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class LaserConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue BLADE_FORWARD;
    public static final ModConfigSpec.DoubleValue BLADE_UP;
    public static final ModConfigSpec.DoubleValue PITCH_DEGREES;
    public static final ModConfigSpec.IntValue START_TICK;
    public static final ModConfigSpec.IntValue END_TICK;
    public static final ModConfigSpec.DoubleValue DAMAGE;
    public static final ModConfigSpec.DoubleValue HP_DAMAGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("Ignis Poke Laser Settings");

        BLADE_FORWARD = builder
                .comment("【剑锋水平距离】：从焰魔身体中心向正前方延伸的格数。受害者挂在 4.0 格，穿透受害者的剑尖大约在 5.0 ~ 5.3 格")
                .defineInRange("bladeForward", 5.2D, 0.0D, 20.0D);

        BLADE_UP = builder
                .comment("【剑锋高度】：从焰魔脚底垂直向上的格数。目标胸口约在 5.0 格，穿出的剑尖约在 5.2 ~ 5.6 格")
                .defineInRange("bladeUp", 5.3D, 0.0D, 20.0D);

        PITCH_DEGREES = builder
                .comment("【剑锋斜上仰角】：激光射出的倾斜仰角（度数）。需与大剑上挑角度平行（默认推荐 27 ~ 29 度）")
                .defineInRange("pitchDegrees", 28.0D, -90.0D, 90.0D);

        START_TICK = builder
                .comment("【顶端爆发时刻】：目标被挑至最高顶点的动画 Tick（源码公式算出的顶点时刻为 25）")
                .defineInRange("startTick", 25, 0, 65);

        END_TICK = builder
                .comment("【放下熄灭时刻】：焰魔将目标从剑上放下的动画 Tick（源码中放下的时刻为 46）")
                .defineInRange("endTick", 46, 0, 65);

        DAMAGE = builder
                .comment("激光基础每 Tick 伤害")
                .defineInRange("damage", 20.0D, 0.0D, 1000.0D);

        HP_DAMAGE = builder
                .comment("激光最大生命值百分比伤害 (%)")
                .defineInRange("hpDamage", 5.0D, 0.0D, 100.0D);

        builder.pop();
        SPEC = builder.build();
    }
}