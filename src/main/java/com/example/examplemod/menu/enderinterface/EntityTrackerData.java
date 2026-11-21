package com.example.examplemod.menu.enderinterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTrackerData extends SavedData {
    private static final String DATA_NAME = EnderInterface.MODID + "_entity_tracker";
    private final Map<UUID, BlockPos> positions = new ConcurrentHashMap<>();

    public static EntityTrackerData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EntityTrackerData::new, EntityTrackerData::load),
                DATA_NAME
        );
    }

    public void update(Entity entity) {
        BlockPos pos = entity.blockPosition();
        BlockPos old = this.positions.put(entity.getUUID(), pos);
        if (!pos.equals(old)) {
            this.setDirty();
        }
    }

    public BlockPos getPos(UUID uuid, BlockPos fallback) {
        return this.positions.getOrDefault(uuid, fallback);
    }

    public void remove(UUID uuid) {
        if (this.positions.remove(uuid) != null) {
            this.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, BlockPos> entry : this.positions.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("id", entry.getKey());
            item.putInt("x", entry.getValue().getX());
            item.putInt("y", entry.getValue().getY());
            item.putInt("z", entry.getValue().getZ());
            list.add(item);
        }
        tag.put("entities", list);
        return tag;
    }

    public static EntityTrackerData load(CompoundTag tag, HolderLookup.Provider registries) {
        EntityTrackerData data = new EntityTrackerData();
        ListTag list = tag.getList("entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            if (item.hasUUID("id")) {
                data.positions.put(
                        item.getUUID("id"),
                        new BlockPos(item.getInt("x"), item.getInt("y"), item.getInt("z"))
                );
            }
        }
        return data;
    }
}