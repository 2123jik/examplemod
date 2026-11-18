package com.example.examplemod.runeword;

import com.example.examplemod.registry.ModRegistry;
import dev.shadowsoffire.apotheosis.Apoth.Items;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.UnsocketedGem;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RuneUpgradeRecipe extends CustomRecipe {

    public RuneUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findUpgradeResult(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        UpgradeTarget target = findUpgradeResult(input);
        if (target == null) return ItemStack.EMPTY;

        Optional<Gem> nextGem = GemRegistry.INSTANCE.getValues().stream()
                .filter(g -> {
                    ResourceLocation id = g.getId();
                    return id.getNamespace().equals("examplemod")
                            && id.getPath().startsWith(String.format("runes/rune_%02d_", target.nextNumber()));
                })
                .findFirst();

        return nextGem.map(gem -> gem.toStack(target.purity())).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RUNE_UPGRADE_SERIALIZER.get();
    }

    private record UpgradeTarget(int nextNumber, Purity purity) {}

    @Nullable
    private static UpgradeTarget findUpgradeResult(CraftingInput input) {
        int count = 0;
        ResourceLocation expectedGemId = null;
        Purity expectedPurity = null;
        int currentNumber = -1;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (!stack.is(Items.GEM.value())) return null;

            UnsocketedGem inst = UnsocketedGem.of(stack);
            if (!inst.isValid() || !inst.gem().isBound()) return null;

            ResourceLocation gemId = inst.gem().getId();
            if (!gemId.getNamespace().equals("examplemod") || !gemId.getPath().startsWith("runes/rune_")) {
                return null;
            }

            int num = parseRuneNumber(gemId.getPath());
            if (num < 1 || num >= 33) return null; // 33# Zod 已是最高阶

            if (expectedGemId == null) {
                expectedGemId = gemId;
                expectedPurity = inst.purity();
                currentNumber = num;
            } else if (!expectedGemId.equals(gemId) || expectedPurity != inst.purity()) {
                return null; // 必须 3 颗完全同号、同纯度
            }

            count++;
            if (count > 3) return null;
        }

        return count == 3 ? new UpgradeTarget(currentNumber + 1, expectedPurity) : null;
    }

    private static int parseRuneNumber(String path) {
        try {
            // "runes/rune_01_el" -> "01"
            return Integer.parseInt(path.substring(11, 13));
        } catch (Exception e) {
            return -1;
        }
    }
}