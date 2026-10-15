package com.nf121.examplemod;

import com.nf121.examplemod.config.LaserConfig;
import com.nf121.examplemod.registry.ModDataComponents;
import com.nf121.examplemod.registry.ModMobEffects;
import com.nf121.examplemod.registry.ModRegistry;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = "examplemod";
    public static final L2Registrate REGISTRATE = new L2Registrate(MOD_ID);

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, LaserConfig.SPEC);
        ModMobEffects.init();
        ModRegistry.init();
        ModDataComponents.init();
    }
}
