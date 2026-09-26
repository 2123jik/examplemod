package com.example.examplemod.mixin.apothic;

import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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

            // 凡是战利品里生成的神化宝石，且尚未被转化的
            if (stack.getItem() instanceof GemItem) {
                CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                if (!data.copyTag().getBoolean("is_unidentified_gem")) {
                    
                    // 1. 【核心：直接读取神化官方已经计算好的真实纯度】
                    // 普通箱子神化算出来是碎裂，这里拿到的就绝对是碎裂，绝不会出现低级箱出无瑕！
                    Purity officialPurity = GemItem.getPurity(stack);

                    // 2. 擦除具体的 Gem 属性，只保留纯度，变成未鉴定原石
                    ItemStack unidentified = new ItemStack(stack.getItem());
                    unidentified.set(DataComponents.MAX_STACK_SIZE, 64);
                    GemItem.setPurity(unidentified, officialPurity);

                    CustomData.update(DataComponents.CUSTOM_DATA, unidentified, tag -> {
                        tag.putBoolean("is_unidentified_gem", true);
                        tag.putString("official_purity", officialPurity.name());
                    });

                    // 3. 替换回战利品列表
                    lootList.set(i, unidentified);
                }
            }
        }
    }
}