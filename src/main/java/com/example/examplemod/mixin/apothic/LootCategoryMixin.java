package com.example.examplemod.mixin.apothic;

import com.example.examplemod.registry.ModDataComponents;
import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootCategory.class, remap = false)
public abstract class LootCategoryMixin {

    @Inject(method = "forItem", at = @At("HEAD"), cancellable = true)
    private static void onForItem(ItemStack stack, CallbackInfoReturnable<LootCategory> cir) {
        if (stack.isEmpty() || !CurioMimicHelper.isCurio(stack)) {
            return;
        }

        // 如果该饰品还没有拟态分类，自动赋予一个随机分类（或者可以在物品生成/重铸时赋予）
        if (!CurioMimicHelper.hasMimicCategory(stack)) {
            CurioMimicHelper.assignRandomCategory(stack);
        }

        ResourceLocation catKey = stack.get(ModDataComponents.MIMIC_CATEGORY.get());
        if (catKey != null) {
            LootCategory category = Apoth.BuiltInRegs.LOOT_CATEGORY.get(catKey);
            if (category != null && !category.isNone()) {
                cir.setReturnValue(category);
            }
        }
    }
}