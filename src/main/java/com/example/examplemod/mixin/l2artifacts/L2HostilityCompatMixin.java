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
     * @author
     * @reason
     */
    @Overwrite
    private static boolean validForDropForHostility(Mob e, int min, int max) {
        Optional<MobTraitCap> opt = ((GeneralCapabilityHolder) LHMiscs.MOB.type()).getExisting(e);
        if (opt.isEmpty()) {
            return false;
        }

        int lv = opt.get().getLevel();

        return lv >= min && (max <= 0 || lv < max);
    }
}