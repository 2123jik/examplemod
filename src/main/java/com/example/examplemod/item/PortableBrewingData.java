package com.example.examplemod.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record PortableBrewingData(
        List<ItemStack> items, // size 5: 0,1,2=药水, 3=材料, 4=烈焰粉
        int brewTime,
        int fuel
) {
    public static final PortableBrewingData EMPTY = new PortableBrewingData(
            List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY), 0, 0
    );

    public static final Codec<PortableBrewingData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(PortableBrewingData::items),
            Codec.INT.fieldOf("brew_time").forGetter(PortableBrewingData::brewTime),
            Codec.INT.fieldOf("fuel").forGetter(PortableBrewingData::fuel)
    ).apply(inst, PortableBrewingData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableBrewingData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), PortableBrewingData::items,
            ByteBufCodecs.VAR_INT, PortableBrewingData::brewTime,
            ByteBufCodecs.VAR_INT, PortableBrewingData::fuel,
            PortableBrewingData::new
    );
}