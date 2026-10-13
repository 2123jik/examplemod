package com.example.examplemod;

import com.example.examplemod.compat.apothic.CurioSlots;
import com.example.examplemod.network.ModPayloads;
import com.example.examplemod.registry.ModAttributes;
import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.registry.ModItems;
import com.example.examplemod.registry.ModRegistries;
import com.example.examplemod.skill.SkillRegistry;
import com.example.examplemod.spell.SummonedCaveSpider;
import com.example.examplemod.spell.SummonedSupportDragon;
import com.example.examplemod.trim.RunewordManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(ExampleMod.class);

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist.isClient()) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        }
        ModRegistries.register(modEventBus);

        modEventBus.addListener(ModItems::addCreative);
        modEventBus.addListener(ModAttributes::modifyEntityAttributes);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(CurioSlots::onRegister);

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SUMMONED_CAVE_SPIDER.get(), SummonedCaveSpider.createAttributes().build());
        event.put(ModEntities.SUMMONED_SUPPORT_DRAGON.get(), SummonedSupportDragon.createAttributes().build());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SkillRegistry::init);
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RunewordManager());
    }
}