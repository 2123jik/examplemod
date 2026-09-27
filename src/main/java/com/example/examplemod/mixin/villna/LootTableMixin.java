package com.example.examplemod.mixin.villna;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;

@Mixin(LootTable.class)
public class LootTableMixin
{

    /**
     * @author
     * @reason
     */
    @Overwrite
    public List<Integer> getAvailableSlots(Container inventory, RandomSource random)
    {
        ObjectArrayList<Integer> objectarraylist = new ObjectArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            if (inventory.getItem(i).isEmpty())
            {
                objectarraylist.add(i);
            }
        }
        return objectarraylist;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void shuffleAndSplitItems(ObjectArrayList<ItemStack> stacks, int emptySlotsCount, RandomSource random) {
        // 用 HashMap 分组，O(n) 索引
        Map<ItemStackKey, List<ItemStack>> groups = new LinkedHashMap<>(stacks.size());
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            groups.computeIfAbsent(new ItemStackKey(s), k -> new ArrayList<>()).add(s);
        }

        for (List<ItemStack> group : groups.values()) {
            if (group.size() < 2) continue;

            int maxStack = group.get(0).getMaxStackSize();
            int total = 0;
            for (ItemStack s : group) total += s.getCount();

            // 重新分配：前面的堆满，最后一个存余数
            int idx = 0;
            while (total > 0 && idx < group.size()) {
                int give = Math.min(maxStack, total);
                group.get(idx).setCount(give);
                total -= give;
                idx++;
            }
            // 清空多余槽位
            for (; idx < group.size(); idx++) group.get(idx).setCount(0);
        }
    }

    // 轻量级 key，只包含 item + 组件 hash（ItemStack.isSameItemSameComponents 的语义）
    private static final class ItemStackKey {
        private final Item item;
        private final int componentsHash;
        private final int hash;

        ItemStackKey(ItemStack stack) {
            this.item = stack.getItem();
            this.componentsHash = stack.getComponents().hashCode();
            this.hash = Objects.hash(item, componentsHash);
        }

        @Override public int hashCode() { return hash; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemStackKey other)) return false;
            return item == other.item
                    && componentsHash == other.componentsHash;
            // 注意：极低概率 hash 冲突会误合并，如需 100% 精确可保存 ItemStack 引用
        }
    }
}