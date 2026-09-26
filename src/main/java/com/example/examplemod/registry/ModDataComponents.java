package com.example.examplemod.registry;

import com.example.examplemod.item.PortableBrewingData;
import com.example.examplemod.item.PortableFurnaceData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

import static com.example.examplemod.registry.ModItems.ITEMS;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "examplemod");

    // 存储分类的 ResourceLocation（例如 "apotheosis:helmet"）
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


    // 3. 酿造台物品与数据组件
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortableBrewingData>> BREWING_DATA =
            COMPONENTS.register("brewing_data", () -> DataComponentType.<PortableBrewingData>builder()
                    .persistent(PortableBrewingData.CODEC)
                    .networkSynchronized(PortableBrewingData.STREAM_CODEC)
                    .build());

   }