package com.example.examplemod.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record PortableFurnaceData(
        List<ItemStack> items, 
        int litTime,
        int litDuration,
        int cookingProgress,
        int cookingTotalTime
) {
    public static final PortableFurnaceData EMPTY = new PortableFurnaceData(
            List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY), 0, 0, 0, 200
    );

    public static final Codec<PortableFurnaceData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(PortableFurnaceData::items),
            Codec.INT.fieldOf("lit_time").forGetter(PortableFurnaceData::litTime),
            Codec.INT.fieldOf("lit_duration").forGetter(PortableFurnaceData::litDuration),
            Codec.INT.fieldOf("cooking_progress").forGetter(PortableFurnaceData::cookingProgress),
            Codec.INT.fieldOf("cooking_total_time").forGetter(PortableFurnaceData::cookingTotalTime)
    ).apply(inst, PortableFurnaceData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableFurnaceData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), PortableFurnaceData::items,
            ByteBufCodecs.VAR_INT, PortableFurnaceData::litTime,
            ByteBufCodecs.VAR_INT, PortableFurnaceData::litDuration,
            ByteBufCodecs.VAR_INT, PortableFurnaceData::cookingProgress,
            ByteBufCodecs.VAR_INT, PortableFurnaceData::cookingTotalTime,
            PortableFurnaceData::new
    );
}