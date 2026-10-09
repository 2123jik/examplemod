package com.example.examplemod.mixin.apothic;

import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootCategory.class, remap = false)
public abstract class LootCategoryMixin {

    @Inject(
            method =
                    "forItem(Lnet/minecraft/world/item/ItemStack;)"
                    + "Ldev/shadowsoffire/apotheosis/loot/LootCategory;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void examplemod$resolveCurioCategory(
            ItemStack stack,
            CallbackInfoReturnable<LootCategory> cir
    ) {
        LootCategory original = cir.getReturnValue();

        LootCategory resolved =
                CurioMimicHelper.resolveCategory(stack, original);

        if (resolved != original) {
            cir.setReturnValue(resolved);
        }
    }
}
