package com.example.examplemod.util;

import com.example.examplemod.compat.apothic.CurioStackIdentity;
import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public final class CurioMimicHelper {

    private CurioMimicHelper() {
    }

    public static boolean isCurio(ItemStack stack) {
        return !stack.isEmpty()
                && !CuriosApi.getItemStackSlots(stack, true).isEmpty();
    }

    /**
     * 保留原方法的含义：
     * 判断是否存在分类组件，不代表该分类当前仍然有效。
     */
    public static boolean hasMimicCategory(ItemStack stack) {
        return stack.has(ModDataComponents.MIMIC_CATEGORY.get());
    }

    /**
     * 读取旧物品保存的分类。
     * 不删除、不覆盖无法解析的旧组件。
     */
    @Nullable
    public static LootCategory getSavedCategory(ItemStack stack) {
        ResourceLocation id =
                stack.get(ModDataComponents.MIMIC_CATEGORY.get());

        if (id == null) {
            return null;
        }

        LootCategory category = Apoth.BuiltInRegs.LOOT_CATEGORY.get(id);

        return category != null && !category.isNone()
                ? category
                : null;
    }



    /**
     * 神化分类查询入口：
     * 旧分类 > 神化原有分类 > 饰品拟态分类。
     *
     * 整个方法不修改物品数据。
     */
    public static LootCategory resolveCategory(
            ItemStack stack,
            LootCategory original
    ) {
        if (stack.isEmpty()) {
            return original;
        }

        LootCategory saved = getSavedCategory(stack);
        if (saved != null) {
            return saved;
        }

        if (!original.isNone()) {
            return original;
        }

        if (!isCurio(stack)) {
            return original;
        }

        CurioStackIdentity.initialize(stack);

        LootCategory assigned = getSavedCategory(stack);

        return assigned != null ? assigned : original;
    }


    /**
     * 为一个物品堆栈选择分类并保存。
     *
     * 必须在服务端调用。
     * 已有分类组件的物品不会重新随机。
     */
    public static void assignRandomCategory(ItemStack stack) {
        if (stack.isEmpty()
                || hasMimicCategory(stack)
                || !isCurio(stack)) {
            return;
        }

        List<ResourceLocation> pool =
                Apoth.BuiltInRegs.LOOT_CATEGORY.stream()
                        .filter(category -> !category.isNone())
                        .filter(category -> !category.isBreaker())
                        .map(LootCategory::getKey)
                        .filter(key ->
                                key != null
                                        && "apotheosis".equals(key.getNamespace())
                                        && !"none".equals(key.getPath())
                        )
                        .sorted(Comparator.comparing(ResourceLocation::toString))
                        .toList();


        if (pool.isEmpty()) {
            return;
        }

        int index = java.util.concurrent.ThreadLocalRandom.current()
                .nextInt(pool.size());

        stack.set(
                ModDataComponents.MIMIC_CATEGORY.get(),
                pool.get(index)
        );
    }

}
