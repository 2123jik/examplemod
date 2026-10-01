package com.example.examplemod.util;

import com.example.examplemod.mixin.villna.ItemStackAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;

import java.util.*;

public final class FastWeaponReplacer {

    private static final double DAMAGE_TOLERANCE = 1.0;

    // 扁平连续内存查找表：Raw ID -> 候选武器Item指针数组
    // 数组下标直接就是 Item.getId(item)，无哈希、无装箱、单次寻址命中 L1/L2 缓存
    private static Item[][] LOOKUP_TABLE = null;

    /**
     * 在 FMLCommonSetupEvent 中构建一次，耗时极短（几十毫秒）
     */
    public static void init() {
        int maxId = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            maxId = Math.max(maxId, Item.getId(item));
        }

        LOOKUP_TABLE = new Item[maxId + 1][];

        // 1. 扫描所有模组物品，按伤害聚类
        Map<Integer, List<Item>> damageBucket = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();

            // 过滤：排除非武器工具、方块、投射武器等
            if (item instanceof PickaxeItem ||
                    item instanceof ShovelItem ||
                    item instanceof HoeItem ||
                    item instanceof ProjectileWeaponItem ||
                    item instanceof ShearsItem ||
                    item instanceof BlockItem) {
                continue;
            }

            if (stack.getMaxStackSize() != 1 || !stack.isDamageableItem()) {
                continue;
            }

            double dmg = getWeaponAttackDamage(stack);
            if (dmg > 0.0) {
                // 乘 2 取整作为 key，支持 0.5 步长
                int bucketKey = (int) Math.round(dmg * 2.0);
                damageBucket.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(item);
            }
        }

        // 2. 扁平化映射到数组表
        int toleranceBuckets = (int) Math.round(DAMAGE_TOLERANCE * 2.0);

        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            // 仅对剑和斧建立替换索引
            if (!stack.is(ItemTags.SWORDS) && !stack.is(ItemTags.AXES)) {
                continue;
            }

            double dmg = getWeaponAttackDamage(stack);
            if (dmg <= 0.0) continue;

            int centerBucket = (int) Math.round(dmg * 2.0);
            List<Item> candidates = new ArrayList<>();

            for (int b = centerBucket - toleranceBuckets; b <= centerBucket + toleranceBuckets; b++) {
                List<Item> inBucket = damageBucket.get(b);
                if (inBucket != null) {
                    candidates.addAll(inBucket);
                }
            }

            if (!candidates.isEmpty()) {
                LOOKUP_TABLE[Item.getId(item)] = candidates.toArray(new Item[0]);
            }
        }
    }

    /**
     * 读取 MAINHAND 伤害加成
     */
    public static double getWeaponAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        double[] damage = new double[]{0.0};

        stack.forEachModifier(EquipmentSlot.MAINHAND, (holder, modifier) -> {
            if (holder.is(Attributes.ATTACK_DAMAGE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                damage[0] += modifier.amount();
            }
        });
        return damage[0];
    }

    /**
     * 原地突变替换（Zero Heap Allocation）：
     * 不 new 任何对象，直接修改 ItemStack 内的 item 引用指针
     */
    public static void tryMutateSlot(Mob mob, EquipmentSlot slot) {
        ItemStack current = mob.getItemBySlot(slot);
        if (current.isEmpty()) return;

        Item oldItem = current.getItem();
        int rawId = Item.getId(oldItem);

        if (LOOKUP_TABLE == null || rawId >= LOOKUP_TABLE.length) return;

        Item[] candidates = LOOKUP_TABLE[rawId];
        if (candidates == null || candidates.length == 0) return;

        Item newItem = candidates[mob.getRandom().nextInt(candidates.length)];
        if (newItem != oldItem) {
            // 通过 Mixin 访问器原地修改指针，0 内存分配！
            ((ItemStackAccessor) (Object) current).setItem(newItem);
        }
    }

    /**
     * 兼容 LootModifier 场景（该场景需要返回新引用）
     */
    public static ItemStack tryReplaceStack(ItemStack original, RandomSource random) {
        if (original.isEmpty() || LOOKUP_TABLE == null) return original;

        int rawId = Item.getId(original.getItem());
        if (rawId >= LOOKUP_TABLE.length) return original;

        Item[] candidates = LOOKUP_TABLE[rawId];
        if (candidates == null || candidates.length == 0) return original;

        Item selected = candidates[random.nextInt(candidates.length)];
        return (selected != original.getItem()) ? original.transmuteCopy(selected, original.getCount()) : original;
    }
}