package com.example.examplemod.skill;

import com.example.examplemod.registry.ModAttachments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class SkillManager {

    /**
     * 尝试为玩家升级某个技能
     */
    public static boolean tryUpgradeSkill(ServerPlayer player, String nodeId) {
        SkillNode node = SkillRegistry.NODES.get(nodeId);
        if (node == null) return false;

        PlayerSkillData data = player.getData(ModAttachments.SKILL_DATA.get());
        int currentRank = data.getRank(node.getId());

        // 1. 点数与满级校验
        if (data.getPoints() <= 0 || currentRank >= node.getMaxRank()) {
            return false;
        }

        // 2. 前置技能校验
        for (String parentId : node.getParents()) {
            if (data.getRank(parentId) <= 0) {
                return false;
            }
        }

        // 3. 扣点并提升等级
        data.setPoints(data.getPoints() - 1);
        data.setRank(node.getId(), currentRank + 1);

        // 4. 局部增量刷新单节点属性，避免全星图遍历开销
        applySingleNodeModifier(player, node);

        // 5. 同步回客户端
        PacketDistributor.sendToPlayer(player, new SyncSkillDataPayload(data));

        return true;
    }

    /**
     * 全量刷新所有技能属性（用于登录、跨维度、重生、指令重置等场景）
     */
    public static void applySkillModifiers(Player player) {
        if (player.level().isClientSide() || !player.isAlive()) {
            return;
        }
        for (SkillNode node : SkillRegistry.NODES.values()) {
            applySingleNodeModifier(player, node);
        }
    }

    /**
     * 单节点属性增量应用
     */
    public static void applySingleNodeModifier(Player player, SkillNode node) {
        if (player.level().isClientSide() || !player.isAlive()) {
            return;
        }

        PlayerSkillData data = player.getData(ModAttachments.SKILL_DATA.get());
        int currentRank = data.getRank(node.getId());
        List<SkillNode.AttributeBonus> bonuses = node.getBonuses();

        for (SkillNode.AttributeBonus bonus : bonuses) {
            ResourceKey<Attribute> key = ResourceKey.create(Registries.ATTRIBUTE, bonus.attributeId());
            var attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(key).orElse(null);
            if (attributeHolder == null) continue;

            AttributeInstance instance = player.getAttribute(attributeHolder);
            if (instance == null) continue;

            ResourceLocation modifierId = bonus.modifierId();
            double targetValue = bonus.amountPerRank() * currentRank;
            AttributeModifier existing = instance.getModifier(modifierId);

            // 若数值一致已存在，跳过冗余操作
            if (existing != null && Math.abs(existing.amount() - targetValue) < 1e-5) {
                continue;
            }

            boolean isMaxHealth = attributeHolder.is(Attributes.MAX_HEALTH);
            double oldBonusAmount = existing != null ? existing.amount() : 0.0;

            if (existing != null) {
                instance.removeModifier(modifierId);
            }

            if (currentRank > 0 && Math.abs(targetValue) > 1e-5) {
                instance.addTransientModifier(new AttributeModifier(
                        modifierId,
                        targetValue,
                        bonus.operation()
                ));
            }

            // RPG 体验优化：当最大生命上限增加时，等量补偿当前生命值，避免升级后血条空出一截
            if (isMaxHealth && targetValue > oldBonusAmount) {
                player.heal((float) (targetValue - oldBonusAmount));
            }
        }
    }
}