package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.trim.RunewordSmithingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ExampleMod.MODID);

    public static final Supplier<RecipeSerializer<RunewordSmithingRecipe>> RUNEWORD_SMITHING_SERIALIZER =
            SERIALIZERS.register("runeword_smithing", RunewordSmithingRecipe.Serializer::new);
}