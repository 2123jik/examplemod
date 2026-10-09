package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.apothic.WornCurios;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.effect.AttributeToggleAffix;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(value = AttributeToggleAffix.class, remap = false)
public abstract class AttributeToggleAffixMixin {

    @Inject(
            method =
                    "streamWornAffixes("
                            + "Lnet/minecraft/world/entity/player/Player;"
                            + ")Ljava/util/stream/Stream;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void examplemod$includeWornCurios(
            Player player,
            CallbackInfoReturnable<Stream<AttributeToggleAffix>> cir
    ) {
        Stream<AttributeToggleAffix> curioAffixes =
                WornCurios.affixes(player)
                        .map(AffixInstance::getAffix)
                        .filter(AttributeToggleAffix.class::isInstance)
                        .map(AttributeToggleAffix.class::cast);

        cir.setReturnValue(
                Stream.concat(cir.getReturnValue(), curioAffixes)
        );
    }
}
