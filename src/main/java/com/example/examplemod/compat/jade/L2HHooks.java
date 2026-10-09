package com.example.examplemod.compat.jade;

import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.l2hostility.init.data.LHTagGen;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import net.minecraft.world.entity.LivingEntity;

public final class L2HHooks {
    private L2HHooks() {}

    /**
     * 获取 L2Hostility 的伤害乘数。没装 / 没等级 / 不缩放通通就是 1.0
     */
    public static double getMultiplier(LivingEntity living) {
        if (!net.neoforged.fml.ModList.get().isLoaded("l2hostility")) return 1.0;
        if (living.getType().is(LHTagGen.NO_SCALING)) return 1.0;

        var capOpt = LHMiscs.MOB.type().getExisting(living);
        if (capOpt.isEmpty()) return 1.0;

        int level = capOpt.get().getLevel();
        if (level <= 0) return 1.0;

        double dF = LHConfig.SERVER.damageFactor.get();
        double factor = LHConfig.SERVER.exponentialDamage.get()
                ? Math.pow(1.0 + dF, level) - 1.0
                : level * dF;

        var config = capOpt.get().getConfigCache(living);
        if (config != null) factor *= config.attackScale;

        return 1.0 + factor;
    }
}