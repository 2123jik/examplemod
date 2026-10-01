package com.example.examplemod.trim;

import com.example.examplemod.registry.ModDataComponents;
import com.example.examplemod.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RunewordSmithingRecipe implements SmithingRecipe {
    final Ingredient template;
    final Ingredient base;
    final Ingredient addition;

    public RunewordSmithingRecipe(Ingredient template, Ingredient base, Ingredient addition) {
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        ItemStack baseStack = input.base();
        if (baseStack.isEmpty()) return false;

        // 已经激活完符文之语的，禁止继续锻造打乱
        if (baseStack.has(ModDataComponents.ACTIVE_RUNEWORD.get())) {
            return false;
        }

        return this.template.test(input.template()) && this.base.test(baseStack) && this.addition.test(input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack baseStack = input.base();
        if (baseStack.isEmpty() || !this.base.test(baseStack)) {
            return ItemStack.EMPTY;
        }

        ItemStack templateStack = input.template();
        ItemStack additionStack = input.addition();

        ItemStack result = baseStack.copyWithCount(1);

        // 1. 如果底板是防具，正常覆盖原版外观 Trim
        Optional<Holder.Reference<TrimMaterial>> trimMaterial = TrimMaterials.getFromIngredient(registries, additionStack);
        Optional<Holder.Reference<TrimPattern>> trimPattern = TrimPatterns.getFromTemplate(registries, templateStack);
        if (trimMaterial.isPresent() && trimPattern.isPresent()) {
            ArmorTrim trim = new ArmorTrim(trimMaterial.get(), trimPattern.get())
                    .withTooltip(false);
            result.set(DataComponents.TRIM, trim);
        }

        // 提取模式与材料 ID
        String patternId;
        if (trimPattern.isPresent()) {
            patternId = trimPattern.get().key().location().toString();
        } else {
            String rawId = BuiltInRegistries.ITEM.getKey(templateStack.getItem()).toString();
            patternId = rawId.replace("_armor_trim_smithing_template", "")
                    .replace("_smithing_template", "");
        }

        String additionId = BuiltInRegistries.ITEM.getKey(additionStack.getItem()).toString();

        // 2. 推进镶嵌时序
        List<ModDataComponents.TrimStep> history = result.getOrDefault(ModDataComponents.TRIM_HISTORY.get(), List.of());
        List<ModDataComponents.TrimStep> newHistory = new ArrayList<>(history);
        newHistory.add(new ModDataComponents.TrimStep(patternId, additionId));
        result.set(ModDataComponents.TRIM_HISTORY.get(), newHistory);

        // 3. 校验并触发共鸣质变
        checkAndActivateRuneword(result, newHistory);

        return result;
    }

    private static void checkAndActivateRuneword(ItemStack stack, List<ModDataComponents.TrimStep> history) {
        for (RunewordDefinition def : RunewordManager.RUNEWORDS.values()) {
            if (history.size() != def.sequence().size()) {
                continue;
            }

            if (!def.matchesBaseItem(stack)) {
                continue;
            }

            boolean match = true;
            for (int i = 0; i < def.sequence().size(); i++) {
                RunewordDefinition.Step expected = def.sequence().get(i);
                ModDataComponents.TrimStep actual = history.get(i);

                if (!expected.pattern().equals(actual.pattern()) || !expected.material().equals(actual.material())) {
                    match = false;
                    break;
                }
            }

            if (match) {
                // 打上激活标签，展示交由 Tooltip 统一用翻译键渲染，杜绝神化二次包裹
                stack.set(ModDataComponents.ACTIVE_RUNEWORD.get(), def.id());
                break;
            }
        }
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack[] items = this.base.getItems();
        if (items.length > 0) {
            return items[0].copy();
        }
        return new ItemStack(Items.IRON_SWORD);
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return this.template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return this.base.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return this.addition.test(stack);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RUNEWORD_SMITHING_SERIALIZER.get();
    }

    @Override
    public boolean isIncomplete() {
        return this.template.hasNoItems() || this.base.hasNoItems() || this.addition.hasNoItems();
    }

    public Ingredient getTemplate() { return this.template; }
    public Ingredient getBase() { return this.base; }
    public Ingredient getAddition() { return this.addition; }

    public static class Serializer implements RecipeSerializer<RunewordSmithingRecipe> {
        private static final MapCodec<RunewordSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("template").forGetter(r -> r.template),
                Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
                Ingredient.CODEC.fieldOf("addition").forGetter(r -> r.addition)
        ).apply(inst, RunewordSmithingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RunewordSmithingRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork
        );

        @Override
        public MapCodec<RunewordSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RunewordSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static RunewordSmithingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            Ingredient template = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            Ingredient base = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            Ingredient addition = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            return new RunewordSmithingRecipe(template, base, addition);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, RunewordSmithingRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.template);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.base);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.addition);
        }
    }
}