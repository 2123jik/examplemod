package com.example.examplemod.mixin.apothic;

import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LootTable.class)
public class MixinLootTable {

    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At("RETURN"))
    private void convertAllGemsToUnidentified(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        ObjectArrayList<ItemStack> lootList = cir.getReturnValue();
        if (lootList == null || lootList.isEmpty()) return;

        for (int i = 0; i < lootList.size(); i++) {
            ItemStack stack = lootList.get(i);

            if (stack.getItem() instanceof GemItem) {
                // 如果已经鉴定过或者是未鉴定宝石，直接放行
                if (stack.has(ModDataComponents.ALREADY_IDENTIFIED.get()) || stack.has(ModDataComponents.IS_UNIDENTIFIED.get())) {
                    continue;
                }

                Purity officialPurity = GemItem.getPurity(stack);

                // 创建干净的宝石载体，不设具体的 GEM 数据
                ItemStack unidentified = new ItemStack(stack.getItem());
                unidentified.set(DataComponents.MAX_STACK_SIZE, 64);
                GemItem.setPurity(unidentified, officialPurity);

                // 挂上未鉴定标记
                unidentified.set(ModDataComponents.IS_UNIDENTIFIED,true);

                lootList.set(i, unidentified);
            }
        }
    }
}