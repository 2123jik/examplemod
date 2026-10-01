package com.example.examplemod.skill;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ExampleMod.MODID)
public class SkillEvents {

    // 玩家登录/重进时，重新挂载技能属性并同步数据到客户端 UI
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillManager.applySkillModifiers(player);
            PlayerSkillData data = player.getData(ModAttachments.SKILL_DATA.get());
            PacketDistributor.sendToPlayer(player, new SyncSkillDataPayload(data));
        }
    }

    // 玩家死亡复活时，重新绑定属性修饰符并重新同步客户端
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillManager.applySkillModifiers(player);
            PlayerSkillData data = player.getData(ModAttachments.SKILL_DATA.get());
            PacketDistributor.sendToPlayer(player, new SyncSkillDataPayload(data));
        }
    }
}