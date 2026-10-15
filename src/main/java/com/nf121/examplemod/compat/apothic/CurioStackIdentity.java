package com.nf121.examplemod.compat.apothic;

import com.nf121.examplemod.registry.ModDataComponents;
import com.nf121.examplemod.util.CurioMimicHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.util.thread.EffectiveSide;

import java.util.UUID;

public final class CurioStackIdentity {

    private CurioStackIdentity() {
    }

    /**
     * 仅在服务端初始化并保存。
     * 客户端读取通过物品组件同步过来的结果。
     */
    public static void initialize(ItemStack stack) {
        if (stack.isEmpty()
                || EffectiveSide.get().isClient()
                || !CurioMimicHelper.isCurio(stack)) {
            return;
        }

        if (!stack.has(ModDataComponents.ITEM_INSTANCE_UUID.get())) {
            stack.set(
                    ModDataComponents.ITEM_INSTANCE_UUID.get(),
                    UUID.randomUUID()
            );
        }

        CurioMimicHelper.assignRandomCategory(stack);
    }
}
