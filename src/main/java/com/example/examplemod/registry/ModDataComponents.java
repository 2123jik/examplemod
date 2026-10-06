package com.example.examplemod.registry;

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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "examplemod");

    // ==========================================
    // 基础标记组件 (带历史向下兼容，防止 1b 崩溃)
    // ==========================================

    /**
     * 超级容错 Codec：
     * 1. 遇到旧存档/L2Backpack 里的 1b (Byte/Boolean) -> 正常读出 true
     * 2. 遇到测试时生成的 {} (Unit/Map) -> 同样安全转为 true
     * 3. 再次保存时写出标准的 boolean (1b)
     */
    private static final Codec<Boolean> LENIENT_BOOL_CODEC = Codec.either(Codec.BOOL, Unit.CODEC)
            .xmap(
                    either -> either.map(b -> b, unit -> true),
                    Either::left
            );

    /** 标记为未鉴定物品（通用） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_UNIDENTIFIED =
            COMPONENTS.register("is_unidentified", () -> DataComponentType.<Boolean>builder()
                    .persistent(LENIENT_BOOL_CODEC)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** 防回炉防刷组件：标记已鉴定过 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ALREADY_IDENTIFIED =
            COMPONENTS.register("already_identified", () -> DataComponentType.<Boolean>builder()
                    .persistent(LENIENT_BOOL_CODEC)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    // ==========================================
    // 卷轴未鉴定参数 (兼容量产与新格式)
    // ==========================================

    public record UnidentifiedScrollData(SpellRarity rarity, Optional<ResourceLocation> schoolFilter) {
        private static final Codec<SpellRarity> RARITY_CODEC =
                Codec.STRING.xmap(SpellRarity::valueOf, SpellRarity::name);

        private static final StreamCodec<ByteBuf, SpellRarity> RARITY_STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.map(SpellRarity::valueOf, SpellRarity::name);

        // 新版结构化 Codec
        private static final Codec<UnidentifiedScrollData> STRUCTURED_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        RARITY_CODEC.fieldOf("rarity").forGetter(UnidentifiedScrollData::rarity),
                        ResourceLocation.CODEC.optionalFieldOf("school").forGetter(UnidentifiedScrollData::schoolFilter)
                ).apply(instance, UnidentifiedScrollData::new)
        );

        // 旧版分号字符串 Codec (防旧卷轴吞档)
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

        // 统一持久化 Codec：无论是字符串还是 Map 都能读入，保存时统一保存为干净 Map
        public static final Codec<UnidentifiedScrollData> CODEC = Codec.either(STRUCTURED_CODEC, LEGACY_STRING_CODEC)
                .xmap(
                        either -> either.map(s -> s, l -> l),
                        Either::left
                );

        // 网络流编解码
        public static final StreamCodec<ByteBuf, UnidentifiedScrollData> STREAM_CODEC = StreamCodec.composite(
                RARITY_STREAM_CODEC, UnidentifiedScrollData::rarity,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), UnidentifiedScrollData::schoolFilter,
                UnidentifiedScrollData::new
        );
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UnidentifiedScrollData>> SCROLL_INFO =
            COMPONENTS.register("unidentified_scroll_info", () -> DataComponentType.<UnidentifiedScrollData>builder()
                    .persistent(UnidentifiedScrollData.CODEC)
                    .networkSynchronized(UnidentifiedScrollData.STREAM_CODEC)
                    .build());

    // ==========================================
    // 其他业务组件 (原封不动保持原样)
    // ==========================================

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> MIMIC_CATEGORY =
            COMPONENTS.register("mimic_cat", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ITEM_INSTANCE_UUID =
            COMPONENTS.register("item_instance_uuid", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortableFurnaceData>> FURNACE_DATA =
            COMPONENTS.register("furnace_data", () -> DataComponentType.<PortableFurnaceData>builder()
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

        public static final StreamCodec<ByteBuf, TrimStep> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TrimStep::pattern,
                ByteBufCodecs.STRING_UTF8, TrimStep::material,
                TrimStep::new
        );
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<TrimStep>>> TRIM_HISTORY =
            COMPONENTS.register("trim_history", () -> DataComponentType.<List<TrimStep>>builder()
                    .persistent(TrimStep.CODEC.listOf())
                    .networkSynchronized(TrimStep.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ACTIVE_RUNEWORD =
            COMPONENTS.register("active_runeword", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
            );
}