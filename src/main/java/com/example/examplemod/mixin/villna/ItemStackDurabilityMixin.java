package com.example.examplemod.mixin.villna;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {

    @Shadow public abstract int getMaxDamage();

    /**
     * 自定义业务判定条件：决定哪些装备享有无限耐久
     */
    @Unique
    private boolean shouldBeInfinite(ItemStack stack) {
        if (stack.isEmpty() || this.getMaxDamage() <= 0) {
            return false;
        }

        // 示例：所有具有耐久度的工具/武器/防具无限耐久
        // 如果想按 Tag 过滤，可使用：stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools")))
        return stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof DiggerItem
                || stack.getItem() instanceof SwordItem;
    }

    /**
     * 1. 核心拦截：原版一切损耗和耐久条判断的核心就是 isDamageableItem()
     * 当返回 false 时：
     * - hurtAndBreak 会直接跳过，物品绝不损耗/破碎
     * - isDamaged() 恒为 false（见源码: this.isDamageableItem() && ...）
     * - 玩家手持该物品时，经验球不会被 Mending 徒劳消耗
     * - 原版铁砧修补逻辑自洽（无法使用材料修补没坏的物品）
     */
    @Inject(method = "isDamageableItem", at = @At("HEAD"), cancellable = true)
    private void makeIndestructible(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (this.shouldBeInfinite(self)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 2. 边界情况 A：防止其他模组/外部调用暴力 bypass hurtAndBreak，直接 setDamageValue
     */
    @Inject(method = "setDamageValue", at = @At("HEAD"), cancellable = true)
    private void preventDirectDamage(int damage, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (this.shouldBeInfinite(self)) {
            ci.cancel(); // 阻断写入任何非0的损坏值
        }
    }

    /**
     * 3. 边界情况 B：如果物品之前已经受过伤（已有 damage），强制对外报 0
     * 防止残留伤痕导致合成配方不匹配、或者触发特定的损坏检测逻辑
     */
    @Inject(method = "getDamageValue", at = @At("HEAD"), cancellable = true)
    private void normalizeDamageValue(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (this.shouldBeInfinite(self)) {
            cir.setReturnValue(0);
        }
    }
}