package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.EquipmentCabinetBlock;
import com.example.examplemod.entity.EquipmentCabinetBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ExampleMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ExampleMod.MOD_ID);

    public static final DeferredHolder<Block, EquipmentCabinetBlock> CABINET =
            BLOCKS.register("equipment_cabinet", () ->
                    new EquipmentCabinetBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EquipmentCabinetBlockEntity>> CABINET_ENTITY =
            BLOCK_ENTITIES.register("equipment_cabinet", () ->
                    BlockEntityType.Builder.of(EquipmentCabinetBlockEntity::new, CABINET.get()).build(null));
}