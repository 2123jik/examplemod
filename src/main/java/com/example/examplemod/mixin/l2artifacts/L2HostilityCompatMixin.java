package com.example.examplemod.mixin.l2artifacts;

import dev.xkmc.l2artifacts.compat.L2HostilityCompat;
import dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.init.registrate.LHMiscs;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(value = L2HostilityCompat.class, remap = false)
public class L2HostilityCompatMixin {

    /**
     * @author examplemod
     * @reason 装了恶意模组时，只根据恶意等级判断，无视怪物自身血量
     */
    @Overwrite
    private static boolean validForDropForHostility(Mob e, int min, int max) {
        // 获取怪物在 L2Hostility 里的 Capability
        Optional<MobTraitCap> opt = ((GeneralCapabilityHolder) LHMiscs.MOB.type()).getExisting(e);
        if (opt.isEmpty()) {
            return false;
        }

        // 取出怪物的真实等级
        int lv = opt.get().getLevel();

        // 【修改的核心】：只看等级
        // 1. 等级必须 >= min
        // 2. 如果 max <= 0 表示无上限；否则等级必须 < max
        return lv >= min && (max <= 0 || lv < max);
    }
}