package com.example.examplemod;

import com.example.examplemod.compat.curios.ModLootCategories;
import com.example.examplemod.event.MobEquipmentHandler;
import com.example.examplemod.registry.ModDataComponents;
import com.example.examplemod.registry.ModEntities;
import com.example.examplemod.registry.ModMenus; // 导入菜单注册
import com.example.examplemod.registry.WeaponReplacementModifier;
import com.example.examplemod.spell.ModSpells;
import com.example.examplemod.spell.SummonedCaveSpider;
import com.example.examplemod.util.WeaponReplacer;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.examplemod.registry.ModItems.ITEMS;

@Mod(ExampleMod.MODID)
public final class ExampleMod {
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleMod.class);

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<WeaponReplacementModifier>> REPLACE_WEAPONS =
            LOOT_MODIFIERS.register("replace_weapons", () -> WeaponReplacementModifier.CODEC);

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        ModSpells.register(modEventBus);
        ModEntities.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(ModLootCategories::onRegister);
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(ExampleMod::registerCommands);
        NeoForge.EVENT_BUS.register(MobEquipmentHandler.class);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SUMMONED_CAVE_SPIDER.get(), SummonedCaveSpider.createAttributes().build());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(WeaponReplacer::initWeaponPools);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
    }
}