package com.example.examplemod.mixin.hostility;

import com.example.examplemod.Config;
import dev.xkmc.l2hostility.content.logic.MobDifficultyCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobDifficultyCollector.class)
public class MobDifficultyCollectorMixin {

    /**
     * 只重定向 trait_chance 里的 Math.min(DD)D：
     *
     *   原： min(1, lv * slope)
     *   新： softCap * (1 - 0.5^(lv / halfLevel))，并 clamp 到 [floor, softCap]
     *
     * 不触碰 fullChance / delegateTrait / trait_chance=0 三条显式路径。
     *
     * Redirect 拿到的两个参数：
     *   a = 1.0
     *   b = lv * initialTraitChanceSlope   （已乘好的 x）
     * 由于 halfLevel 用的是等级而不是 x，需要反推 lv：
     *   lv = b / slope
     * slope 从 LHConfig.SERVER.initialTraitChanceSlope 实时读取。
     */
    @Redirect(
            method = "trait_chance",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;min(DD)D"
            )
    )
    private double examplemod$softSaturation(double one, double x) {


        double slope = dev.xkmc.l2hostility.init.data.LHConfig.SERVER
                .initialTraitChanceSlope.get();
        if (slope <= 0) {
            return Math.min(one, x);
        }

        double lv = x / slope;
        double softCap   = Config.L2H_TRAIT_CHANCE_SOFT_CAP.get();
        double halfLevel = Config.L2H_TRAIT_CHANCE_HALF_LEVEL.get();
        double floor     = Config.L2H_TRAIT_CHANCE_FLOOR.get();

        // 半饱和曲线：lv = 0 → 0；lv = halfLevel → softCap/2；lv → ∞ → softCap
        double sat = softCap * (1.0 - Math.pow(0.5, lv / halfLevel));

        // 保底 + 上限，保证结果严格 ∈ [floor, softCap]，softCap < 1
        if (sat < floor) sat = floor;
        if (sat > softCap) sat = softCap;
        return sat;
    }
}