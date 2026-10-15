package com.nf121.examplemod.skill;

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
                    Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ranks").forGetter(PlayerSkillData::getAllocatedRanks),
                    // 新增：记录已经通过食物领取的里程碑数（默认 0，兼容旧存档）
                    Codec.INT.optionalFieldOf("claimed_milestones", 0).forGetter(PlayerSkillData::getClaimedMilestones)
            ).apply(instance, PlayerSkillData::new)
    );

    public static final StreamCodec<ByteBuf, PlayerSkillData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerSkillData::getPoints,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), PlayerSkillData::getAllocatedRanks,
            ByteBufCodecs.VAR_INT, PlayerSkillData::getClaimedMilestones,
            PlayerSkillData::new
    );

    private int points;
    private final Map<String, Integer> allocatedRanks;
    private int claimedMilestones; // 记录已领取的里程碑数

    public PlayerSkillData() {
        this(5, new HashMap<>(), 0);
    }

    public PlayerSkillData(int points, Map<String, Integer> allocatedRanks, int claimedMilestones) {
        this.points = points;
        this.allocatedRanks = new HashMap<>(allocatedRanks);
        this.claimedMilestones = claimedMilestones;
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

    public int getClaimedMilestones() { return claimedMilestones; }
    public void setClaimedMilestones(int claimedMilestones) { this.claimedMilestones = claimedMilestones; }

    /**
     * 结算新的里程碑点数
     * @param totalMilestones 当前达到的总里程碑数
     * @param pointsPerMilestone 每个里程碑奖励的点数
     * @return 如果获得了新点数则返回 true
     */
    public boolean updateFoodMilestones(int totalMilestones, int pointsPerMilestone) {
        if (totalMilestones > this.claimedMilestones) {
            int delta = totalMilestones - this.claimedMilestones;
            this.points += delta * pointsPerMilestone;
            this.claimedMilestones = totalMilestones;
            return true;
        }
        return false;
    }
}