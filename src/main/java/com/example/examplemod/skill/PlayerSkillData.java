package com.example.examplemod.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkillData {
    public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("points").forGetter(PlayerSkillData::getPoints),
                    Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ranks").forGetter(PlayerSkillData::getAllocatedRanks)
            ).apply(instance, PlayerSkillData::new)
    );

    public static final StreamCodec<ByteBuf, PlayerSkillData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerSkillData::getPoints,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), PlayerSkillData::getAllocatedRanks,
            PlayerSkillData::new
    );

    private int points;
    private final Map<String, Integer> allocatedRanks;

    public PlayerSkillData() {
        this(5, new HashMap<>());
    }

    public PlayerSkillData(int points, Map<String, Integer> allocatedRanks) {
        this.points = points;
        this.allocatedRanks = new HashMap<>(allocatedRanks);
    }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public Map<String, Integer> getAllocatedRanks() { return allocatedRanks; }

    public int getRank(String nodeId) {
        return allocatedRanks.getOrDefault(nodeId, 0);
    }

    public void setRank(String nodeId, int rank) {
        allocatedRanks.put(nodeId, rank);
    }
}