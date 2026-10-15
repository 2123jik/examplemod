package com.example.examplemod;

import com.example.examplemod.config.LaserConfig;
import com.example.examplemod.event.util.AttributeBookData;
import com.example.examplemod.registry.ModMobEffects;
import com.example.examplemod.registry.ModRegistry;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = "examplemod";

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistry.init(modEventBus);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, LaserConfig.SPEC);
        ModMobEffects.register(modEventBus);
    }
}