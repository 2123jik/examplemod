package com.example.examplemod.engine;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Supplier;

@EventBusSubscriber(modid = "examplemod")
public class SpellBackpackData {

    public static final int TEMP_SLOTS = 9;
    public static final int BACKPACK_PAGE_SLOTS = 36;

    private static final ResourceLocation BACKPACK_ID =
            ResourceLocation.fromNamespaceAndPath("examplemod", "spell_backpack");

    public static final Supplier<AttachmentType<SpellBackpackData>> BACKPACK_DATA =
            DeferredHolder.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BACKPACK_ID);

    public final ItemStackHandler tempBar = new ItemStackHandler(TEMP_SLOTS);
    public final ItemStackHandler activePage = new ItemStackHandler(BACKPACK_PAGE_SLOTS);
    public final ItemStackHandler enhancePage = new ItemStackHandler(BACKPACK_PAGE_SLOTS);
    public final ItemStackHandler passivePage = new ItemStackHandler(BACKPACK_PAGE_SLOTS);

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, h ->
                h.register(BACKPACK_ID, AttachmentType.builder(SpellBackpackData::new)
                        .serialize(new Serializer())
                        .copyOnDeath()
                        .build()));
    }

    public static class Serializer implements IAttachmentSerializer<CompoundTag, SpellBackpackData> {
        @Override
        public SpellBackpackData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            SpellBackpackData data = new SpellBackpackData();
            if (tag.contains("temp")) data.tempBar.deserializeNBT(provider, tag.getCompound("temp"));
            if (tag.contains("active")) data.activePage.deserializeNBT(provider, tag.getCompound("active"));
            if (tag.contains("enhance")) data.enhancePage.deserializeNBT(provider, tag.getCompound("enhance"));
            if (tag.contains("passive")) data.passivePage.deserializeNBT(provider, tag.getCompound("passive"));
            return data;
        }

        @Override
        public CompoundTag write(SpellBackpackData data, HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.put("temp", data.tempBar.serializeNBT(provider));
            tag.put("active", data.activePage.serializeNBT(provider));
            tag.put("enhance", data.enhancePage.serializeNBT(provider));
            tag.put("passive", data.passivePage.serializeNBT(provider));
            return tag;
        }
    }
}