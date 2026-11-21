package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.attribute.PhysicalSchoolAttributes;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class PhysicalSchool {

    public static final ResourceLocation PHYSICAL_ID =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "physical");

    public static final DeferredHolder<SchoolType, SchoolType> PHYSICAL =
            DeferredHolder.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, PHYSICAL_ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(SchoolRegistry.SCHOOL_REGISTRY_KEY, helper ->
                helper.register(PHYSICAL_ID, new SchoolType(
                        PHYSICAL_ID,
                        ItemTags.SWORDS,
                        Component.literal("物理").withStyle(ChatFormatting.GRAY),
                        PhysicalSchoolAttributes.PHYSICAL_SPELL_POWER,
                        PhysicalSchoolAttributes.PHYSICAL_RESIST,
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_ATTACK_STRONG),
                        DamageTypes.PLAYER_ATTACK,
                        false,
                        false
                ))
        );
    }
}