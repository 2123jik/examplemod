package com.example.examplemod.worldgen;

import com.example.examplemod.ExampleMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ExampleMod.MODID);

    // 注册类型名称为 "dense_flat"
    public static final Supplier<MapCodec<DenseFlatChunkGenerator>> DENSE_FLAT =
            CHUNK_GENERATORS.register("dense_flat", () -> DenseFlatChunkGenerator.CODEC);
}