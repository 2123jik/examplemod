package com.example.examplemod.registry;

import com.example.examplemod.content.item.PortableBrewingData;
import com.example.examplemod.content.item.PortableFurnaceData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

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

    public record TrimStep(String pattern, String material) {
        public static final Codec<TrimStep> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("pattern").forGetter(TrimStep::pattern),
                        Codec.STRING.fieldOf("material").forGetter(TrimStep::material)
                ).apply(instance, TrimStep::new)
        );

        // 增加网络编解码器，保证客户端完美同步
        public static final StreamCodec<ByteBuf, TrimStep> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TrimStep::pattern,
                ByteBufCodecs.STRING_UTF8, TrimStep::material,
                TrimStep::new
        );
    }

    // 1. 镶嵌时序历史记录 (带网络同步)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<TrimStep>>> TRIM_HISTORY =
            COMPONENTS.register("trim_history", () ->
                    DataComponentType.<List<TrimStep>>builder()
                            .persistent(TrimStep.CODEC.listOf())
                            .networkSynchronized(TrimStep.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build()
            );

    // 2. 当前激活的符文之语 ID (带网络同步)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ACTIVE_RUNEWORD =
            COMPONENTS.register("active_runeword", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            );
}