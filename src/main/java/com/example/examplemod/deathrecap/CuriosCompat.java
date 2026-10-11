package com.example.examplemod.deathrecap;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CuriosCompat {

    public static List<ItemStack> getCurios(LivingEntity entity) {
        List<ItemStack> list = new ArrayList<>();
        // 只有加载了 curios 模组时才通过 API 读取
        if (ModList.get().isLoaded("curios")) {
            collectCuriosInternal(entity, list);
        }
        return list;
    }

    private static void collectCuriosInternal(LivingEntity entity, List<ItemStack> out) {
        try {
            Optional<ICuriosItemHandler> handler = CuriosApi.getCuriosInventory(entity);
            handler.ifPresent(curios -> {
                var equipped = curios.getEquippedCurios();
                for (int i = 0; i < equipped.getSlots(); i++) {
                    ItemStack stack = equipped.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        out.add(stack.copy()); // 深拷贝饰品
                    }
                }
            });
        } catch (Throwable ignored) {
            // 兼容性保护
        }
    }
}