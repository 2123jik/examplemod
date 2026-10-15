package com.nf121.examplemod.compat.apothic;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class WornCurios {

    private WornCurios() {
    }

    /**
     * 一个实际佩戴位置。
     *
     * stack 是库存中的实际物品引用。
     * 不遍历装饰栏，不按物品内容或 UUID 去重。
     */
    public record Entry(
            String slotType,
            int slotIndex,
            ItemStack stack
    ) {
    }

    public static List<Entry> entries(LivingEntity wearer) {
        List<Entry> result = new ArrayList<>();

        CuriosApi.getCuriosInventory(wearer).ifPresent(inventory -> {
            inventory.getCurios().forEach((slotType, stacksHandler) -> {
                var stacks = stacksHandler.getStacks();

                for (int index = 0; index < stacks.getSlots(); index++) {
                    ItemStack stack = stacks.getStackInSlot(index);

                    if (!stack.isEmpty()) {
                        result.add(new Entry(slotType, index, stack));
                    }
                }
            });
        });

        // 使用稳定顺序处理可能连续修改同一个结果的效果。
        result.sort(
                Comparator.comparing(Entry::slotType)
                        .thenComparingInt(Entry::slotIndex)
        );

        return result;
    }

    public static Stream<AffixInstance> affixes(LivingEntity wearer) {
        return entries(wearer).stream()
                .flatMap(entry ->
                        AffixHelper.streamAffixes(entry.stack())
                );
    }
}
