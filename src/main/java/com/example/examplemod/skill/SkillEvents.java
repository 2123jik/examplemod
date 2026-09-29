package com.example.examplemod.skill;


import com.example.examplemod.registry.ModAttachments;
import com.example.examplemod.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class SkillEvents {



    // 登录、重进时同步与恢复属性
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillManager.applySkillModifiers(player);
            PlayerSkillData data = player.getData(ModAttachments.SKILL_DATA.get());
            PacketDistributor.sendToPlayer(player, new SyncSkillDataPayload(data));
        }
    }

    // 死亡复活时重新绑定属性修饰符
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillManager.applySkillModifiers(player);
            PlayerSkillData data = player.getData(ModAttachments.SKILL_DATA.get());
            PacketDistributor.sendToPlayer(player, new SyncSkillDataPayload(data));
        }
    }

    // 示例：每升 5 级获得 1 点技能点（或者由 Boss 掉落物提供）
    @SubscribeEvent
    public static void onLevelUp(PlayerXpEvent.LevelChange event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevels() > 0) {
            int newLevel = player.experienceLevel + event.getLevels();
            if (newLevel % 5 == 0) {
                PlayerSkillData data = player.getData(ModAttachments.SKILL_DATA.get());
                data.setPoints(data.getPoints() + 1);
                PacketDistributor.sendToPlayer(player, new SyncSkillDataPayload(data));
            }
        }
    }
}