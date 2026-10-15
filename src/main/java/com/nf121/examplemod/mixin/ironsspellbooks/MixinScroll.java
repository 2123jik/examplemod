package com.nf121.examplemod.mixin.ironsspellbooks;

import com.nf121.examplemod.registry.ModDataComponents;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.Scroll;
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

@Mixin(value = Scroll.class, remap = false)
public abstract class MixinScroll {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (!stack.has(ModDataComponents.IS_UNIDENTIFIED.get())) return;

        var scrollData = stack.get(ModDataComponents.SCROLL_INFO.get());
        SpellRarity rarity = scrollData != null ? scrollData.rarity() : SpellRarity.COMMON;

        cir.setReturnValue(Component.translatable("item.examplemod.unidentified_scroll", rarity.getDisplayName()));
    }

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void overrideUnidentifiedTooltip(ItemStack stack, Item.TooltipContext ctx,
                                             List<Component> lines, TooltipFlag flag,
                                             CallbackInfo ci) {
        if (!stack.has(ModDataComponents.IS_UNIDENTIFIED.get())) return;

        var scrollData = stack.get(ModDataComponents.SCROLL_INFO.get());
        if (scrollData != null && scrollData.schoolFilter().isPresent()) {
            SchoolType school = SchoolRegistry.getSchool(scrollData.schoolFilter().get());
            if (school != null) {
                lines.add(Component.translatable("tooltip.examplemod.school_filter", school.getDisplayName()));
            }
        }

        lines.add(Component.translatable("tooltip.examplemod.right_click_to_identify"));
        ci.cancel(); // 阻断原版读取法术信息的逻辑，防止空法术报错
    }
}