package com.nf121.examplemod.mixin.vanilla;


import com.nf121.examplemod.entity.chest.PlayerMarkedChest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin
        extends RandomizableContainerBlockEntity
        implements PlayerMarkedChest {

    @Unique
    private static final String examplemod_MARKED_PLAYERS_TAG =
            "examplemodMarkedPlayers";

    @Unique
    private final Set<UUID> examplemod$markedPlayers = new HashSet<>();

    /**
     * Mixin 编译所需的伪构造器。
     *
     * 这不会替换 ChestBlockEntity 的真实构造器。
     */
    protected ChestBlockEntityMixin(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    @Override
    public boolean examplemod$isMarkedBy(UUID playerUuid) {
        return this.examplemod$markedPlayers.contains(playerUuid);
    }

    @Override
    public boolean examplemod$mark(UUID playerUuid) {
        if (this.level == null || this.level.isClientSide()) {
            return false;
        }

        if (!this.examplemod$markedPlayers.add(playerUuid)) {
            return false;
        }

        this.examplemod$setChangedAndSync();
        return true;
    }

    @Override
    public boolean examplemod$unmark(UUID playerUuid) {
        if (this.level == null || this.level.isClientSide()) {
            return false;
        }

        if (!this.examplemod$markedPlayers.remove(playerUuid)) {
            return false;
        }

        this.examplemod$setChangedAndSync();
        return true;
    }

    /*
     * 写入区块存档。
     *
     * 根据你贴出的反编译结果，saveAdditional 明确存在于
     * ChestBlockEntity，因此直接注入这个类即可。
     */
    @Inject(
            method = "saveAdditional",
            at = @At("TAIL")
    )
    private void examplemod$saveMarkedPlayers(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo callback
    ) {
        this.examplemod$writeMarkedPlayers(tag);
    }

    /*
     * 从区块存档加载。
     */
    @Inject(
            method = "loadAdditional",
            at = @At("TAIL")
    )
    private void examplemod$loadMarkedPlayers(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo callback
    ) {
        this.examplemod$readMarkedPlayers(tag);
    }

    /**
     * 玩家走近、开始追踪区块时，用于初始同步。
     *
     * 这里专门创建只包含标记数据的 Tag，不发送箱子物品。
     */
    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = new CompoundTag();
        this.examplemod$writeMarkedPlayers(tag);
        return tag;
    }

    /**
     * sendBlockUpdated() 之后用于实时同步。
     */
    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                (ChestBlockEntity) (Object) this
        );
    }

    /**
     * 客户端收到 BlockEntity 更新 Tag。
     *
     * 不调用完整的 loadAdditional，因为更新 Tag 只包含标记，
     * 不包含 Items、LootTable 等持久化数据。
     */
    @Override
    public void handleUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        this.examplemod$readMarkedPlayers(tag);
    }

    @Unique
    private void examplemod$setChangedAndSync() {
        this.setChanged();

        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState state = this.getBlockState();

        serverLevel.sendBlockUpdated(
                this.worldPosition,
                state,
                state,
                Block.UPDATE_CLIENTS
        );
    }

    @Unique
    private void examplemod$writeMarkedPlayers(CompoundTag tag) {
        if (this.examplemod$markedPlayers.isEmpty()) {
            tag.remove(examplemod_MARKED_PLAYERS_TAG);
            return;
        }

        ListTag playerList = new ListTag();

        for (UUID playerUuid : this.examplemod$markedPlayers) {
            playerList.add(
                    new IntArrayTag(
                            UUIDUtil.uuidToIntArray(playerUuid)
                    )
            );
        }

        tag.put(examplemod_MARKED_PLAYERS_TAG, playerList);
    }

    @Unique
    private void examplemod$readMarkedPlayers(CompoundTag tag) {
        this.examplemod$markedPlayers.clear();

        if (!tag.contains(
                examplemod_MARKED_PLAYERS_TAG,
                Tag.TAG_LIST
        )) {
            return;
        }

        ListTag playerList = tag.getList(
                examplemod_MARKED_PLAYERS_TAG,
                Tag.TAG_INT_ARRAY
        );

        for (int index = 0; index < playerList.size(); index++) {
            Tag element = playerList.get(index);

            if (!(element instanceof IntArrayTag uuidTag)) {
                continue;
            }

            int[] uuidArray = uuidTag.getAsIntArray();

            if (uuidArray.length != 4) {
                continue;
            }

            this.examplemod$markedPlayers.add(
                    UUIDUtil.uuidFromIntArray(uuidArray)
            );
        }
    }
}
