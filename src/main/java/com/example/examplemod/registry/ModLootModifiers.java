package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.loot.EntityLootModifier;
import com.example.examplemod.registry.loot.WeaponReplacementModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ExampleMod.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<WeaponReplacementModifier>> REPLACE_WEAPONS =
            LOOT_MODIFIERS.register("replace_weapons", () -> WeaponReplacementModifier.CODEC);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<EntityLootModifier>> ENTITY_LOOT =
            LOOT_MODIFIERS.register("entity_loot", () -> EntityLootModifier.CODEC);
}