package com.example.examplemod.mixin.villna;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SetItemDamageFunction.class)
public class SetItemDamageFunctionMixin {
    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void skipUnbreakable(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        if (!stack.isDamageableItem()) {
            cir.setReturnValue(stack); // 直接返回原物，不给它调用 LOGGER.warn 的机会
        }
    }
}