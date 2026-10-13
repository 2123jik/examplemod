package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ExampleMod.MOD_ID);

    // 注册 SimpleParticleType（第二个参数表示是否强制在所有距离渲染）
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DAMAGE_SPARK =
            PARTICLE_TYPES.register("damage_spark", () -> new SimpleParticleType(false));
}