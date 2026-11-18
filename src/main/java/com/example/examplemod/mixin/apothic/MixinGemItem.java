package com.example.examplemod.mixin.apothic;

import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = GemItem.class, remap = false)
public abstract class MixinGemItem {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (!stack.has(ModDataComponents.IS_UNIDENTIFIED.get())) return;

        // 直接用 GemItem 自身的 getPurity
        Purity purity = GemItem.getPurity(stack);
        cir.setReturnValue(Component.translatable("item.examplemod.unidentified_gem", purity.toComponent()));
    }

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedTooltip(ItemStack stack, Item.TooltipContext ctx,
                                             List<Component> lines, TooltipFlag flag,
                                             CallbackInfo ci) {
        if (!stack.has(ModDataComponents.IS_UNIDENTIFIED.get())) return;

        lines.add(Component.translatable("tooltip.examplemod.right_click_to_identify"));
        ci.cancel(); // 阻断原来 UnsocketedGem.of(stack) 的执行，防止报错显示 "Errored gem with no bonus!"
    }
}