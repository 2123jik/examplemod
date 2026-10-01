package com.example.examplemod.event;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import static com.example.examplemod.registry.ModAttachments.TIMED_MODIFIERS;

@EventBusSubscriber(modid = ExampleMod.MODID)
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