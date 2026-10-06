package com.example.examplemod.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class CurioAffixHelper {

    /**
     * 遍历实体身上佩戴的所有 Curios 物品
     */
    public static void forEachCurio(LivingEntity entity, Consumer<ItemStack> consumer) {
        CuriosApi.getCuriosInventory(entity).ifPresent(inv -> {
            inv.getCurios().forEach((id, stacksHandler) -> {
                var handler = stacksHandler.getStacks();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        consumer.accept(stack);
                    }
                }
            });
        });
    }
}