package com.example.examplemod.registry;

import com.example.examplemod.content.item.PortableBrewingData;
import com.example.examplemod.content.item.PortableFurnaceData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "examplemod");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> MIMIC_CATEGORY =
            COMPONENTS.register("mimic_cat", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ITEM_INSTANCE_UUID =
            COMPONENTS.register("item_instance_uuid",() -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortableFurnaceData>> FURNACE_DATA =
            COMPONENTS.register("furnace_data",() ->DataComponentType.<PortableFurnaceData>builder()

                    .persistent(PortableFurnaceData.CODEC)
                    .networkSynchronized(PortableFurnaceData.STREAM_CODEC)
                    .build());


    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortableBrewingData>> BREWING_DATA =
            COMPONENTS.register("brewing_data", () -> DataComponentType.<PortableBrewingData>builder()
                    .persistent(PortableBrewingData.CODEC)
                    .networkSynchronized(PortableBrewingData.STREAM_CODEC)
                    .build());

   }