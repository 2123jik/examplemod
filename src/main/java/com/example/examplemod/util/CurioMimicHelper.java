package com.example.examplemod.util;

import com.example.examplemod.compat.apothic.CurioStackIdentity;
import com.example.examplemod.registry.ModDataComponents;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.util.thread.EffectiveSide;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public final class CurioMimicHelper {

    private CurioMimicHelper() {
    }

    public static boolean isCurio(ItemStack stack) {
        return !stack.isEmpty()
                && !CuriosApi.getItemStackSlots(stack, EffectiveSide.get().isClient()).isEmpty();
    }

    public static boolean hasMimicCategory(ItemStack stack) {
        if (!ModDataComponents.MIMIC_CATEGORY.isBound()) {
            return false;
        }
        return stack.has(ModDataComponents.MIMIC_CATEGORY.get());
    }

    /**
     * 读取旧物品保存的分类。
     * 不删除、不覆盖无法解析的旧组件。
     */
    @Nullable
    public static LootCategory getSavedCategory(ItemStack stack) {
        if (!ModDataComponents.MIMIC_CATEGORY.isBound()) {
            return null;
        }

        ResourceLocation id = stack.get(ModDataComponents.MIMIC_CATEGORY.get());
        if (id == null) {
            return null;
        }

        LootCategory category = Apoth.BuiltInRegs.LOOT_CATEGORY.get(id);
        return category != null && !category.isNone() ? category : null;
    }

    public static LootCategory resolveCategory(ItemStack stack, LootCategory original) {
        if (stack.isEmpty() || !ModDataComponents.MIMIC_CATEGORY.isBound()) {
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

    public static void assignRandomCategory(ItemStack stack) {
        if (stack.isEmpty()
                || !ModDataComponents.MIMIC_CATEGORY.isBound()
                || hasMimicCategory(stack)
                || !isCurio(stack)) {
            return;
        }

        List<ResourceLocation> pool = Apoth.BuiltInRegs.LOOT_CATEGORY.stream()
                .filter(category -> !category.isNone())
                .filter(category -> !category.isBreaker())
                .map(LootCategory::getKey)
                .filter(key -> key != null
                        && "apotheosis".equals(key.getNamespace())
                        && !"none".equals(key.getPath()))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();

        if (pool.isEmpty()) {
            return;
        }

        int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size());
        stack.set(ModDataComponents.MIMIC_CATEGORY.get(), pool.get(index));
    }

}
