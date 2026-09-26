package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.spell.SummonedCaveSpider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ExampleMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SummonedCaveSpider>> SUMMONED_CAVE_SPIDER =
            ENTITIES.register("summoned_cave_spider", () ->
                    EntityType.Builder.<SummonedCaveSpider>of(SummonedCaveSpider::new, MobCategory.MISC)
                            .sized(0.7F, 0.5F)
                            .clientTrackingRange(8)
                            .build("summoned_cave_spider")
            );

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}