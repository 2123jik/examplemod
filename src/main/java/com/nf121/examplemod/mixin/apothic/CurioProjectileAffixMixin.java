package com.nf121.examplemod.mixin.apothic;

import com.nf121.examplemod.compat.apothic.CurioProjectileHooks;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(value = AffixHelper.class, remap = false)
public abstract class CurioProjectileAffixMixin {

    @Inject(
            method =
                    "streamAffixes("
                            + "Lnet/minecraft/world/entity/projectile/Projectile;"
                            + ")Ljava/util/stream/Stream;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void examplemod$includeSnapshotAffixes(
            Projectile projectile,
            CallbackInfoReturnable<Stream<AffixInstance>> cir
    ) {
        cir.setReturnValue(
                Stream.concat(
                        cir.getReturnValue(),
                        CurioProjectileHooks.affixes(projectile)
                )
        );
    }
}
