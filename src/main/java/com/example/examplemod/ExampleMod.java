package com.example.examplemod;

import com.example.examplemod.compat.apothic.affix.SpellLevelAffix;
import com.example.examplemod.config.LaserConfig;
import com.example.examplemod.registry.ModAttributes;
import com.example.examplemod.registry.ModDataComponents;
import com.example.examplemod.registry.ModMobEffects;
import com.example.examplemod.registry.ModRegistry;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(ExampleMod.MODID)
public final class ExampleMod {
    public static final String MODID = "examplemod";
    public static final L2Registrate REGISTRATE = new L2Registrate(MODID);

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, LaserConfig.SPEC);
        ModMobEffects.register(modEventBus);
        ModRegistry.init();
        ModDataComponents.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        ModAttributes.register(modEventBus);
        ModRegistry.RECIPE_SERIALIZERS.register(modEventBus);
    }
    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AffixRegistry.INSTANCE.registerCodec(
                    ResourceLocation.fromNamespaceAndPath("examplemod", "spell_level"),
                    SpellLevelAffix.CODEC
            );
        });
    }
}
