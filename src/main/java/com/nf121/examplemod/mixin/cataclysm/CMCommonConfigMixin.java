package com.nf121.examplemod.mixin.cataclysm;

import com.github.L_Ender.cataclysm.config.CMCommonConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 目标定位灾变的静态配置类，remap = false（因为这是模组自身代码，不需要SRG/Mojang反混淆映射）
@Mixin(value = CMCommonConfig.class, remap = false)
public class CMCommonConfigMixin {

    @Inject(method = "Commonbake", at = @At("RETURN"))
    private static void cataclysm$removeAllDamageCaps(CallbackInfo ci) {
        double max = 9999999.0D;

        // 末影卫士
        CMCommonConfig.EnderGuardian.damageCap = max;
        CMCommonConfig.EnderGuardian.dpsCap = max;
        CMCommonConfig.EnderGuardian.rangeCap = max;

        // 下界合金巨兽
        CMCommonConfig.NetheriteMonstrosity.damageCap = max;
        CMCommonConfig.NetheriteMonstrosity.dpsCap = max;
        CMCommonConfig.NetheriteMonstrosity.rangeCap = max;

        // 燃烬
        CMCommonConfig.Ignis.damageCap = max;
        CMCommonConfig.Ignis.dpsCap = max;
        CMCommonConfig.Ignis.rangeCap = max;

        // 先驱者
        CMCommonConfig.Harbinger.damageCap = max;
        CMCommonConfig.Harbinger.dpsCap = max;
        CMCommonConfig.Harbinger.rangeCap = max;

        // 利维坦
        CMCommonConfig.Leviathan.damageCap = max;
        CMCommonConfig.Leviathan.dpsCap = max;
        CMCommonConfig.Leviathan.rangeCap = max;

        // 远古残骸 / 沙漠巨兽
        CMCommonConfig.AncientRemnant.damageCap = max;
        CMCommonConfig.AncientRemnant.dpsCap = max;
        CMCommonConfig.AncientRemnant.rangeCap = max;

        // 斯库拉
        CMCommonConfig.Scylla.damageCap = max;
        CMCommonConfig.Scylla.dpsCap = max;
        CMCommonConfig.Scylla.rangeCap = max;

        // 诅咒使徒 / 玛勒迪克图斯
        CMCommonConfig.Maledictus.damageCap = max;
        CMCommonConfig.Maledictus.dpsCap = max;
        CMCommonConfig.Maledictus.rangeCap = max;

        // 钳皇
        CMCommonConfig.Clawdian.damageCap = max;
        CMCommonConfig.Clawdian.dpsCap = max;
        CMCommonConfig.Clawdian.rangeCap = max;
    }
}