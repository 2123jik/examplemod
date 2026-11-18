package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.PortableBrewingData;
import com.example.examplemod.item.PortableFurnaceData;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ModDataComponents
{

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ExampleMod.MODID);

    private static final Codec<Boolean> LENIENT_BOOL_CODEC = Codec.either(Codec.BOOL, Unit.CODEC)
            .xmap(
                    either -> either.map(b -> b, unit -> true),
                    Either::left
            );

    /** 标记为未鉴定物品（通用） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_UNIDENTIFIED =
            DATA_COMPONENTS.registerComponentType("is_unidentified", builder -> builder
                    .persistent(LENIENT_BOOL_CODEC)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /** 防回炉防刷组件：标记已鉴定过 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ALREADY_IDENTIFIED =
            DATA_COMPONENTS.registerComponentType("already_identified", builder -> builder
                    .persistent(LENIENT_BOOL_CODEC)
                    .networkSynchronized(ByteBufCodecs.BOOL));

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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UnidentifiedScrollData>> SCROLL_INFO =
            DATA_COMPONENTS.registerComponentType("unidentified_scroll_info", builder -> builder
                    .persistent(UnidentifiedScrollData.CODEC)
                    .networkSynchronized(UnidentifiedScrollData.STREAM_CODEC));

    // ==========================================
    // 其他业务组件
    // ==========================================

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> MIMIC_CATEGORY =
            DATA_COMPONENTS.registerComponentType("mimic_cat", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ITEM_INSTANCE_UUID =
            DATA_COMPONENTS.registerComponentType("item_instance_uuid", builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortableFurnaceData>> FURNACE_DATA =
            DATA_COMPONENTS.registerComponentType("furnace_data", builder -> builder
                    .persistent(PortableFurnaceData.CODEC)
                    .networkSynchronized(PortableFurnaceData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortableBrewingData>> BREWING_DATA =
            DATA_COMPONENTS.registerComponentType("brewing_data", builder -> builder
                    .persistent(PortableBrewingData.CODEC)
                    .networkSynchronized(PortableBrewingData.STREAM_CODEC));

    public record ActiveRuneword(String id, String purity) {
        public static final Codec<ActiveRuneword> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("id").forGetter(ActiveRuneword::id),
                Codec.STRING.fieldOf("purity").forGetter(ActiveRuneword::purity)
        ).apply(inst, ActiveRuneword::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ActiveRuneword> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ActiveRuneword::id,
                ByteBufCodecs.STRING_UTF8, ActiveRuneword::purity,
                ActiveRuneword::new
        );
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ActiveRuneword>> ACTIVE_RUNEWORD =
            DATA_COMPONENTS.registerComponentType("active_runeword", builder -> builder
                    .persistent(ActiveRuneword.CODEC)
                    .networkSynchronized(ActiveRuneword.STREAM_CODEC));

    public record CorruptionData(int count, boolean bricked, List<String> effects) {
        public static final Codec<CorruptionData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("count").forGetter(CorruptionData::count),
                Codec.BOOL.fieldOf("bricked").forGetter(CorruptionData::bricked),
                Codec.STRING.listOf().fieldOf("effects").forGetter(CorruptionData::effects)
        ).apply(inst, CorruptionData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CorruptionData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CorruptionData::count,
                ByteBufCodecs.BOOL, CorruptionData::bricked,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CorruptionData::effects,
                CorruptionData::new
        );
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CorruptionData>> CORRUPTION =
            DATA_COMPONENTS.registerComponentType("corruption", builder -> builder
                    .persistent(CorruptionData.CODEC)
                    .networkSynchronized(CorruptionData.STREAM_CODEC));

    /** 蓝/紫装出生自动腐化 1 次的防重复标记 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CORRUPTION_CHECKED =
            DATA_COMPONENTS.registerComponentType("corruption_checked", builder -> builder
                    .persistent(LENIENT_BOOL_CODEC)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}