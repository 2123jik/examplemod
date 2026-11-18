package com.example.examplemod.mixin.vanilla;

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

    
    @Overwrite
    public void shuffleAndSplitItems(ObjectArrayList<ItemStack> stacks, int emptySlotsCount, RandomSource random) {

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


            int idx = 0;
            while (total > 0 && idx < group.size()) {
                int give = Math.min(maxStack, total);
                group.get(idx).setCount(give);
                total -= give;
                idx++;
            }

            for (; idx < group.size(); idx++) group.get(idx).setCount(0);
        }
    }


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

        }
    }
}