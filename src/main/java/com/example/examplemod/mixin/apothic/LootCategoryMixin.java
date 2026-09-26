package com.example.examplemod.mixin.apothic;

import com.example.examplemod.compat.curios.ModLootCategories;
import com.example.examplemod.registry.ModDataComponents;
import com.example.examplemod.util.CurioMimicHelper;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootCategory.class, remap = false)
public abstract class LootCategoryMixin {

    @Unique
    private static final ThreadLocal<ItemStack> CURRENT_QUERY_STACK = new ThreadLocal<>();

    /**
     * 1. 终结分类随机：
     * 保证同一物品 ID 每次都能随机获得神化分类（武器、胸甲等，继承全部词缀池）
     */
    @Inject(method = "forItem", at = @At("HEAD"), cancellable = true)
    private static void onForItem(ItemStack stack, CallbackInfoReturnable<LootCategory> cir) {
        if (stack.isEmpty() || !CurioMimicHelper.isCurio(stack)) {
            CURRENT_QUERY_STACK.remove();
            return;
        }

        if (!CurioMimicHelper.hasMimicCategory(stack)) {
            CurioMimicHelper.assignRandomCategory(stack);
        }

        CURRENT_QUERY_STACK.set(stack);

        ResourceLocation catKey = stack.get(ModDataComponents.MIMIC_CATEGORY.get());
        if (catKey != null) {
            LootCategory category = Apoth.BuiltInRegs.LOOT_CATEGORY.get(catKey);
            if (category != null && !category.isNone()) {
                cir.setReturnValue(category);
            }
        }
    }

    /**
     * 2. 终结槽位脱绑：
     * 返回我们真正注册好、带有 slotHolder 的合法 Curios 槽位组！
     */
    @Inject(method = "getSlots", at = @At("HEAD"), cancellable = true)
    private void onGetSlots(CallbackInfoReturnable<EntitySlotGroup> cir) {
        ItemStack current = CURRENT_QUERY_STACK.get();
        if (current != null && CurioMimicHelper.isCurio(current)) {
            // 从 ModLootCategories 获取该物品真实的 Curios 槽位组（如项链组、戒指组）
            EntitySlotGroup realCurioGroup = ModLootCategories.getGroupForStack(current);
            if (realCurioGroup != null) {
                cir.setReturnValue(realCurioGroup);
            }
        }
    }
}