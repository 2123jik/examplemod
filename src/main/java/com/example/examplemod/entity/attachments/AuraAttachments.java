package com.example.examplemod.entity.attachments;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

@EventBusSubscriber(modid = "examplemod")
public class AuraAttachments {

    private static final ResourceLocation ACTIVE_AURAS_ID =
            ResourceLocation.fromNamespaceAndPath("examplemod", "active_auras");

    private static final Codec<LinkedHashMap<ResourceLocation, Integer>> AURA_MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                    .xmap(LinkedHashMap::new, Function.identity());

    public static final Supplier<AttachmentType<LinkedHashMap<ResourceLocation, Integer>>> ACTIVE_AURAS =
            DeferredHolder.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ACTIVE_AURAS_ID);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper ->
                helper.register(ACTIVE_AURAS_ID,
                        AttachmentType.builder(() -> new LinkedHashMap<ResourceLocation, Integer>())
                                .serialize(AURA_MAP_CODEC)
                                .build()
                )
        );
    }
}