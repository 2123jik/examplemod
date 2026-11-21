package com.example.examplemod.registry.attribute;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class PhysicalSchoolAttributes {

    public static final ResourceLocation PHYSICAL_RESIST_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "physical_resist");
    public static final DeferredHolder<Attribute, Attribute> PHYSICAL_RESIST =
            DeferredHolder.create(Registries.ATTRIBUTE, PHYSICAL_RESIST_ID);

    public static final ResourceLocation PHYSICAL_SPELL_POWER_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "physical_spell_power");
    public static final DeferredHolder<Attribute, Attribute> PHYSICAL_SPELL_POWER =
            DeferredHolder.create(Registries.ATTRIBUTE, PHYSICAL_SPELL_POWER_ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.ATTRIBUTE, helper -> {
            helper.register(PHYSICAL_RESIST_ID,
                    new RangedAttribute("attribute.examplemod.physical_resist", 1.0, -1.0, 50.0).setSyncable(true));
            helper.register(PHYSICAL_SPELL_POWER_ID,
                    new RangedAttribute("attribute.examplemod.physical_spell_power", 1.0, 0.0, 50.0).setSyncable(true));
        });
    }

    @SubscribeEvent
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            event.add(type, PHYSICAL_RESIST);
            event.add(type, PHYSICAL_SPELL_POWER);
        }
    }
}