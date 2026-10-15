package com.nf121.examplemod.entity;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class EquipmentCabinetBlock extends BaseEntityBlock {
    public static final MapCodec<EquipmentCabinetBlock> CODEC =
            simpleCodec(EquipmentCabinetBlock::new);

    public EquipmentCabinetBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EquipmentCabinetBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit
    ) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EquipmentCabinetBlockEntity cabinet) {
            serverPlayer.openMenu(cabinet, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide
                    && level.getBlockEntity(pos) instanceof EquipmentCabinetBlockEntity cabinet) {
                Containers.dropContents(level, pos, cabinet);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
