package com.nf121.examplemod.registry;

import com.nf121.examplemod.ExampleMod;
import com.nf121.examplemod.item.PortableBrewingData;
import com.nf121.examplemod.item.PortableFurnaceData;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tterrag.registrate.builders.NoConfigBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ModDataComponents {

    private static final Codec<Boolean> LENIENT_BOOL_CODEC = Codec.either(Codec.BOOL, Unit.CODEC)
            .xmap(
                    either -> either.map(b -> b, unit -> true),
                    Either::left
            );

    /** 标记为未鉴定物品（通用） */
    public static final RegistryEntry<DataComponentType<?>, DataComponentType<Boolean>> IS_UNIDENTIFIED =
            ExampleMod.REGISTRATE.entry("is_unidentified", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "is_unidentified", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(LENIENT_BOOL_CODEC)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build()
            )).register();

    /** 防回炉防刷组件：标记已鉴定过 */
    public static final RegistryEntry<DataComponentType<?>, DataComponentType<Boolean>> ALREADY_IDENTIFIED =
            ExampleMod.REGISTRATE.entry("already_identified", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "already_identified", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(LENIENT_BOOL_CODEC)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build()
            )).register();

    // ==========================================
    // 卷轴未鉴定参数 (兼容量产与新格式)
    // ==========================================

    public record UnidentifiedScrollData(SpellRarity rarity, Optional<ResourceLocation> schoolFilter) {
        private static final Codec<SpellRarity> RARITY_CODEC =
                Codec.STRING.xmap(SpellRarity::valueOf, SpellRarity::name);

        private static final StreamCodec<ByteBuf, SpellRarity> RARITY_STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.map(SpellRarity::valueOf, SpellRarity::name);

        private static final Codec<UnidentifiedScrollData> STRUCTURED_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        RARITY_CODEC.fieldOf("rarity").forGetter(UnidentifiedScrollData::rarity),
                        ResourceLocation.CODEC.optionalFieldOf("school").forGetter(UnidentifiedScrollData::schoolFilter)
                ).apply(instance, UnidentifiedScrollData::new)
        );

        private static final Codec<UnidentifiedScrollData> LEGACY_STRING_CODEC = Codec.STRING.xmap(
                s -> {
                    try {
                        String[] parts = s.split(";", 2);
                        SpellRarity r = SpellRarity.valueOf(parts[0]);
                        Optional<ResourceLocation> sch = parts.length > 1 && !parts[1].isEmpty()
                                ? Optional.of(ResourceLocation.parse(parts[1])) : Optional.empty();
                        return new UnidentifiedScrollData(r, sch);
                    } catch (Exception e) {
                        return new UnidentifiedScrollData(SpellRarity.COMMON, Optional.empty());
                    }
                },
                d -> d.rarity.name() + (d.schoolFilter.map(res -> ";" + res).orElse(""))
        );

        public static final Codec<UnidentifiedScrollData> CODEC = Codec.either(STRUCTURED_CODEC, LEGACY_STRING_CODEC)
                .xmap(
                        either -> either.map(s -> s, l -> l),
                        Either::left
                );

        public static final StreamCodec<ByteBuf, UnidentifiedScrollData> STREAM_CODEC = StreamCodec.composite(
                RARITY_STREAM_CODEC, UnidentifiedScrollData::rarity,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), UnidentifiedScrollData::schoolFilter,
                UnidentifiedScrollData::new
        );
    }

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<UnidentifiedScrollData>> SCROLL_INFO =
            ExampleMod.REGISTRATE.entry("unidentified_scroll_info", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "unidentified_scroll_info", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<UnidentifiedScrollData>builder()
                            .persistent(UnidentifiedScrollData.CODEC)
                            .networkSynchronized(UnidentifiedScrollData.STREAM_CODEC)
                            .build()
            )).register();

    // ==========================================
    // 其他业务组件
    // ==========================================

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<ResourceLocation>> MIMIC_CATEGORY =
            ExampleMod.REGISTRATE.entry("mimic_cat", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "mimic_cat", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
                            .build()
            )).register();

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<UUID>> ITEM_INSTANCE_UUID =
            ExampleMod.REGISTRATE.entry("item_instance_uuid", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "item_instance_uuid", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<UUID>builder()
                            .persistent(UUIDUtil.CODEC)
                            .networkSynchronized(UUIDUtil.STREAM_CODEC)
                            .build()
            )).register();

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<PortableFurnaceData>> FURNACE_DATA =
            ExampleMod.REGISTRATE.entry("furnace_data", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "furnace_data", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<PortableFurnaceData>builder()
                            .persistent(PortableFurnaceData.CODEC)
                            .networkSynchronized(PortableFurnaceData.STREAM_CODEC)
                            .build()
            )).register();

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<PortableBrewingData>> BREWING_DATA =
            ExampleMod.REGISTRATE.entry("brewing_data", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "brewing_data", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<PortableBrewingData>builder()
                            .persistent(PortableBrewingData.CODEC)
                            .networkSynchronized(PortableBrewingData.STREAM_CODEC)
                            .build()
            )).register();

    public record TrimStep(String pattern, String material) {
        public static final Codec<TrimStep> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("pattern").forGetter(TrimStep::pattern),
                        Codec.STRING.fieldOf("material").forGetter(TrimStep::material)
                ).apply(instance, TrimStep::new)
        );

        public static final StreamCodec<ByteBuf, TrimStep> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TrimStep::pattern,
                ByteBufCodecs.STRING_UTF8, TrimStep::material,
                TrimStep::new
        );
    }

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<List<TrimStep>>> TRIM_HISTORY =
            ExampleMod.REGISTRATE.entry("trim_history", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "trim_history", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<List<TrimStep>>builder()
                            .persistent(TrimStep.CODEC.listOf())
                            .networkSynchronized(TrimStep.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build()
            )).register();

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<String>> ACTIVE_RUNEWORD =
            ExampleMod.REGISTRATE.entry("active_runeword", cb -> new NoConfigBuilder<>(
                    ExampleMod.REGISTRATE, ExampleMod.REGISTRATE, "active_runeword", cb,
                    Registries.DATA_COMPONENT_TYPE,
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            )).register();

    public static void init(){}
}