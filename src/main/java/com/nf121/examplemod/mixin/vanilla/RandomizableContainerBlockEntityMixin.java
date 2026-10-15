package com.nf121.examplemod.mixin.vanilla;

import com.nf121.examplemod.entity.chest.PlayerMarkedChest;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin {

    @Shadow
    @Nullable
    protected ResourceKey<LootTable> lootTable;

    @Shadow
    public abstract boolean canOpen(Player player);

    /**
     * 在原版 unpackLootTable(player) 之前记录首次开启玩家。
     */
    @Inject(
            method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
            at = @At("HEAD")
    )
    private void examplemod$markBeforeLootTableUnpack(
            int containerId,
            Inventory playerInventory,
            Player player,
            CallbackInfoReturnable<AbstractContainerMenu> callback
    ) {
        /*
         * 普通箱子没有待解包的 LootTable。
         */
        if (this.lootTable == null) {
            return;
        }

        /*
         * 只允许逻辑服务端上的真实 ServerPlayer 修改数据。
         */
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        /*
         * 与原版 createMenu 的条件保持一致。
         *
         * 例如上锁、旁观者等不能开启箱子的情况不会被记录。
         */
        if (!this.canOpen(player)) {
            return;
        }

        Object self = this;

        if (!(self instanceof ChestBlockEntity chest)) {
            return;
        }

        ((PlayerMarkedChest) chest)
                .examplemod$mark(serverPlayer.getUUID());
    }
}
