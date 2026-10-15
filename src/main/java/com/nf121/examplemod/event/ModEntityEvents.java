package com.nf121.examplemod.event;

import com.nf121.examplemod.ExampleMod;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import static com.nf121.examplemod.registry.ModRegistry.TIMED_MODIFIERS;


@EventBusSubscriber(modid = ExampleMod.MOD_ID)
public class ModEntityEvents {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) {
            if (living.hasData(TIMED_MODIFIERS.get())) {
                living.getData(TIMED_MODIFIERS.get()).tick(living);
            }
        }
    }
}