package com.example.examplemod.menu.enderinterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record RemoteBlockSnapshot(
        ResourceKey<Level> dim,
        List<BlockEntry> entries
) {
    public record BlockEntry(
            BlockPos pos,
            BlockState state,
            Optional<CompoundTag> beTag
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockEntry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, BlockEntry::pos,
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), BlockEntry::state,
                ByteBufCodecs.optional(ByteBufCodecs.TRUSTED_COMPOUND_TAG), BlockEntry::beTag,
                BlockEntry::new
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteBlockSnapshot> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), RemoteBlockSnapshot::dim,
            BlockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), RemoteBlockSnapshot::entries,
            RemoteBlockSnapshot::new
    );

    public static Optional<RemoteBlockSnapshot> capture(ServerLevel level, BlockPos centerPos) {
        List<BlockEntry> entries = new ArrayList<>();
        BlockState centerState = level.getBlockState(centerPos);
        BlockEntity centerBe = level.getBlockEntity(centerPos);
        entries.add(captureOne(level, centerPos, centerState, centerBe));

        // 兼容双箱/相连存储方块（acquireChunk 使用半径 2 的票据，水平相邻区块已在加载范围内）
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = centerPos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            boolean sameBlock = neighborState.is(centerState.getBlock());
            boolean sameBe = centerBe != null && neighborBe != null && neighborBe.getType() == centerBe.getType();
            if (sameBlock || sameBe) {
                entries.add(captureOne(level, neighborPos, neighborState, neighborBe));
            }
        }

        return Optional.of(new RemoteBlockSnapshot(level.dimension(), entries));
    }

    private static BlockEntry captureOne(ServerLevel level, BlockPos pos, BlockState state, BlockEntity be) {
        if (be == null) {
            return new BlockEntry(pos.immutable(), state, Optional.empty());
        }
        CompoundTag tag = be.getUpdateTag(level.registryAccess());
        if (tag.isEmpty() && be.getUpdatePacket() instanceof ClientboundBlockEntityDataPacket pkt && pkt.getTag() != null) {
            tag = pkt.getTag();
        }
        return new BlockEntry(pos.immutable(), state, Optional.of(tag.copy()));
    }
}