package com.example.examplemod.mixin.vanilla;

import com.example.examplemod.util.MerchantAutoFill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {

    @Shadow
    @Final
    private Merchant trader;

    /**
     * 只记录选择。
     * 此时原版可能尚未完成上一笔输入材料的退回。
     */
    @Inject(
            method = "setSelectionHint(I)V",
            at = @At("TAIL")
    )
    private void examplemod$recordSelection(
            int selectedIndex,
            CallbackInfo ci
    ) {
        if (this.trader.getTradingPlayer() instanceof ServerPlayer) {
            MerchantAutoFill.select(
                    (MerchantMenu) (Object) this,
                    selectedIndex
            );
        }
    }

    /**
     * 原版尝试从玩家库存填入材料之后，
     * 再从背包范围补足缺额。
     *
     * RETURN 也覆盖原版方法的提前返回。
     * 如果旧输入仍不兼容，fillMerchant 会保持原状。
     */
    @Inject(
            method = "tryMoveItems(I)V",
            at = @At("RETURN")
    )
    private void examplemod$fillAfterSelection(
            int selectedIndex,
            CallbackInfo ci
    ) {
        if (this.trader.getTradingPlayer()
                instanceof ServerPlayer player) {
            MerchantMenu menu = (MerchantMenu) (Object) this;

            MerchantAutoFill.select(menu, selectedIndex);
            MerchantAutoFill.fill(player, menu);
        }
    }

    /**
     * 必须在整个 quickMoveStack 返回时补料：
     * 此时 MerchantResultSlot.onTake 已完成本次交易结算。
     *
     * 原版快捷点击流程可继续取出重新生成的交易结果。
     * 转移失败时不补料。
     */
    @Inject(
            method =
                    "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)"
                    + "Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN")
    )
    private void examplemod$fillAfterQuickTrade(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (slotIndex != 2
                || cir.getReturnValue().isEmpty()
                || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MerchantAutoFill.fill(
                serverPlayer,
                (MerchantMenu) (Object) this
        );
    }

    @Inject(
            method = "removed(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD")
    )
    private void examplemod$forgetSelection(
            Player player,
            CallbackInfo ci
    ) {
        if (player instanceof ServerPlayer) {
            MerchantAutoFill.forget(
                    (MerchantMenu) (Object) this
            );
        }
    }
}
