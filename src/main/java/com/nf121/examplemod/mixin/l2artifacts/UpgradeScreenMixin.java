package com.nf121.examplemod.mixin.l2artifacts;

import com.nf121.examplemod.util.ArtifactStatFormatter;
import dev.xkmc.l2artifacts.content.core.ArtifactStats;
import dev.xkmc.l2artifacts.content.core.StatEntry;
import dev.xkmc.l2artifacts.content.search.upgrade.UpgradeMenuScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(value = UpgradeMenuScreen.class, remap = false)
public class UpgradeScreenMixin {

    @Shadow @Nullable private ArtifactStats current;

    @Inject(method = "addEntry", at = @At("RETURN"), cancellable = true)
    private void modifyAddEntry(boolean main, StatEntry entry, @Nullable StatEntry old, CallbackInfoReturnable<Component[]> cir) {
        Component[] ans = cir.getReturnValue();
        if (ans != null && ans.length >= 2 && this.current != null) {
            ans[1] = ArtifactStatFormatter.format(ans[1], entry, this.current.level(), main);
            cir.setReturnValue(ans);
        }
    }
}